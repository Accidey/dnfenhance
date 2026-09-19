package com.xulai.dnfenhance.client;

import com.xulai.dnfenhance.enhance.EnhanceLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public final class EnhanceNameFx {
    private static final int[] RED_WAVE = {0xFFFFAAAA, 0xFFFF5555, 0xFFCC0000, 0xFFAA0000};

    private static final long RED_STEP_MS = 250;
    private static final long RAINBOW_STEP_MS = 220;

    private EnhanceNameFx() {}

    public static MutableComponent buildPrefix(int level, long timeMillis) {
        MutableComponent prefix = Component.empty().setStyle(EnhanceLogic.baseNameStyle());
        String text = "+" + level;
        for (int i = 0; i < text.length(); i++) {
            prefix.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(EnhanceLogic.baseNameStyle()
                            .withColor(TextColor.fromRgb(colorOf(level, i, timeMillis)))));
        }
        return prefix;
    }

    public static int colorOf(int level, int charIndex, long timeMillis) {
        if (level >= 14) {
            int index = (int) Math.floorMod(timeMillis / RAINBOW_STEP_MS - charIndex,
                    (long) EnhanceLogic.RAINBOW.length);
            return EnhanceLogic.RAINBOW[index];
        }
        if (level >= 11) {
            int index = (int) Math.floorMod(timeMillis / RED_STEP_MS - charIndex, (long) RED_WAVE.length);
            return RED_WAVE[index];
        }
        return EnhanceLogic.prefixColor(level, charIndex);
    }
}
