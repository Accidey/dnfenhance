package com.xulai.dnfenhance.net;

import net.minecraft.network.FriendlyByteBuf;

public final class EnhanceRequestPayload {
    public static final EnhanceRequestPayload INSTANCE = new EnhanceRequestPayload();

    public static void encode(EnhanceRequestPayload payload, FriendlyByteBuf buf) {
    }

    public static EnhanceRequestPayload decode(FriendlyByteBuf buf) {
        return INSTANCE;
    }
}
