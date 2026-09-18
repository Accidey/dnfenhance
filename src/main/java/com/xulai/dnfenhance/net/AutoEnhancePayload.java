package com.xulai.dnfenhance.net;

import net.minecraft.network.FriendlyByteBuf;

public final class AutoEnhancePayload {
    private final int targetLevel;

    public AutoEnhancePayload(int targetLevel) {
        this.targetLevel = targetLevel;
    }

    public int targetLevel() {
        return this.targetLevel;
    }

    public static void encode(AutoEnhancePayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.targetLevel);
    }

    public static AutoEnhancePayload decode(FriendlyByteBuf buf) {
        return new AutoEnhancePayload(buf.readVarInt());
    }
}
