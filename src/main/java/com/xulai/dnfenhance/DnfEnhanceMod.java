package com.xulai.dnfenhance;

import com.mojang.logging.LogUtils;
import com.xulai.dnfenhance.enhance.EnhanceConfig;
import com.xulai.dnfenhance.enhance.EnhanceEvents;
import com.xulai.dnfenhance.net.ModNetworking;
import com.xulai.dnfenhance.registry.ModBlockEntities;
import com.xulai.dnfenhance.registry.ModBlocks;
import com.xulai.dnfenhance.registry.ModCreativeTabs;
import com.xulai.dnfenhance.registry.ModItems;
import com.xulai.dnfenhance.registry.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DnfEnhanceMod.MODID)
public class DnfEnhanceMod {
    public static final String MODID = "dnfenhance";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DnfEnhanceMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        ModNetworking.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EnhanceConfig.SPEC, "dnfenhance.toml");

        MinecraftForge.EVENT_BUS.register(EnhanceEvents.class);

        LOGGER.info("DNF Enhance mod loaded");
    }
}
