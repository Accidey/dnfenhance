package com.xulai.dnfenhance.enhance;

import com.xulai.dnfenhance.DnfEnhanceMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnhanceLogic {
    private EnhanceLogic() {}

    public enum Penalty { NONE, DOWNGRADE, DESTROY }

    public static final Pattern ENHANCE_PREFIX = Pattern.compile("^\\+\\d+\\s*");
    private static final Pattern DOWN_PATTERN = Pattern.compile("DOWN_(\\d+)");

    public static final String NBT_LEVEL = "dnfenhance_level";

    public static int getLevel(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return 0;
        return Math.max(0, stack.getTag().getInt(NBT_LEVEL));
    }

    public static int maxLevel() {
        return Mth.clamp(EnhanceConfig.SUCCESS_RATES.get().size(), 1, 99);
    }

    public static boolean canEnhance(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ProjectileWeaponItem) return true;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (var entry : stack.getItem().getDefaultAttributeModifiers(slot).entries()) {
                if (!isEnhanceable(entry.getKey())) continue;
                if (entry.getValue().getAmount() > 0.0) return true;
            }
        }
        return false;
    }

    public static boolean isVanillaAttribute(Attribute attribute) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id != null && id.getNamespace().equals("minecraft");
    }

    public static boolean isAttributeBlacklisted(Attribute attribute) {
        ResourceLocation loc = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (loc == null) return true;
        String id = loc.toString();
        String path = loc.getPath();
        for (String raw : EnhanceConfig.ENHANCE_ATTRIBUTE_BLACKLIST.get()) {
            if (raw == null || raw.isBlank()) continue;
            String entry = raw.trim();
            if (id.equalsIgnoreCase(entry) || path.equalsIgnoreCase(entry)) return true;
        }
        return false;
    }

    public static boolean isEnhanceable(Attribute attribute) {
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
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && key.equals(new ResourceLocation(DnfEnhanceMod.MODID, "luck_charm"));
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
            if (stack.hasTag()) stack.removeTagKey(NBT_LEVEL);
            if (stack.hasCustomHoverName()) {
                String stripped = ENHANCE_PREFIX.matcher(stack.getHoverName().getString()).replaceFirst("");
                String defaultName = stack.getItem().getName(stack).getString();
                if (stripped.isEmpty() || stripped.equals(defaultName)) {
                    stack.removeTagKey("display.Name");
                } else {
                    stack.setHoverName(Component.literal(stripped));
                }
            }
            return;
        }

        stack.getOrCreateTag().putInt(NBT_LEVEL, level);

        String baseName;
        if (stack.hasCustomHoverName()) {
            baseName = ENHANCE_PREFIX.matcher(stack.getHoverName().getString()).replaceFirst("");
        } else {
            baseName = stack.getItem().getName(stack).getString();
        }
        stack.setHoverName(buildDisplayName(level, baseName));
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
