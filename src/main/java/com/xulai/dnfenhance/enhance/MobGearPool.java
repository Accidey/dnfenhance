package com.xulai.dnfenhance.enhance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

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
        for (Item item : BuiltInRegistries.ITEM) {
            if (isGear(new ItemStack(item))) items.add(item);
        }
        return List.copyOf(items);
    }

    public static boolean isGear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ProjectileWeaponItem) return true;
        return !stack.getAttributeModifiers().modifiers().isEmpty();
    }

    public static boolean isGolden(Item item) {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
        return loc != null && loc.getPath().startsWith("golden_");
    }
}
