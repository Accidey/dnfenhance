package com.xulai.dnfenhance.enhance;

import com.xulai.dnfenhance.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KaiLiPigHelper {
    private KaiLiPigHelper() {}

    public static final String TAG_SUMMONED = "dnfenhance_kai_li";
    public static final String TAG_BLACKENED = "dnfenhance_kai_li_blackened";
    public static final String TAG_MASTER = "dnfenhance_kai_li_master";
    public static final String TAG_TNT = "dnfenhance_kai_li_tnt";

    public static final String TAG_ACTIVE = "dnfenhance_kai_li_active";

    public enum KaiLiVariant { NORMAL, BLACKENED, MASTER }

    public static final int MASTER_GEAR_LEVEL = 10;

    private static final int COMBO_TNT_FUSE = 30;
    private static final int BARRAGE_TNT_FUSE = 60;

    private static final Map<UUID, MasterState> ACTIVE_MASTERS = new HashMap<>();

    private static final class MasterState {
        final Pig pig;
        final KaiLiVariant variant;
        final boolean active;
        long nextTntAt;
        long nextSonicAt;
        long nextComboAt;
        long squadRespawnAt;
        UUID lastTarget;
        List<Monster> squad = List.of();

        MasterState(Pig pig, KaiLiVariant variant, boolean active) {
            this.pig = pig;
            this.variant = variant;
            this.active = active;
        }
    }

    public static MasterState registerCombat(ServerLevel level, Pig pig, KaiLiVariant variant) {
        return ACTIVE_MASTERS.computeIfAbsent(pig.getUUID(), id -> {
            MasterState state = new MasterState(pig, variant, isCombatActive(pig));
            state.nextTntAt = level.getGameTime() + Math.max(20, EnhanceConfig.PIG_MASTER_TNT_INTERVAL.get());
            state.nextSonicAt = 0L;
            return state;
        });
    }

    public static boolean isCombatActive(Entity entity) {
        return entity.entityTags().contains(TAG_ACTIVE);
    }

    public static KaiLiVariant combatVariantOf(Entity entity) {
        if (entity.entityTags().contains(TAG_MASTER)) return KaiLiVariant.MASTER;
        if (entity.entityTags().contains(TAG_BLACKENED)) return KaiLiVariant.BLACKENED;
        return null;
    }

    public static void onMasterDied(Entity pig) {
        ACTIVE_MASTERS.remove(pig.getUUID());
    }

    public static void tickMasters(MinecraftServer server) {
        if (ACTIVE_MASTERS.isEmpty()) return;
        var it = ACTIVE_MASTERS.values().iterator();
        while (it.hasNext()) {
            MasterState state = it.next();
            Pig pig = state.pig;
            if (!pig.isAlive() || pig.isRemoved()) {
                it.remove();
                continue;
            }
            if (!(pig.level() instanceof ServerLevel level)) continue;
            if (!state.active) continue;
            long now = level.getGameTime();

            if (!state.squad.isEmpty() && state.squad.stream().noneMatch(LivingEntity::isAlive)) {
                state.squad = List.of();
                if (state.squadRespawnAt == 0) {
                    state.squadRespawnAt = now + Math.max(20, EnhanceConfig.PIG_MASTER_SQUAD_RESPAWN.get());
                }
            }
            Player focus = resolveTarget(level, pig, state);
            if (state.squadRespawnAt != 0 && now >= state.squadRespawnAt) {
                state.squadRespawnAt = 0;
                state.squad = summonSquad(level, pig, focus);
            }

            if (now >= state.nextTntAt) {
                state.nextTntAt = now + Math.max(20, EnhanceConfig.PIG_MASTER_TNT_INTERVAL.get());
                if (focus != null) {
                    throwTnt(level, pig, focus.position(), BARRAGE_TNT_FUSE);
                }
            }

            if (state.variant == KaiLiVariant.MASTER && now >= state.nextSonicAt) {
                double radius = EnhanceConfig.PIG_MASTER_PROXIMITY_RADIUS.get();
                Player near = nearestPlayer(level, pig.getX(), pig.getY(), pig.getZ(), radius);
                if (near != null) {
                    state.nextSonicAt = now + Math.max(20, EnhanceConfig.PIG_MASTER_PROXIMITY_COOLDOWN.get());
                    sonicBoom(level, pig, near);
                }
            }
        }
    }

    public static void onKaiLiAttacked(ServerLevel level, Pig pig, KaiLiVariant variant, Player attacker) {
        pig.addTag(TAG_ACTIVE);
        MasterState state = registerCombat(level, pig, variant);
        if (state == null) return;
        state.lastTarget = attacker.getUUID();

        double range = EnhanceConfig.PIG_MASTER_TNT_RANGE.get();
        if (attacker.distanceToSqr(pig.getX(), pig.getY(), pig.getZ()) > range * range) return;

        long now = level.getGameTime();
        if (now < state.nextComboAt) return;
        state.nextComboAt = now + EnhanceConfig.PIG_MASTER_COMBO_COOLDOWN.get();

        if (variant == KaiLiVariant.MASTER) {
            sonicBoom(level, pig, attacker);
            throwTnt(level, pig, predictKnockbackPoint(pig, attacker), COMBO_TNT_FUSE);
        } else {
            throwTnt(level, pig, attacker.position(), COMBO_TNT_FUSE);
        }

        if (state.squad.isEmpty() && state.squadRespawnAt == 0) {
            state.squad = summonSquad(level, pig, attacker);
        }
    }

    private static void sonicBoom(ServerLevel level, Pig pig, Player target) {
        level.playSound(null, pig.getX(), pig.getY(), pig.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                pig.getX(), pig.getY() + 1.0, pig.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        double dmg = EnhanceConfig.PIG_MASTER_SONIC_DAMAGE.get();
        if (dmg > 0) {
            target.hurt(level.damageSources().sonicBoom(pig), (float) dmg);
        }
        var away = target.position().subtract(pig.position());
        var horizontal = new net.minecraft.world.phys.Vec3(away.x, 0.0, away.z);
        if (horizontal.lengthSqr() < 1.0E-4) horizontal = new net.minecraft.world.phys.Vec3(0.0, 0.0, 1.0);
        var impulse = horizontal.normalize().scale(1.2).add(0.0, 0.4, 0.0);
        target.push(impulse);
    }

    private static Vec3 predictKnockbackPoint(Pig pig, Player attacker) {
        Vec3 vel = attacker.getDeltaMovement();
        Vec3 predicted = new Vec3(attacker.getX() + vel.x * 12.0, attacker.getY(), attacker.getZ() + vel.z * 12.0);
        if (predicted.distanceToSqr(attacker.position()) < 2.25) {
            Vec3 dir = new Vec3(attacker.getX() - pig.getX(), 0.0, attacker.getZ() - pig.getZ());
            if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0.0, 0.0, 1.0);
            predicted = attacker.position().add(dir.normalize().scale(4.0));
        }
        double range = EnhanceConfig.PIG_MASTER_TNT_RANGE.get();
        double dx = predicted.x - pig.getX();
        double dz = predicted.z - pig.getZ();
        double distH2 = dx * dx + dz * dz;
        if (distH2 > range * range) {
            double scale = range / Math.sqrt(distH2);
            predicted = new Vec3(pig.getX() + dx * scale, predicted.y, pig.getZ() + dz * scale);
        }
        return predicted;
    }

    private static Player resolveTarget(ServerLevel level, Pig pig, MasterState state) {
        double range = EnhanceConfig.PIG_MASTER_TNT_RANGE.get();
        if (state.lastTarget != null) {
            for (Player p : level.players()) {
                if (p.getUUID().equals(state.lastTarget) && p.isAlive() && !p.isSpectator()) {
                    if (p.distanceToSqr(pig.getX(), pig.getY(), pig.getZ()) <= range * range) {
                        return p;
                    }
                    break;
                }
            }
        }
        return nearestPlayer(level, pig.getX(), pig.getY(), pig.getZ(), range);
    }

    private static Player nearestPlayer(ServerLevel level, double x, double y, double z, double range) {
        if (range <= 0) return null;
        double r2 = range * range;
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : level.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            double d = p.distanceToSqr(x, y, z);
            if (d <= r2 && d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private static void throwTnt(ServerLevel level, Pig pig, Vec3 target, int fuse) {
        PrimedTnt tnt = EntityTypes.TNT.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (tnt == null) return;
        double px = pig.getX();
        double py = pig.getY() + 0.8;
        double pz = pig.getZ();
        tnt.snapTo(px, py, pz, pig.getYRot(), 0.0F);
        double dx = target.x - px;
        double dy = target.y + 0.5 - py;
        double dz = target.z - pz;
        double distH = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
        double power = Math.min(2.0, 0.6 + distH * 0.08);
        double vy = Math.min(0.9, 0.35 + distH * 0.03) + Math.max(0.0, dy) * 0.02;
        tnt.setDeltaMovement(dx / distH * power * 0.7, vy, dz / distH * power * 0.7);
        tnt.setFuse(fuse);
        tnt.addTag(TAG_TNT);
        level.addFreshEntity(tnt);
        level.playSound(null, px, py, pz, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private static List<Monster> summonSquad(ServerLevel level, Pig pig, Player focus) {
        int min = EnhanceConfig.PIG_MASTER_MINION_MIN.get();
        int max = Math.max(min, EnhanceConfig.PIG_MASTER_MINION_MAX.get());
        int count = min + (max > min ? level.getRandom().nextInt(max - min + 1) : 0);
        double cx = focus != null ? focus.getX() : pig.getX();
        double cy = focus != null ? focus.getY() : pig.getY();
        double cz = focus != null ? focus.getZ() : pig.getZ();
        List<Monster> squad = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Monster mob = spawnMinion(level, cx, cy, cz);
            if (mob != null) squad.add(mob);
        }
        return squad;
    }

    private static Monster spawnMinion(ServerLevel level, double cx, double cy, double cz) {
        boolean zombie = level.getRandom().nextBoolean();
        Monster mob = zombie ? EntityTypes.ZOMBIE.create(level, EntitySpawnReason.MOB_SUMMONED) : EntityTypes.SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return null;
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        double dist = 2.0 + level.getRandom().nextDouble() * 3.0;
        double x = cx + Math.cos(angle) * dist;
        double z = cz + Math.sin(angle) * dist;
        mob.snapTo(x, cy, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        int min = EnhanceConfig.PIG_MASTER_MINION_LEVEL_MIN.get();
        int max = Math.max(min, EnhanceConfig.PIG_MASTER_MINION_LEVEL_MAX.get());
        int gearLevel = min + (max > min ? level.getRandom().nextInt(max - min + 1) : 0);
        ItemStack weapon = new ItemStack(zombie ? Items.IRON_SWORD : Items.BOW);
        EnhanceLogic.applyLevel(weapon, gearLevel);
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                x, cy + 1.0, z, 10, 0.3, 0.5, 0.3, 0.02);
        return mob;
    }

    public static Component kaiLiName(KaiLiVariant variant) {
        return switch (variant) {
            case BLACKENED -> Component.translatable("dnfenhance.pig.kai_li_blackened")
                    .withStyle(ChatFormatting.DARK_RED);
            case MASTER -> Component.translatable("dnfenhance.pig.kai_li_master")
                    .withStyle(ChatFormatting.GOLD);
            case NORMAL -> Component.translatable("dnfenhance.pig.kai_li");
        };
    }

    public static KaiLiVariant getVariant(Entity entity) {
        if (!(entity instanceof Pig pig)) return null;
        Component custom = pig.getCustomName();
        if (custom == null) return null;
        String name = custom.getString().trim();
        for (KaiLiVariant v : KaiLiVariant.values()) {
            if (name.equals(kaiLiName(v).getString())) return v;
        }
        return null;
    }

    public static boolean isSummoned(Entity entity) {
        return entity.entityTags().contains(TAG_SUMMONED);
    }

    public static int countNearby(Level level, BlockPos pos, KaiLiVariant variant) {
        int n = 0;
        for (Pig p : pigsInAura(level, pos)) {
            if (getVariant(p) == variant) n++;
        }
        return n;
    }

    public static int countNearby(Level level, BlockPos pos) {
        return pigsInAura(level, pos).size();
    }

    public static NearbyAura scanAura(Level level, BlockPos pos) {
        List<Pig> pigs = pigsInAura(level, pos);
        if (pigs.isEmpty()) return NearbyAura.NONE;
        boolean hasMaster = false;
        boolean hasBlackened = false;
        int normal = 0;
        for (Pig p : pigs) {
            KaiLiVariant v = getVariant(p);
            if (v == KaiLiVariant.MASTER) hasMaster = true;
            else if (v == KaiLiVariant.BLACKENED) hasBlackened = true;
            else if (v == KaiLiVariant.NORMAL) normal++;
        }
        if (hasMaster) return new NearbyAura(KaiLiVariant.MASTER, pigs.size());
        if (hasBlackened) return new NearbyAura(KaiLiVariant.BLACKENED, pigs.size());
        return new NearbyAura(KaiLiVariant.NORMAL, normal);
    }

    public record NearbyAura(KaiLiVariant variant, int count) {
        public static final NearbyAura NONE = new NearbyAura(null, 0);

        public boolean present() {
            return variant != null;
        }
    }

    private static List<Pig> pigsInAura(Level level, BlockPos pos) {
        if (level == null || !EnhanceConfig.PIG_ENABLED.get()) return List.of();
        double radius = EnhanceConfig.PIG_AURA_RADIUS.get();
        if (radius <= 0.0) return List.of();
        return level.getEntitiesOfClass(Pig.class, new AABB(pos).inflate(radius),
                pig -> pig.isAlive() && getVariant(pig) != null);
    }

    public static boolean atCapacity(Level level, BlockPos pos) {
        return countNearby(level, pos) >= EnhanceConfig.PIG_MAX_COUNT;
    }

    public static Pig summon(ServerLevel level, BlockPos pos, KaiLiVariant variant) {
        if (!EnhanceConfig.PIG_ENABLED.get()) return null;
        if (atCapacity(level, pos)) return null;

        double y = pos.getY() + 1.0;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
            double dist = 1.0 + level.getRandom().nextDouble();
            double x = pos.getX() + 0.5 + Math.cos(angle) * dist;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * dist;
            BlockPos feet = BlockPos.containing(x, y, z);
            if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
                Pig pig = spawnNamedPig(level, x, y, z, variant);
                if (pig != null) return pig;
            }
        }
        return spawnNamedPig(level, pos.getX() + 0.5 + 1.5, y, pos.getZ() + 0.5, variant);
    }

    private static Pig spawnNamedPig(ServerLevel level, double x, double y, double z, KaiLiVariant variant) {
        Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (pig == null) return null;
        pig.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        pig.setBaby(false);
        pig.setCustomName(kaiLiName(variant));
        pig.setCustomNameVisible(true);
        pig.setPersistenceRequired();
        pig.addTag(TAG_SUMMONED);
        switch (variant) {
            case BLACKENED -> {
                pig.addTag(TAG_BLACKENED);
                applyVariantStats(pig, EnhanceConfig.PIG_BLACKENED_HEALTH.get(), false);
            }
            case MASTER -> {
                pig.addTag(TAG_MASTER);
                applyVariantStats(pig, EnhanceConfig.PIG_MASTER_HEALTH.get(), true);
            }
            case NORMAL -> {
            }
        }
        level.addFreshEntity(pig);
        return pig;
    }

    private static void applyVariantStats(Pig pig, double health, boolean diamond) {
        AttributeInstance maxHealth = pig.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
            pig.setHealth((float) health);
        }
        Item helmet = diamond ? Items.DIAMOND_HELMET : Items.IRON_HELMET;
        Item chest = diamond ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE;
        Item legs = diamond ? Items.DIAMOND_LEGGINGS : Items.IRON_LEGGINGS;
        Item boots = diamond ? Items.DIAMOND_BOOTS : Items.IRON_BOOTS;
        equipArmor(pig, EquipmentSlot.HEAD, helmet);
        equipArmor(pig, EquipmentSlot.CHEST, chest);
        equipArmor(pig, EquipmentSlot.LEGS, legs);
        equipArmor(pig, EquipmentSlot.FEET, boots);
    }

    private static void equipArmor(Pig pig, EquipmentSlot slot, Item item) {
        ItemStack stack = new ItemStack(item);
        EnhanceLogic.applyLevel(stack, MASTER_GEAR_LEVEL);
        pig.setItemSlot(slot, stack);
        pig.setDropChance(slot, 0.0F);
    }

    public static void dropLoot(ServerLevel level, LivingEntity pig, KaiLiVariant variant, int looting) {
        int bonus = Math.max(0, looting) * EnhanceConfig.PIG_LOOTING_BONUS.get();
        switch (variant) {
            case BLACKENED -> {
                dropRolled(level, pig, ModItems.ADVANCED_CARBON.get(),
                        EnhanceConfig.PIG_BLACKENED_CARBON_MIN.get(), EnhanceConfig.PIG_BLACKENED_CARBON_MAX.get(), bonus);
                dropRolled(level, pig, ModItems.LUCK_CHARM.get(),
                        EnhanceConfig.PIG_BLACKENED_CHARM_MIN.get(), EnhanceConfig.PIG_BLACKENED_CHARM_MAX.get(), bonus);
                dropTickets(level, pig, KaiLiVariant.BLACKENED);
            }
            case MASTER -> {
                dropRolled(level, pig, ModItems.ADVANCED_CARBON.get(),
                        EnhanceConfig.PIG_MASTER_CARBON_MIN.get(), EnhanceConfig.PIG_MASTER_CARBON_MAX.get(), bonus);
                dropRolled(level, pig, ModItems.LUCK_CHARM.get(),
                        EnhanceConfig.PIG_MASTER_CHARM_MIN.get(), EnhanceConfig.PIG_MASTER_CHARM_MAX.get(), bonus);
                dropTickets(level, pig, KaiLiVariant.MASTER);
            }
            case NORMAL -> {
                dropRolled(level, pig, ModItems.ADVANCED_CARBON.get(),
                        EnhanceConfig.PIG_ADVANCED_CARBON_MIN.get(), EnhanceConfig.PIG_ADVANCED_CARBON_MAX.get(), bonus);
                dropRolled(level, pig, ModItems.LUCK_CHARM.get(),
                        EnhanceConfig.PIG_CHARM_MIN.get(), EnhanceConfig.PIG_CHARM_MAX.get(), bonus);
            }
        }
    }

    private static void dropTickets(ServerLevel level, LivingEntity pig, KaiLiVariant variant) {
        int count = variant == KaiLiVariant.MASTER
                ? EnhanceConfig.PIG_MASTER_TICKET_COUNT.get()
                : EnhanceConfig.PIG_BLACKENED_TICKET_COUNT.get();
        List<? extends Double> weights = variant == KaiLiVariant.MASTER
                ? EnhanceConfig.PIG_MASTER_TICKET_RATES.get()
                : EnhanceConfig.PIG_BLACKENED_TICKET_RATES.get();
        if (count <= 0 || weights.isEmpty()) return;
        double total = 0.0;
        for (int i = 0; i < weights.size() && i < 6; i++) {
            Double w = weights.get(i);
            if (w != null && !w.isNaN() && w > 0.0) total += w;
        }
        if (total <= 0.0) return;
        for (int n = 0; n < count; n++) {
            double roll = level.getRandom().nextDouble() * total;
            int tier = 10;
            double acc = 0.0;
            for (int i = 0; i < weights.size() && i < 6; i++) {
                Double w = weights.get(i);
                acc += (w == null || w.isNaN() || w < 0.0) ? 0.0 : w;
                if (roll < acc) {
                    tier = 10 + i;
                    break;
                }
            }
            if (acc <= roll) {
                tier = 10 + Math.min(weights.size(), 6) - 1;
            }
            Item ticket = ModItems.ticketItem(tier);
            if (ticket == null) continue;
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
                    pig.getX(), pig.getY() + 0.4, pig.getZ(), new ItemStack(ticket)));
        }
    }

    private static void dropRolled(ServerLevel level, LivingEntity at,
            Item item, int min, int max, int bonus) {
        int low = Math.max(0, Math.min(min, max));
        int high = Math.max(low, max);
        int rolled = high > low ? level.getRandom().nextInt(high - low + 1) : 0;
        int count = low + rolled + bonus;
        if (count <= 0) return;
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
                at.getX(), at.getY() + 0.4, at.getZ(), new ItemStack(item, count)));
    }
}
