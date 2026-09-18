package com.xulai.dnfenhance.registry;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.block.EnhancementFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DnfEnhanceMod.MODID);

    public static final RegistryObject<EnhancementFurnaceBlock> ENHANCEMENT_FURNACE =
            BLOCKS.register("enhancement_furnace", () -> new EnhancementFurnaceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .sound(SoundType.ANVIL)
                            .strength(5.0f, 1200.0f)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> state.getValue(EnhancementFurnaceBlock.LIT) ? 13 : 0)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
