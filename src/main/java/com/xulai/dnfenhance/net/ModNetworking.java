package com.xulai.dnfenhance.net;

import com.xulai.dnfenhance.DnfEnhanceMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetworking {
    private ModNetworking() {}

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DnfEnhanceMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, EnhanceRequestPayload.class,
                EnhanceRequestPayload::encode, EnhanceRequestPayload::decode, ModNetworking::handleEnhanceRequest);
        CHANNEL.registerMessage(id++, AutoEnhancePayload.class,
                AutoEnhancePayload::encode, AutoEnhancePayload::decode, ModNetworking::handleAutoEnhance);
    }

    private static void handleEnhanceRequest(EnhanceRequestPayload payload,
            java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof com.xulai.dnfenhance.menu.EnhancementMenu menu) {
                menu.enhance(player);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleAutoEnhance(AutoEnhancePayload payload,
            java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof com.xulai.dnfenhance.menu.EnhancementMenu menu)) return;
            com.xulai.dnfenhance.block.EnhancementFurnaceBlockEntity furnace = menu.getFurnace();
            if (payload.targetLevel() <= 0) {
                furnace.setAutoTarget(0);
                furnace.setAutoOwner(null);
            } else {
                furnace.setAutoOwner(player.getUUID());
                furnace.setAutoTarget(Math.min(payload.targetLevel(), 99));
            }
        });
        ctx.setPacketHandled(true);
    }
}
