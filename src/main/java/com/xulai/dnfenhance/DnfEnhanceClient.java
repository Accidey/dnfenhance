package com.xulai.dnfenhance;

import com.xulai.dnfenhance.client.EnhancementScreen;
import com.xulai.dnfenhance.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = DnfEnhanceMod.MODID, dist = Dist.CLIENT)
public class DnfEnhanceClient {
    public DnfEnhanceClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener((RegisterMenuScreensEvent event) ->
                event.register(ModMenus.ENHANCEMENT_MENU.get(), EnhancementScreen::new));

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
