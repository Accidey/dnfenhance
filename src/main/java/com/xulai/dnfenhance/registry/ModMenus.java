package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.menu.EnhancementMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DnfEnhanceMod.MODID);

    public static final RegistryObject<MenuType<EnhancementMenu>> ENHANCEMENT_MENU =
            MENUS.register("enhancement_furnace",
                    () -> IForgeMenuType.create((windowId, inventory, buf) -> {
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
