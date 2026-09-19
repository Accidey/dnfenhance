package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.block.EnhancementFurnaceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DnfEnhanceMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnhancementFurnaceBlockEntity>> ENHANCEMENT_FURNACE =
            BLOCK_ENTITIES.register("enhancement_furnace",
                    () -> new BlockEntityType<>(
                            EnhancementFurnaceBlockEntity::new,
                            ModBlocks.ENHANCEMENT_FURNACE.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
