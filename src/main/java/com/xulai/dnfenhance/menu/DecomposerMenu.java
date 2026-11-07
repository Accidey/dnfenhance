package com.xulai.dnfenhance.menu;

import com.xulai.dnfenhance.block.DecomposerBlockEntity;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import com.xulai.dnfenhance.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DecomposerMenu extends AbstractContainerMenu {
    public static final int SLOT_EQUIP = DecomposerBlockEntity.SLOT_EQUIP;
    public static final int SLOT_BUFFER_START = DecomposerBlockEntity.SLOT_BUFFER_START;
    public static final int MACHINE_SLOTS = DecomposerBlockEntity.SIZE;

    private final DecomposerBlockEntity decomposer;
    private final BlockPos decomposerPos;
    private final ContainerData data;

    public DecomposerMenu(int containerId, Inventory playerInventory,
            DecomposerBlockEntity decomposer, BlockPos pos) {
        super(ModMenus.DECOMPOSER_MENU.get(), containerId);
        this.decomposer = decomposer;
        this.decomposerPos = pos;

        this.addSlot(new Slot(decomposer, SLOT_EQUIP, 48, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnhanceLogic.canEnhance(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < DecomposerBlockEntity.BUFFER_SIZE; i++) {
            this.addSlot(new Slot(decomposer, SLOT_BUFFER_START + i, 8 + i * 18, 62) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 178));
        }

        this.addDataSlots(this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> decomposer.getProgress();
                    case 1 -> decomposer.getWorkTicks();
                    case 2 -> decomposer.isPowered() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> decomposer.setProgress(value);
                    case 2 -> decomposer.setPowered(value != 0);
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
    }

    public DecomposerBlockEntity getDecomposer() {
        return this.decomposer;
    }

    public BlockPos getDecomposerPos() {
        return this.decomposerPos;
    }

    public ItemStack getEquipStack() {
        return this.decomposer.getItem(SLOT_EQUIP);
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getWorkTicks() {
        return this.data.get(1);
    }

    public boolean isPowered() {
        return this.data.get(2) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else {
                if (EnhanceLogic.canEnhance(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_EQUIP, SLOT_EQUIP + 1, false)) return ItemStack.EMPTY;
                } else if (index < MACHINE_SLOTS + 27) {
                    if (!this.moveItemStackTo(stack, MACHINE_SLOTS + 27, MACHINE_SLOTS + 36, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.decomposer.stillValid(player);
    }
}
