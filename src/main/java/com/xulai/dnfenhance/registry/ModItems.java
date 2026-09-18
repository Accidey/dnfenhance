package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.enhance.EnhanceTicketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DnfEnhanceMod.MODID);

    public static final DeferredItem<BlockItem> ENHANCEMENT_FURNACE_ITEM =
            ITEMS.registerSimpleBlockItem("enhancement_furnace", ModBlocks.ENHANCEMENT_FURNACE);

    public static final DeferredItem<Item> FURNACE_CARBON = ITEMS.registerSimpleItem("furnace_carbon");

    public static final DeferredItem<Item> ADVANCED_CARBON = ITEMS.registerSimpleItem("advanced_furnace_carbon");

    public static final DeferredItem<Item> LUCK_CHARM = ITEMS.registerSimpleItem("luck_charm");

    public static final DeferredItem<Item> ENHANCE_TICKET_10 = ITEMS.registerItem("enhance_ticket_10",
            properties -> new EnhanceTicketItem(10, properties));
    public static final DeferredItem<Item> ENHANCE_TICKET_11 = ITEMS.registerItem("enhance_ticket_11",
            properties -> new EnhanceTicketItem(11, properties));
    public static final DeferredItem<Item> ENHANCE_TICKET_12 = ITEMS.registerItem("enhance_ticket_12",
            properties -> new EnhanceTicketItem(12, properties));
    public static final DeferredItem<Item> ENHANCE_TICKET_13 = ITEMS.registerItem("enhance_ticket_13",
            properties -> new EnhanceTicketItem(13, properties));
    public static final DeferredItem<Item> ENHANCE_TICKET_14 = ITEMS.registerItem("enhance_ticket_14",
            properties -> new EnhanceTicketItem(14, properties));
    public static final DeferredItem<Item> ENHANCE_TICKET_15 = ITEMS.registerItem("enhance_ticket_15",
            properties -> new EnhanceTicketItem(15, properties));

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
