package com.xulai.dnfenhance.enhance;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnhanceLogic {
    private EnhanceLogic() {}

    public enum Penalty { NONE, DOWNGRADE, DESTROY }

    public static final Pattern ENHANCE_PREFIX = Pattern.compile("^\\+\\d+\\s*");
    private static final Pattern DOWN_PATTERN = Pattern.compile("DOWN_(\\d+)");

    public static int getLevel(ItemStack stack) {
        Integer level = stack.get(ModDataComponents.ENHANCE_LEVEL.get());
        return level == null ? 0 : Math.max(0, level);
    }

    public static int maxLevel() {
        return Mth.clamp(EnhanceConfig.SUCCESS_RATES.get().size(), 1, 99);
    }

    public static boolean canEnhance(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ProjectileWeaponItem) return true;
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!isEnhanceable(entry.attribute())) continue;
            if (entry.modifier().amount() > 0.0) return true;
        }
        return false;
    }

    public static boolean isVanillaAttribute(Holder<Attribute> attribute) {
        return attribute.unwrapKey()
                .map(key -> key.location().getNamespace().equals("minecraft"))
                .orElse(false);
    }

    public static boolean isAttributeBlacklisted(Holder<Attribute> attribute) {
        String id = attribute.unwrapKey().map(key -> key.location().toString()).orElse(null);
        if (id == null) return true;
        String path = attribute.unwrapKey().map(key -> key.location().getPath()).orElse(null);
        for (String raw : EnhanceConfig.ENHANCE_ATTRIBUTE_BLACKLIST.get()) {
            if (raw == null || raw.isBlank()) continue;
            String entry = raw.trim();
            if (id.equalsIgnoreCase(entry) || (path != null && path.equalsIgnoreCase(entry))) return true;
        }
        return false;
    }

    public static boolean isEnhanceable(Holder<Attribute> attribute) {
        if (attribute == null) return false;
        if (isAttributeBlacklisted(attribute)) return false;
        return isVanillaAttribute(attribute) || EnhanceConfig.ENHANCE_MOD_ATTRIBUTES.get();
    }

    public static double successRate(int target) {
        List<? extends Double> rates = EnhanceConfig.SUCCESS_RATES.get();
        if (target >= 1 && target <= rates.size()) {
            Double value = rates.get(target - 1);
            if (value != null && !value.isNaN()) {
                return Mth.clamp(value, 0.0, 1.0);
            }
        }
        return defaultRate(target);
    }

    private static double defaultRate(int target) {
        if (target <= 3) return 1.0;
        if (target <= 10) return Math.max(0.10, 1.0 - (target - 3) * 0.10);
        return Math.max(0.05, 0.35 - (target - 10) * 0.05);
    }

    public static Penalty penaltyType(int target) {
        String rule = penaltyRule(target);
        if ("DESTROY".equals(rule)) return Penalty.DESTROY;
        if (rule.startsWith("DOWN_")) return Penalty.DOWNGRADE;
        return Penalty.NONE;
    }

    public static int penaltyDowngrade(int target) {
        Matcher matcher = DOWN_PATTERN.matcher(penaltyRule(target));
        if (matcher.matches()) {
            try {
                return Math.max(1, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    private static String penaltyRule(int target) {
        List<? extends String> rules = EnhanceConfig.FAILURE_PENALTIES.get();
        if (target >= 1 && target <= rules.size()) {
            String rule = rules.get(target - 1);
            if (rule != null && !rule.isBlank()) {
                return rule.trim().toUpperCase(Locale.ROOT);
            }
        }
        if (target <= 3) return "NONE";
        if (target <= 7) return "DOWN_1";
        if (target <= 10) return "DOWN_3";
        return "DESTROY";
    }

    public static int carbonCost(int target) {
        List<? extends Integer> costs = EnhanceConfig.CARBON_COSTS.get();
        if (target >= 1 && target <= costs.size()) {
            Integer cost = costs.get(target - 1);
            if (cost != null && cost >= 0) return cost;
        }
        return Math.max(1, target);
    }

    public static double bonusForLevel(int level) {
        if (level <= 0) return 0.0;
        List<? extends Double> bonuses = EnhanceConfig.BONUS_PER_LEVELS.get();
        if (bonuses == null || bonuses.isEmpty()) return 0.0;
        Double value = bonuses.get(Math.min(level, bonuses.size()) - 1);
        return value == null || value.isNaN() ? 0.0 : Math.max(0.0, value);
    }

    public static boolean isAdvancedCarbon(ItemStack stack) {
        return stack.is(com.xulai.dnfenhance.registry.ModItems.ADVANCED_CARBON.get());
    }

    public static boolean isCarbon(ItemStack stack) {
        return stack.is(com.xulai.dnfenhance.registry.ModItems.FURNACE_CARBON.get())
                || isAdvancedCarbon(stack);
    }

    public static boolean isProtectionCharm(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                .equals(ResourceLocation.fromNamespaceAndPath(DnfEnhanceMod.MODID, "luck_charm"));
    }

    public static boolean isEnhanceTicket(ItemStack stack) {
        return ticketLevel(stack) > 0;
    }

    public static int ticketLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        for (int level = 10; level <= 15; level++) {
            if (item == com.xulai.dnfenhance.registry.ModItems.ticketItem(level)) return level;
        }
        return 0;
    }

    public static double ticketSuccessRate(int ticketLevel) {
        List<? extends Double> rates = EnhanceConfig.TICKET_SUCCESS_RATES.get();
        int index = ticketLevel - 10;
        if (index >= 0 && index < rates.size()) {
            Double value = rates.get(index);
            if (value != null && !value.isNaN()) {
                return Mth.clamp(value, 0.0, 1.0);
            }
        }
        return 1.0;
    }

    public static int prefixLevel(ItemStack stack) {
        int level = getLevel(stack);
        if (level > 0) return level;
        return ticketLevel(stack);
    }

    public static int destroyThreshold() {
        List<? extends String> rules = EnhanceConfig.FAILURE_PENALTIES.get();
        for (int i = 0; i < rules.size(); i++) {
            String rule = rules.get(i);
            if (rule != null && "DESTROY".equalsIgnoreCase(rule.trim())) {
                return i + 1;
            }
        }
        return Integer.MAX_VALUE;
    }

    public static void applyLevel(ItemStack stack, int level) {
        if (level <= 0) {
            stack.remove(ModDataComponents.ENHANCE_LEVEL.get());
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            stack.remove(DataComponents.RARITY);
            Component customName = stack.get(DataComponents.CUSTOM_NAME);
            if (customName != null) {
                String stripped = ENHANCE_PREFIX.matcher(customName.getString()).replaceFirst("");
                if (stripped.isEmpty() || stripped.equals(stack.getItem().getName(stack).getString())) {
                    stack.remove(DataComponents.CUSTOM_NAME);
                } else {
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal(stripped));
                }
            }
            return;
        }

        stack.set(ModDataComponents.ENHANCE_LEVEL.get(), level);
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);

        String baseName;
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            baseName = ENHANCE_PREFIX.matcher(customName.getString()).replaceFirst("");
        } else {
            baseName = stack.getItem().getName(stack).getString();
        }
        stack.set(DataComponents.CUSTOM_NAME, buildDisplayName(level, baseName));

        stack.remove(DataComponents.RARITY);
    }

    public static Style baseNameStyle() {
        return Style.EMPTY.withBold(true).withItalic(false);
    }

    public static Style plainNameStyle() {
        return Style.EMPTY.withItalic(false);
    }

    public static final int[] RAINBOW = {
            0xFF5555,
            0xFFAA00,
            0xFFFF55,
            0x55FF55,
            0x55FFFF,
            0x5555FF,
            0xFF55FF
    };

    public static int prefixColor(int level, int charIndex) {
        if (level >= 14) return RAINBOW[Math.floorMod(charIndex, RAINBOW.length)];
        if (level >= 11) return 0xFF5555;
        if (level >= 8)  return 0xFFAA00;
        return 0xFFFFFF;
    }

    public static MutableComponent buildDisplayName(int level, String baseName) {
        MutableComponent name = Component.empty().setStyle(plainNameStyle());
        String prefix = "+" + level;
        for (int i = 0; i < prefix.length(); i++) {
            name.append(Component.literal(String.valueOf(prefix.charAt(i)))
                    .setStyle(baseNameStyle().withColor(TextColor.fromRgb(prefixColor(level, i)))));
        }
        name.append(Component.literal(" " + baseName).setStyle(plainNameStyle()));
        return name;
    }

    public static String stripPrefix(String displayName) {
        return ENHANCE_PREFIX.matcher(displayName).replaceFirst("");
    }

    public static double auraMultiplier(int pigCount) {
        if (pigCount <= 0 || !EnhanceConfig.PIG_ENABLED.get()) return 1.0;
        double penalty = Mth.clamp(EnhanceConfig.PIG_AURA_PENALTY.get(), 0.0, 1.0);
        if (penalty <= 0.0) return 1.0;
        int effective = EnhanceConfig.PIG_AURA_STACKING.get() ? pigCount : 1;
        return Math.pow(1.0 - penalty, effective);
    }

    public record AuraResult(boolean present, boolean mustFail, boolean resetToZero, double multiplier, int count,
            KaiLiPigHelper.KaiLiVariant variant) {
        public static final AuraResult NONE = new AuraResult(false, false, false, 1.0, 0, null);
    }

    public static AuraResult aura(KaiLiPigHelper.KaiLiVariant variant, int count) {
        if (variant == null || count <= 0 || !EnhanceConfig.PIG_ENABLED.get()) return AuraResult.NONE;
        return switch (variant) {
            case MASTER -> new AuraResult(true, true, true, 0.0, count, variant);
            case BLACKENED -> new AuraResult(true, true, false, 0.0, count, variant);
            case NORMAL -> new AuraResult(true, false, false, auraMultiplier(count), count, variant);
        };
    }

    public static double applyAura(double baseRate, int pigCount) {
        return Mth.clamp(Mth.clamp(baseRate, 0.0, 1.0) * auraMultiplier(pigCount), 0.0, 1.0);
    }
}
