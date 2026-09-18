package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.enhance.EnhanceTicketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DnfEnhanceMod.MODID);

    public static final RegistryObject<BlockItem> ENHANCEMENT_FURNACE_ITEM =
            ITEMS.register("enhancement_furnace",
                    () -> new BlockItem(ModBlocks.ENHANCEMENT_FURNACE.get(), new Item.Properties()));

    public static final RegistryObject<Item> FURNACE_CARBON =
            ITEMS.register("furnace_carbon", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ADVANCED_CARBON =
            ITEMS.register("advanced_furnace_carbon", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LUCK_CHARM =
            ITEMS.register("luck_charm", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ENHANCE_TICKET_10 =
            ITEMS.register("enhance_ticket_10",
                    () -> new EnhanceTicketItem(10, new Item.Properties()));
    public static final RegistryObject<Item> ENHANCE_TICKET_11 =
            ITEMS.register("enhance_ticket_11",
                    () -> new EnhanceTicketItem(11, new Item.Properties()));
    public static final RegistryObject<Item> ENHANCE_TICKET_12 =
            ITEMS.register("enhance_ticket_12",
                    () -> new EnhanceTicketItem(12, new Item.Properties()));
    public static final RegistryObject<Item> ENHANCE_TICKET_13 =
            ITEMS.register("enhance_ticket_13",
                    () -> new EnhanceTicketItem(13, new Item.Properties()));
    public static final RegistryObject<Item> ENHANCE_TICKET_14 =
            ITEMS.register("enhance_ticket_14",
                    () -> new EnhanceTicketItem(14, new Item.Properties()));
    public static final RegistryObject<Item> ENHANCE_TICKET_15 =
            ITEMS.register("enhance_ticket_15",
                    () -> new EnhanceTicketItem(15, new Item.Properties()));

    public static Item ticketItem(int level) {
        return switch (level) {
            case 10 -> ENHANCE_TICKET_10.get();
            case 11 -> ENHANCE_TICKET_11.get();
            case 12 -> ENHANCE_TICKET_12.get();
            case 13 -> ENHANCE_TICKET_13.get();
            case 14 -> ENHANCE_TICKET_14.get();
            case 15 -> ENHANCE_TICKET_15.get();
            default -> null;
        };
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
