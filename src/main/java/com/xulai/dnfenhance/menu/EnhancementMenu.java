package com.xulai.dnfenhance.menu;

import com.xulai.dnfenhance.block.EnhancementFurnaceBlockEntity;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import com.xulai.dnfenhance.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnhancementMenu extends AbstractContainerMenu {
    public static final int SLOT_CARBON = 0;
    public static final int SLOT_EQUIP = 1;
    public static final int SLOT_PROTECT = 2;

    private final EnhancementFurnaceBlockEntity furnace;
    private final BlockPos furnacePos;
    private int auraCount = 0;
    private int auraVariant = -1;

    public EnhancementMenu(int containerId, Inventory playerInventory,
            EnhancementFurnaceBlockEntity furnace, BlockPos pos) {
        super(ModMenus.ENHANCEMENT_MENU.get(), containerId);
        this.furnace = furnace;
        this.furnacePos = pos;

        this.addSlot(new Slot(furnace, SLOT_CARBON, 20, 28) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnhanceLogic.isCarbon(stack);
            }
        });
        this.addSlot(new Slot(furnace, SLOT_EQUIP, 70, 28) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnhanceLogic.canEnhance(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(furnace, SLOT_PROTECT, 120, 28) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnhanceLogic.isProtectionCharm(stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 152 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 206));
        }

        this.addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> furnace.getAutoTarget();
                    case 1 -> furnace.getAuraCount();
                    case 2 -> furnace.getAuraVariant();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> furnace.setAutoTarget(value);
                    case 1 -> auraCount = value;
                    case 2 -> auraVariant = value;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
    }

    public int getAuraCount() {
        return this.auraCount;
    }

    public int getAuraVariant() {
        return this.auraVariant;
    }

    public EnhancementFurnaceBlockEntity getFurnace() {
        return this.furnace;
    }

    public BlockPos getFurnacePos() {
        return this.furnacePos;
    }

    public int getAutoTarget() {
        return this.furnace.getAutoTarget();
    }

    public ItemStack getEquipStack() {
        return this.furnace.getItem(SLOT_EQUIP);
    }

    public ItemStack getCarbonStack() {
        return this.furnace.getItem(SLOT_CARBON);
    }

    public ItemStack getProtectStack() {
        return this.furnace.getItem(SLOT_PROTECT);
    }

    public void enhance(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            this.furnace.enhanceOnce(serverPlayer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
            } else {
                if (EnhanceLogic.canEnhance(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_EQUIP, SLOT_EQUIP + 1, false)) return ItemStack.EMPTY;
                } else if (EnhanceLogic.isCarbon(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_CARBON, SLOT_CARBON + 1, false)) return ItemStack.EMPTY;
                } else if (EnhanceLogic.isProtectionCharm(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_PROTECT, SLOT_PROTECT + 1, false)) return ItemStack.EMPTY;
                } else if (index < 30) {
                    if (!this.moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
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
        return this.furnace.stillValid(player);
    }
}
