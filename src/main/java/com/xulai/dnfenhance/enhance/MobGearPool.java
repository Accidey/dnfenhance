package com.xulai.dnfenhance.enhance;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class MobGearPool {
    private MobGearPool() {}

    private static volatile List<Item> cached;

    public static List<Item> gearPool() {
        List<Item> pool = cached;
        if (pool == null) {
            pool = buildPool();
            cached = pool;
        }
        return pool;
    }

    public static void resetGearPoolCache() {
        cached = null;
    }

    private static List<Item> buildPool() {
        List<Item> items = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (isGear(new ItemStack(item))) items.add(item);
        }
        return List.copyOf(items);
    }

    public static boolean isGear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ProjectileWeaponItem) return true;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!stack.getAttributeModifiers(slot).isEmpty()) return true;
        }
        return false;
    }

    public static boolean isGolden(Item item) {
        ResourceLocation loc = ForgeRegistries.ITEMS.getKey(item);
        return loc != null && loc.getPath().startsWith("golden_");
    }
}
