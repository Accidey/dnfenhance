package com.xulai.dnfenhance.block;

import com.xulai.dnfenhance.enhance.DecomposeConfig;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import com.xulai.dnfenhance.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public class DecomposerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_EQUIP = 0;
    public static final int SLOT_BUFFER_START = 1;
    public static final int BUFFER_SIZE = 9;
    public static final int SIZE = SLOT_BUFFER_START + BUFFER_SIZE;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress = 0;
    private boolean powered = false;
    private int litTimer = 0;

    public DecomposerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECOMPOSER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DecomposerBlockEntity be) {
        if (level.isClientSide()) return;
        be.powered = level.hasNeighborSignal(pos);
        if (be.litTimer > 0) be.litTimer--;
        be.updateLit();

        ItemStack equip = be.getItem(SLOT_EQUIP);
        boolean runnable = be.powered && !equip.isEmpty() && be.canDecompose(equip) && be.canInsertOutputs();
        if (!runnable) {
            be.progress = 0;
            return;
        }

        be.progress++;
        be.litTimer = 20;
        if (be.progress >= DecomposeConfig.workTicks()) {
            be.progress = 0;
            be.decompose();
        }
    }

    private boolean canDecompose(ItemStack equip) {
        return !DecomposeConfig.outputsFor(EnhanceLogic.getLevel(equip)).isEmpty();
    }

    private List<ItemStack> pendingOutputs() {
        return DecomposeConfig.outputsFor(EnhanceLogic.getLevel(this.getItem(SLOT_EQUIP)));
    }

    private boolean canInsertOutputs() {
        int[] space = new int[SIZE];
        for (int i = SLOT_BUFFER_START; i < SIZE; i++) {
            ItemStack in = this.items.get(i);
            space[i] = in.isEmpty() ? 64 : in.getMaxStackSize() - in.getCount();
        }
        for (ItemStack out : this.pendingOutputs()) {
            int remaining = out.getCount();
            for (int i = SLOT_BUFFER_START; i < SIZE && remaining > 0; i++) {
                ItemStack in = this.items.get(i);
                if (!in.isEmpty() && !ItemStack.isSameItemSameComponents(in, out)) continue;
                int moved = Math.min(remaining, Math.max(0, Math.min(out.getMaxStackSize(), space[i])));
                remaining -= moved;
                space[i] -= moved;
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private void insertOutputs() {
        for (ItemStack out : this.pendingOutputs()) {
            int remaining = out.getCount();
            for (int i = SLOT_BUFFER_START; i < SIZE && remaining > 0; i++) {
                ItemStack in = this.items.get(i);
                if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, out)) {
                    int moved = Math.min(remaining, in.getMaxStackSize() - in.getCount());
                    in.grow(moved);
                    remaining -= moved;
                }
            }
            for (int i = SLOT_BUFFER_START; i < SIZE && remaining > 0; i++) {
                if (this.items.get(i).isEmpty()) {
                    int moved = Math.min(remaining, out.getMaxStackSize());
                    this.items.set(i, out.copyWithCount(moved));
                    remaining -= moved;
                }
            }
        }
    }

    private void decompose() {
        ItemStack equip = this.getItem(SLOT_EQUIP);
        if (equip.isEmpty()) return;
        List<ItemStack> outputs = DecomposeConfig.outputsFor(EnhanceLogic.getLevel(equip));
        if (outputs.isEmpty() || !this.canInsertOutputs()) return;

        this.insertOutputs();
        this.setItem(SLOT_EQUIP, ItemStack.EMPTY);

        Level level = this.level;
        if (level != null) {
            level.playSound(null, this.worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1, this.worldPosition.getZ() + 0.5,
                        12, 0.3, 0.4, 0.3, 0.02);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1, this.worldPosition.getZ() + 0.5,
                        8, 0.3, 0.4, 0.3, 0.05);
            }
        }
        this.setChanged();
    }

    private void updateLit() {
        if (this.level == null) return;
        BlockState current = this.getBlockState();
        boolean lit = this.litTimer > 0;
        if (current.hasProperty(DecomposerBlock.LIT) && current.getValue(DecomposerBlock.LIT) != lit) {
            this.level.setBlockAndUpdate(this.worldPosition, current.setValue(DecomposerBlock.LIT, lit));
        }
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int value) {
        this.progress = value;
    }

    public int getWorkTicks() {
        return DecomposeConfig.workTicks();
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean value) {
        this.powered = value;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                        this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_EQUIP && EnhanceLogic.canEnhance(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        if (face == Direction.UP) {
            return new int[]{SLOT_EQUIP};
        }
        if (face == Direction.DOWN) {
            int[] slots = new int[BUFFER_SIZE];
            for (int i = 0; i < BUFFER_SIZE; i++) {
                slots[i] = SLOT_BUFFER_START + i;
            }
            return slots;
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return face == Direction.UP && slot == SLOT_EQUIP && this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return face == Direction.DOWN && slot >= SLOT_BUFFER_START;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("Progress", this.progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = Math.max(0, input.getIntOr("Progress", 0));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dnfenhance.decomposer");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new com.xulai.dnfenhance.menu.DecomposerMenu(containerId, inventory, this, this.worldPosition);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
