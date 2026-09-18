package com.xulai.dnfenhance.enhance;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = DnfEnhanceMod.MODID)
public final class EnhanceEvents {
    private EnhanceEvents() {}

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        int level = EnhanceLogic.getLevel(stack);
        if (level <= 0) return;

        double percent = EnhanceLogic.bonusForLevel(level);
        if (percent <= 0) return;

        List<ItemAttributeModifiers.Entry> entries =
                List.copyOf(event.getDefaultModifiers().modifiers());

        for (ItemAttributeModifiers.Entry entry : entries) {
            Holder<Attribute> attribute = entry.attribute();
            if (!EnhanceLogic.isEnhanceable(attribute)) continue;

            AttributeModifier modifier = entry.modifier();
            if (modifier.id().getNamespace().equals(DnfEnhanceMod.MODID)) continue;

            double base = modifier.amount();
            if (base <= 0.0) continue;

            event.replaceModifier(attribute,
                    new AttributeModifier(modifier.id(), base * (1.0 + percent), modifier.operation()),
                    entry.slot());
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

    private static final List<Item> WEAPONS = List.of(
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD,
            Items.IRON_AXE, Items.DIAMOND_AXE);
    private static final Item[][] ARMOR_SLOTS = {
            { Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET },
            { Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE },
            { Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS },
            { Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS }
    };

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (event.getSpawnType() == MobSpawnType.SPAWNER) return;

        RandomSource random = event.getEntity().getRandom();
        if (random.nextFloat() >= EnhanceConfig.MOB_GEAR_CHANCE.get().floatValue()) return;

        int level = 1 + (int) (random.nextFloat() * random.nextFloat() * 9.99f);

        if (random.nextBoolean()) {
            Item weapon = WEAPONS.get(Math.min(WEAPONS.size() - 1, (level - 1) * WEAPONS.size() / 10
                    + random.nextInt(2)));
            ItemStack stack = new ItemStack(weapon);
            EnhanceLogic.applyLevel(stack, level);
            monster.setItemSlot(EquipmentSlot.MAINHAND, stack);
            monster.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                } else {
            int piece = random.nextInt(ARMOR_SLOTS.length);
            Item[] materials = ARMOR_SLOTS[piece];
            Item armor = materials[Math.min(materials.length - 1, level * materials.length / 11
                    + (random.nextBoolean() ? 0 : 1))];
            ItemStack stack = new ItemStack(armor);
            EquipmentSlot slot = monster.getEquipmentSlotForItem(stack);
            if (slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.MAINHAND) return;
            if (!monster.getItemBySlot(slot).isEmpty()) return;
            EnhanceLogic.applyLevel(stack, level);
            monster.setItemSlot(slot, stack);
            monster.setGuaranteedDrop(slot);
        }
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
            var lootingHolder = level.registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING);
            int looting = killer.getMainHandItem().getEnchantmentLevel(lootingHolder);
            KaiLiPigHelper.dropLoot(level, event.getEntity(), variant, looting);
            return;
        }

        if (!(event.getEntity() instanceof Monster monster)) return;

        boolean carriedEnhancedGear = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.BODY) continue;
            ItemStack gear = monster.getItemBySlot(slot);
            if (!gear.isEmpty() && EnhanceLogic.getLevel(gear) > 0) {
                carriedEnhancedGear = true;
                monster.setItemSlot(slot, ItemStack.EMPTY);
                level.addFreshEntity(new ItemEntity(level,
                        monster.getX(), monster.getY() + 0.4, monster.getZ(), gear));
            }
        }

        if (carriedEnhancedGear && event.getSource().getEntity() instanceof Player killer) {
            var lootingHolder = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING);
            float chance = EnhanceConfig.CARBON_DROP_CHANCE.get().floatValue()
                    + 0.05f * killer.getMainHandItem().getEnchantmentLevel(lootingHolder);
            RandomSource random = level.getRandom();
            if (random.nextFloat() < chance) {
                int count = 1 + random.nextInt(3);
                ItemStack carbon = new ItemStack(ModItems.FURNACE_CARBON.get(), count);
                level.addFreshEntity(new ItemEntity(level,
                        monster.getX(), monster.getY() + 0.4, monster.getZ(), carbon));
            }
        }
    }

    private static Player playerResponsibleFor(net.minecraft.world.damagesource.DamageSource source) {
        Entity direct = source.getEntity();
        if (direct instanceof Player player) {
            return player;
        }
        if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile
                && projectile.getOwner() instanceof Player owner) {
            return owner;
        }
        if (direct instanceof net.minecraft.world.entity.TamableAnimal tameable
                && tameable.getOwner() instanceof Player owner) {
            return owner;
        }
        Entity indirect = source.getDirectEntity();
        if (indirect != direct && indirect instanceof Player player) {
            return player;
        }
        return null;
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
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
    public static void onServerTick(ServerTickEvent.Post event) {
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
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        var source = event.getExplosion().getDirectSourceEntity();
        if (source != null && source.getTags().contains(KaiLiPigHelper.TAG_TNT)) {
            event.getExplosion().getToBlow().clear();
            event.getAffectedEntities().removeIf(entity ->
                    entity instanceof Pig pig && KaiLiPigHelper.getVariant(pig) != null);
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

        int repairCost = left.getOrDefault(DataComponents.REPAIR_COST, 0);
        if (repairCost + 1 >= 40 && !event.getPlayer().getAbilities().instabuild) return;

        ItemStack output = left.copy();
        output.set(DataComponents.CUSTOM_NAME,
                EnhanceLogic.buildDisplayName(level, EnhanceLogic.stripPrefix(name)));
        event.setOutput(output);
        event.setCost(repairCost + 1L);
        event.setMaterialCost(0);
    }
}
