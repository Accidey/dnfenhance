package com.xulai.dnfenhance.net;

import com.xulai.dnfenhance.DnfEnhanceMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AutoEnhancePayload(int targetLevel) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AutoEnhancePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DnfEnhanceMod.MODID, "auto_enhance"));

    public static final StreamCodec<FriendlyByteBuf, AutoEnhancePayload> STREAM_CODEC =
            StreamCodec.ofMember(AutoEnhancePayload::write, AutoEnhancePayload::read);

    public static AutoEnhancePayload read(FriendlyByteBuf buf) {
        return new AutoEnhancePayload(buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.targetLevel);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
