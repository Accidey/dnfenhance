package com.xulai.dnfenhance.net;

import com.xulai.dnfenhance.DnfEnhanceMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EnhanceRequestPayload() implements CustomPacketPayload {
    public static final EnhanceRequestPayload INSTANCE = new EnhanceRequestPayload();

    public static final CustomPacketPayload.Type<EnhanceRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DnfEnhanceMod.MODID, "enhance_request"));

    public static final StreamCodec<FriendlyByteBuf, EnhanceRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
