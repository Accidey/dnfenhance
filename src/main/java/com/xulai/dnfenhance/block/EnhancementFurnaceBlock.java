package com.xulai.dnfenhance.block;

import com.xulai.dnfenhance.menu.EnhancementMenu;
import com.xulai.dnfenhance.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class EnhancementFurnaceBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public EnhancementFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        Direction facing = state.getValue(FACING);
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double ox = facing.getStepX() * 0.55;
        double oz = facing.getStepZ() * 0.55;
        if (random.nextDouble() < 0.25) {
            double y = pos.getY() + 0.45 + random.nextDouble() * 0.3;
            double jitter = random.nextDouble() * 0.5 - 0.25;
            level.addParticle(ParticleTypes.SMOKE, cx + ox + jitter, y, cz + oz + jitter, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, cx + ox + jitter, y, cz + oz + jitter, 0.0, 0.0, 0.0);
        }
        if (random.nextDouble() < 0.08) {
            level.addParticle(ParticleTypes.ENCHANT, cx + ox * 0.6, pos.getY() + 0.8, cz + oz * 0.6, 0.0, 0.5, 0.0);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnhancementFurnaceBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        BlockEntityTicker<EnhancementFurnaceBlockEntity> ticker = EnhancementFurnaceBlockEntity::serverTick;
        return type == ModBlockEntities.ENHANCEMENT_FURNACE.get() ? (BlockEntityTicker<T>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof EnhancementFurnaceBlockEntity be) {
            player.openMenu(new SimpleMenuProviderBridge(be, pos), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    private record SimpleMenuProviderBridge(EnhancementFurnaceBlockEntity be, BlockPos pos)
            implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return be.getDisplayName();
        }

        @Override
        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                net.minecraft.world.entity.player.Inventory inventory, Player player) {
            return new EnhancementMenu(containerId, inventory, be, pos);
        }
    }
}
