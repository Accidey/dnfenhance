package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DnfEnhanceMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DNF_ENHANCE_TAB =
            CREATIVE_MODE_TABS.register("dnf_enhance_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dnfenhance"))
                    .icon(() -> new ItemStack(ModBlocks.ENHANCEMENT_FURNACE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.ENHANCEMENT_FURNACE.get());
                        output.accept(ModItems.FURNACE_CARBON.get());
                        output.accept(ModItems.ADVANCED_CARBON.get());
                        output.accept(ModItems.LUCK_CHARM.get());
                        output.accept(ModItems.ENHANCE_TICKET_10.get());
                        output.accept(ModItems.ENHANCE_TICKET_11.get());
                        output.accept(ModItems.ENHANCE_TICKET_12.get());
                        output.accept(ModItems.ENHANCE_TICKET_13.get());
                        output.accept(ModItems.ENHANCE_TICKET_14.get());
                        output.accept(ModItems.ENHANCE_TICKET_15.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
