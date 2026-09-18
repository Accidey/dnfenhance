package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.block.EnhancementFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DnfEnhanceMod.MODID);

    public static final RegistryObject<BlockEntityType<EnhancementFurnaceBlockEntity>> ENHANCEMENT_FURNACE =
            BLOCK_ENTITIES.register("enhancement_furnace",
                    () -> BlockEntityType.Builder.of(
                            EnhancementFurnaceBlockEntity::new,
                            ModBlocks.ENHANCEMENT_FURNACE.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
