package com.xulai.dnfenhance.net;

import com.xulai.dnfenhance.block.EnhancementFurnaceBlockEntity;
import com.xulai.dnfenhance.menu.EnhancementMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                EnhanceRequestPayload.TYPE,
                EnhanceRequestPayload.STREAM_CODEC,
                ModNetworking::handleEnhanceRequest);
        registrar.playToServer(
                AutoEnhancePayload.TYPE,
                AutoEnhancePayload.STREAM_CODEC,
                ModNetworking::handleAutoEnhance);
    }

    private static void handleEnhanceRequest(EnhanceRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof EnhancementMenu menu) {
                menu.enhance(player);
            }
        });
    }

    private static void handleAutoEnhance(AutoEnhancePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof EnhancementMenu menu) {
                EnhancementFurnaceBlockEntity furnace = menu.getFurnace();
                if (payload.targetLevel() <= 0) {
                    furnace.setAutoTarget(0);
                    furnace.setAutoOwner(null);
                } else {
                    furnace.setAutoOwner(player.getUUID());
                    furnace.setAutoTarget(Math.min(payload.targetLevel(), 99));
                }
            }
        });
    }
}
