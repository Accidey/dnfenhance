package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.menu.EnhancementMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DnfEnhanceMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnhancementMenu>> ENHANCEMENT_MENU =
            MENUS.register("enhancement_furnace",
                    () -> IMenuTypeExtension.create((windowId, inventory, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        var be = inventory.player.level().getBlockEntity(pos);
                        if (be instanceof com.xulai.dnfenhance.block.EnhancementFurnaceBlockEntity furnace) {
                            return new EnhancementMenu(windowId, inventory, furnace, pos);
                        }
                        return null;
                    }));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
