package com.xulai.dnfenhance;

import com.mojang.logging.LogUtils;
import com.xulai.dnfenhance.enhance.EnhanceConfig;
import com.xulai.dnfenhance.net.ModNetworking;
import com.xulai.dnfenhance.registry.ModBlockEntities;
import com.xulai.dnfenhance.registry.ModBlocks;
import com.xulai.dnfenhance.registry.ModCreativeTabs;
import com.xulai.dnfenhance.registry.ModDataComponents;
import com.xulai.dnfenhance.registry.ModItems;
import com.xulai.dnfenhance.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(DnfEnhanceMod.MODID)
public class DnfEnhanceMod {
    public static final String MODID = "dnfenhance";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DnfEnhanceMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(ModNetworking::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, EnhanceConfig.SPEC, "dnfenhance.toml");

        LOGGER.info("DNF Enhance mod loaded");
    }
}
