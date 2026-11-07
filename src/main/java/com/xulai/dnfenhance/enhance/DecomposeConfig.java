package com.xulai.dnfenhance.enhance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DecomposeConfig {
    private DecomposeConfig() {}

    private static List<? extends String> cachedSource = null;
    private static Map<Integer, List<ItemStack>> cachedOutputs = Map.of();

    public static int workTicks() {
        return EnhanceConfig.DECOMPOSE_WORK_TICKS.get();
    }

    public static List<ItemStack> outputsFor(int level) {
        if (level < 0) return List.of();
        Map<Integer, List<ItemStack>> parsed = parsedOutputs();
        List<ItemStack> result = parsed.get(level);
        return result == null ? List.of() : result;
    }

    private static Map<Integer, List<ItemStack>> parsedOutputs() {
        List<? extends String> source = EnhanceConfig.DECOMPOSE_OUTPUTS.get();
        if (source != cachedSource) {
            Map<Integer, List<ItemStack>> map = new HashMap<>();
            for (int i = 0; i < source.size(); i++) {
                List<ItemStack> stacks = parseEntry(source.get(i));
                if (!stacks.isEmpty()) {
                    map.put(i, List.copyOf(stacks));
                }
            }
            cachedOutputs = Map.copyOf(map);
            cachedSource = source;
        }
        return cachedOutputs;
    }

    private static List<ItemStack> parseEntry(String raw) {
        List<ItemStack> stacks = new ArrayList<>();
        if (raw == null) return stacks;
        for (String part : raw.split(";")) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            int star = s.indexOf('*');
            String idPart = star >= 0 ? s.substring(0, star).trim() : s;
            int count = 1;
            if (star >= 0) {
                try {
                    count = Integer.parseInt(s.substring(star + 1).trim());
                } catch (NumberFormatException ignored) {
                    count = 1;
                }
            }
            count = Math.max(1, Math.min(64, count));
            Identifier id = Identifier.tryParse(idPart);
            if (id == null) continue;
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item == Items.AIR) continue;
            stacks.add(new ItemStack(item, count));
        }
        return stacks;
    }
}
