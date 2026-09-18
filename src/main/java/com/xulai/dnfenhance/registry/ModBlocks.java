package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.block.EnhancementFurnaceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DnfEnhanceMod.MODID);

    public static final DeferredBlock<EnhancementFurnaceBlock> ENHANCEMENT_FURNACE = BLOCKS.registerBlock(
            "enhancement_furnace",
            EnhancementFurnaceBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.ANVIL)
                    .strength(5.0f, 1200.0f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(EnhancementFurnaceBlock.LIT) ? 13 : 0));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
