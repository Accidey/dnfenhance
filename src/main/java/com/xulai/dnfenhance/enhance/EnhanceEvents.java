package com.xulai.dnfenhance.enhance;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = DnfEnhanceMod.MODID)
public final class EnhanceEvents {
    private EnhanceEvents() {}

    private static final String MODIFIER_NAME = "DNF Enhance bonus";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        int level = EnhanceLogic.getLevel(stack);
        if (level <= 0) return;

        double percent = EnhanceLogic.bonusForLevel(level);
        if (percent <= 0) return;

        List<Map.Entry<Attribute, AttributeModifier>> entries =
                List.copyOf(event.getModifiers().entries());

        for (Map.Entry<Attribute, AttributeModifier> entry : entries) {
            Attribute attribute = entry.getKey();
            if (!EnhanceLogic.isEnhanceable(attribute)) continue;

            AttributeModifier modifier = entry.getValue();
            if (MODIFIER_NAME.equals(modifier.getName())) continue;

            double base = modifier.getAmount();
            if (base <= 0.0) continue;

            event.removeModifier(attribute, modifier);
            event.addModifier(attribute, new AttributeModifier(modifier.getId(), modifier.getName(),
                    base * (1.0 + percent), modifier.getOperation()));
        }
    }

    @SubscribeEvent
    public static void onArrowFired(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;

        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem)) {
            weapon = player.getOffhandItem();
        }
        if (!(weapon.getItem() instanceof ProjectileWeaponItem)) return;

        int level = EnhanceLogic.getLevel(weapon);
        if (level <= 0) return;

        double multiplier = 1.0 + EnhanceLogic.bonusForLevel(level);
        arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
    }

    private static final EquipmentSlot[] GEAR_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String MOB_GEAR_DONE = "DnfEnhance_MobGearDone";

    @SubscribeEvent
    public static void onMobGearJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.getPersistentData().getBoolean(MOB_GEAR_DONE)) return;
        if (monster.getSpawnType() == null || monster.getSpawnType() == MobSpawnType.SPAWNER) return;
        if (EnhanceLogic.isMobBlacklisted(monster.getType())) return;

        RandomSource random = monster.getRandom();
        boolean hit = random.nextFloat() < EnhanceConfig.MOB_GEAR_CHANCE.get().floatValue();
        monster.getPersistentData().putBoolean(MOB_GEAR_DONE, true);
        if (!hit) return;

        List<EquipmentSlot> occupied = new ArrayList<>();
        for (EquipmentSlot slot : GEAR_SLOTS) {
            if (!monster.getItemBySlot(slot).isEmpty()) occupied.add(slot);
        }

        if (occupied.isEmpty()) {
            giveRandomEnhancedGear(monster, random);
            return;
        }

        ItemStack guaranteed = null;
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack stack = monster.getItemBySlot(slot);
            if (stack.isEmpty() || !EnhanceLogic.isItemAllowed(stack)) continue;
            if (guaranteed == null) guaranteed = stack;
            if (random.nextBoolean()) {
                EnhanceLogic.applyLevel(stack, randomMobEnhanceLevel(random));
            }
        }
        if (guaranteed != null && EnhanceLogic.getLevel(guaranteed) == 0) {
            EnhanceLogic.applyLevel(guaranteed, randomMobEnhanceLevel(random));
        }
    }

    private static int randomMobEnhanceLevel(RandomSource random) {
        int max = Math.min(15, EnhanceLogic.maxLevel());
        if (max <= 10) return 1 + random.nextInt(max);
        if (random.nextFloat() < 0.8f) return 1 + random.nextInt(10);
        return 11 + random.nextInt(max - 10);
    }

    private static final EquipmentSlot[] GIVE_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static void giveRandomEnhancedGear(Monster monster, RandomSource random) {
        boolean piglin = monster instanceof Piglin;
        List<Item> pool = new ArrayList<>();
        for (Item item : MobGearPool.gearPool()) {
            if (piglin && !MobGearPool.isGolden(item)) continue;
            ItemStack candidate = new ItemStack(item);
            if (EnhanceLogic.isItemAllowed(candidate)) pool.add(item);
        }
        if (pool.isEmpty()) return;
        boolean gave = false;
        for (EquipmentSlot slot : GIVE_SLOTS) {
            if (random.nextBoolean()) {
                gave |= giveEnhancedGear(monster, random, pool, slot);
            }
        }
        if (!gave) giveEnhancedGear(monster, random, pool, EquipmentSlot.MAINHAND);
    }

    private static boolean giveEnhancedGear(Monster monster, RandomSource random, List<Item> pool,
            EquipmentSlot target) {
        if (!monster.getItemBySlot(target).isEmpty()) return false;
        for (int attempt = 0; attempt < 8; attempt++) {
            ItemStack stack = new ItemStack(pool.get(random.nextInt(pool.size())));
            if (monster.getEquipmentSlotForItem(stack) != target) continue;
            EnhanceLogic.applyLevel(stack, randomMobEnhanceLevel(random));
            monster.setItemSlot(target, stack);
            monster.setGuaranteedDrop(target);
            return true;
        }
        return false;
    }

    private static boolean hasElementalEnchantment(ItemStack stack) {
        for (Enchantment ench : EnchantmentHelper.getEnchantments(stack).keySet()) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(ench);
            if (id != null && id.getNamespace().equals("elementalcraft")) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        KaiLiPigHelper.KaiLiVariant variant = KaiLiPigHelper.getVariant(event.getEntity());
        if (variant != null) {
            if (!KaiLiPigHelper.isSummoned(event.getEntity())) return;
            KaiLiPigHelper.onMasterDied(event.getEntity());
            Player killer = playerResponsibleFor(event.getSource());
            if (killer == null) return;
            int looting = killer.getMainHandItem().getEnchantmentLevel(Enchantments.MOB_LOOTING);
            KaiLiPigHelper.dropLoot(level, event.getEntity(), variant, looting);
            return;
        }

        if (!(event.getEntity() instanceof Monster monster)) return;

        boolean carriedEnhancedGear = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.OFFHAND) continue;
            ItemStack gear = monster.getItemBySlot(slot);
            if (!gear.isEmpty() && EnhanceLogic.getLevel(gear) > 0) {
                carriedEnhancedGear = true;
                if (hasElementalEnchantment(gear)) continue;
                monster.setItemSlot(slot, ItemStack.EMPTY);
                if (EnhanceConfig.MOB_ENHANCED_GEAR_DROP.get()) {
                    level.addFreshEntity(new ItemEntity(level,
                            monster.getX(), monster.getY() + 0.4, monster.getZ(), gear));
                }
            }
        }

        if (carriedEnhancedGear && event.getSource().getEntity() instanceof Player killer) {
            float chance = EnhanceConfig.CARBON_DROP_CHANCE.get().floatValue()
                    + 0.05f * killer.getMainHandItem().getEnchantmentLevel(Enchantments.MOB_LOOTING);
            RandomSource random = level.getRandom();
            if (random.nextFloat() < chance) {
                int count = 1 + random.nextInt(3);
                ItemStack carbon = new ItemStack(ModItems.FURNACE_CARBON.get(), count);
                level.addFreshEntity(new ItemEntity(level,
                        monster.getX(), monster.getY() + 0.4, monster.getZ(), carbon));
            }
        }
    }

    private static Player playerResponsibleFor(DamageSource source) {
        Entity direct = source.getEntity();
        if (direct instanceof Player player) {
            return player;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
            return owner;
        }
        if (direct instanceof TamableAnimal tameable && tameable.getOwner() instanceof Player owner) {
            return owner;
        }
        Entity indirect = source.getDirectEntity();
        if (indirect != direct && indirect instanceof Player player) {
            return player;
        }
        return null;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Pig pig)) return;
        KaiLiPigHelper.KaiLiVariant variant = KaiLiPigHelper.combatVariantOf(pig);
        if (variant == null) return;
        if (!KaiLiPigHelper.isSummoned(pig)) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (!(pig.level() instanceof ServerLevel level)) return;
        KaiLiPigHelper.onKaiLiAttacked(level, pig, variant, attacker);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        var source = event.getExplosion().getDirectSourceEntity();
        if (source != null && source.getTags().contains(KaiLiPigHelper.TAG_TNT)) {
            event.getExplosion().getToBlow().clear();
            event.getAffectedEntities().removeIf(entity ->
                    entity instanceof Pig pig && KaiLiPigHelper.getVariant(pig) != null);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        KaiLiPigHelper.tickMasters(event.getServer());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Pig pig)) return;
        KaiLiPigHelper.KaiLiVariant variant = KaiLiPigHelper.combatVariantOf(pig);
        if (variant == null) return;
        if (!KaiLiPigHelper.isSummoned(pig)) return;
        if (!KaiLiPigHelper.isCombatActive(pig)) return;
        if (event.getLevel() instanceof ServerLevel level) {
            KaiLiPigHelper.registerCombat(level, pig, variant);
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        int level = EnhanceLogic.getLevel(left);
        if (level <= 0) return;
        if (!event.getRight().isEmpty()) return;

        String name = event.getName();
        if (name == null || name.isBlank()) return;
        if (name.equals(left.getHoverName().getString())) return;

        int repairCost = left.getBaseRepairCost();
        if (repairCost + 1 >= 40 && !event.getPlayer().getAbilities().instabuild) return;

        ItemStack output = left.copy();
        output.setHoverName(EnhanceLogic.buildDisplayName(level, EnhanceLogic.stripLevelMarker(name)));
        event.setOutput(output);
        event.setCost(repairCost + 1);
        event.setMaterialCost(0);
    }
}
