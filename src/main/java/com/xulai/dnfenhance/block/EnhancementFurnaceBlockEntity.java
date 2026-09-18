package com.xulai.dnfenhance.block;

import com.xulai.dnfenhance.enhance.EnhanceConfig;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import com.xulai.dnfenhance.enhance.KaiLiPigHelper;
import com.xulai.dnfenhance.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class EnhancementFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_CARBON = 0;
    public static final int SLOT_EQUIP = 1;
    public static final int SLOT_PROTECT = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int autoTarget = 0;
    private boolean autoCompleted = false;
    private int tickCounter = 0;
    private int litTimer = 0;
    private UUID autoOwner = null;
    private double kaiLiLuck = 0.0;

    public EnhancementFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(com.xulai.dnfenhance.registry.ModBlockEntities.ENHANCEMENT_FURNACE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnhancementFurnaceBlockEntity be) {
        if (level.isClientSide()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (be.autoTarget > 0 && powered) {
            be.litTimer = 40;
        }
        be.updateLit();
        if (be.autoTarget <= 0) return;
        if (!powered) return;
        be.tickCounter++;
        if (be.tickCounter % 20 != 0) return;

        ItemStack equip = be.getItem(SLOT_EQUIP);
        if (!EnhanceLogic.canEnhance(equip)) {
            be.autoTarget = 0;
            be.setChanged();
            return;
        }
        int current = EnhanceLogic.getLevel(equip);
        int effectiveTarget = Math.min(be.autoTarget, EnhanceLogic.maxLevel());
        if (current >= effectiveTarget) {
            be.autoTarget = 0;
            be.autoCompleted = true;
            be.setChanged();
            return;
        }

        if (current + 1 > EnhanceLogic.maxLevel()) {
            be.autoTarget = 0;
            be.autoCompleted = true;
            be.setChanged();
            return;
        }

        int target = current + 1;
        boolean wouldDestroy = EnhanceLogic.penaltyType(target) == EnhanceLogic.Penalty.DESTROY
                && !EnhanceLogic.isProtectionCharm(be.getItem(SLOT_PROTECT));
        if (wouldDestroy) {
            be.autoTarget = 0;
            be.setChanged();
            if (be.autoOwner != null && be.level != null && be.level.getServer() != null) {
                ServerPlayer owner = be.level.getServer().getPlayerList().getPlayer(be.autoOwner);
                if (owner != null) {
                    owner.sendSystemMessage(
                            Component.translatable("dnfenhance.msg.auto_stopped", equip.getHoverName())
                                    .withStyle(ChatFormatting.YELLOW));
                }
            }
            return;
        }

        KaiLiPigHelper.NearbyAura nearby = KaiLiPigHelper.scanAura(level, pos);
        if (EnhanceLogic.aura(nearby.variant(), nearby.count()).mustFail()) {
            be.autoTarget = 0;
            be.setChanged();
            if (be.autoOwner != null && be.level != null && be.level.getServer() != null) {
                ServerPlayer owner = be.level.getServer().getPlayerList().getPlayer(be.autoOwner);
                if (owner != null) {
                    owner.sendSystemMessage(
                            Component.translatable("dnfenhance.msg.auto_stopped_aura")
                                    .withStyle(ChatFormatting.YELLOW));
                }
            }
            return;
        }

        be.enhanceOnce(null);
    }

    public void enhanceOnce(ServerPlayer player) {
        ItemStack equip = this.getItem(SLOT_EQUIP);
        if (!EnhanceLogic.canEnhance(equip)) return;
        this.litTimer = 40;

        int current = EnhanceLogic.getLevel(equip);
        int target = current + 1;
        if (target > EnhanceLogic.maxLevel()) {
            this.autoTarget = 0;
            if (player != null) {
                player.displayClientMessage(Component.translatable("dnfenhance.msg.max_level"), true);
            }
            return;
        }

        ItemStack carbon = this.getItem(SLOT_CARBON);
        int cost = EnhanceLogic.carbonCost(target);
        if (!EnhanceLogic.isCarbon(carbon) || carbon.getCount() < cost) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("dnfenhance.msg.not_enough_carbon", cost), true);
            }
            return;
        }

        boolean advanced = EnhanceLogic.isAdvancedCarbon(carbon);
        double rate = EnhanceLogic.successRate(target) + (advanced ? EnhanceConfig.ADVANCED_BONUS.get() : 0.0);
        ServerPlayer luckHolder = player;
        if (luckHolder == null && this.autoOwner != null && this.level != null && this.level.getServer() != null) {
            luckHolder = this.level.getServer().getPlayerList().getPlayer(this.autoOwner);
        }
        if (luckHolder != null && luckHolder.hasEffect(MobEffects.LUCK)) {
            rate += EnhanceConfig.LUCK_BONUS.get();
        }
        rate = Math.max(0.0, Math.min(1.0, rate));

        Level level = this.level;
        BlockPos pos = this.worldPosition;

        KaiLiPigHelper.NearbyAura nearby = KaiLiPigHelper.scanAura(level, pos);
        EnhanceLogic.AuraResult aura = EnhanceLogic.aura(nearby.variant(), nearby.count());
        rate = aura.mustFail() ? 0.0 : EnhanceLogic.applyAura(rate, aura.count());

        carbon.shrink(cost);

        boolean success = (player != null ? player.getRandom() : level.random).nextDouble() < rate;
        if (player != null) {
            this.autoCompleted = false;
        }

        boolean summoned = maybeSummonKaiLi(level, pos, player);

        if (success) {
            if (!summoned) {
                this.kaiLiLuck = Math.min(1.0, this.kaiLiLuck + EnhanceConfig.PIG_LUCK_PER_SUCCESS.get());
            }
            EnhanceLogic.applyLevel(equip, target);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.1f);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.6f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.02);
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.05);
            }
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("dnfenhance.msg.success", target).withStyle(ChatFormatting.GREEN), false);
            }
        } else if (aura.resetToZero()) {
                EnhanceLogic.applyLevel(equip, 0);
                this.autoTarget = 0;
                level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.6f);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 15, 0.3, 0.4, 0.3, 0.02);
                }
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable("dnfenhance.msg.fail_reset").withStyle(ChatFormatting.DARK_RED), false);
                }
            } else {
            switch (EnhanceLogic.penaltyType(target)) {
                case DESTROY -> {
                    ItemStack protect = this.getItem(SLOT_PROTECT);
                    if (EnhanceLogic.isProtectionCharm(protect)) {
                        if (EnhanceConfig.PROTECTION_CONSUME.get()) {
                            protect.shrink(1);
                        }
                        level.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 40, 0.5, 0.6, 0.5, 0.05);
                            serverLevel.sendParticles(ParticleTypes.END_ROD,
                                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 10, 0.4, 0.5, 0.4, 0.03);
                        }
                        if (player != null) {
                            player.displayClientMessage(
                                    Component.translatable("dnfenhance.msg.protected").withStyle(ChatFormatting.AQUA), false);
                        }
                    } else {
                        this.setItem(SLOT_EQUIP, net.minecraft.world.item.ItemStack.EMPTY);
                        this.autoTarget = 0;
                        level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
                        level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.4, 0.5, 0.4, 0.02);
                        }
                        if (player != null) {
                            player.displayClientMessage(
                                    Component.translatable("dnfenhance.msg.destroy").withStyle(ChatFormatting.RED), false);
                        }
                    }
                }
                case DOWNGRADE -> {
                    int newLevel = Math.max(0, current - EnhanceLogic.penaltyDowngrade(target));
                    EnhanceLogic.applyLevel(equip, newLevel);
                    level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.6f);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 15, 0.3, 0.4, 0.3, 0.02);
                    }
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable("dnfenhance.msg.fail_down", newLevel).withStyle(ChatFormatting.RED), false);
                    }
                }
                default -> {
                    level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.2f);
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable("dnfenhance.msg.fail_safe").withStyle(ChatFormatting.YELLOW), false);
                    }
                }
            }
        }

        this.setChanged();
    }

    private boolean maybeSummonKaiLi(Level level, BlockPos pos, ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!EnhanceConfig.PIG_ENABLED.get()) return false;
        if (KaiLiPigHelper.atCapacity(serverLevel, pos)) return false;

        double luck = Math.max(0.0, Math.min(1.0, this.kaiLiLuck));
        double roll = serverLevel.getRandom().nextDouble();
        KaiLiPigHelper.KaiLiVariant variant;
        if (roll < EnhanceConfig.PIG_MASTER_CHANCE.get() + luck) {
            variant = KaiLiPigHelper.KaiLiVariant.MASTER;
        } else if (roll < EnhanceConfig.PIG_BLACKENED_CHANCE.get() + luck) {
            variant = KaiLiPigHelper.KaiLiVariant.BLACKENED;
        } else if (roll < EnhanceConfig.PIG_SUMMON_CHANCE.get() + luck) {
            variant = KaiLiPigHelper.KaiLiVariant.NORMAL;
        } else {
            return false;
        }

        var pig = KaiLiPigHelper.summon(serverLevel, pos, variant);
        if (pig == null) return false;
        this.kaiLiLuck = 0.0;

        serverLevel.playSound(null, pos, SoundEvents.PIG_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.7f);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                pig.getX(), pig.getY() + 0.6, pig.getZ(), 25, 0.4, 0.4, 0.4, 0.08);
        if (variant == KaiLiPigHelper.KaiLiVariant.BLACKENED
                || variant == KaiLiPigHelper.KaiLiVariant.MASTER) {
            net.minecraft.network.chat.MutableComponent broadcast = Component.translatable(variant
                    == KaiLiPigHelper.KaiLiVariant.BLACKENED
                    ? "dnfenhance.msg.kai_li_blackened_summoned"
                    : "dnfenhance.msg.kai_li_master_summoned").withStyle(ChatFormatting.LIGHT_PURPLE);
            for (ServerPlayer recipient : serverLevel.players()) {
                recipient.displayClientMessage(broadcast, false);
            }
        } else if (player != null) {
            player.displayClientMessage(Component.translatable("dnfenhance.msg.kai_li_summoned")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        return true;
    }

    private void updateLit() {
        if (this.litTimer > 0) this.litTimer--;
        if (this.level == null) return;
        BlockState current = this.getBlockState();
        boolean lit = this.litTimer > 0;
        if (current.hasProperty(EnhancementFurnaceBlock.LIT)
                && current.getValue(EnhancementFurnaceBlock.LIT) != lit) {
            this.level.setBlockAndUpdate(this.worldPosition, current.setValue(EnhancementFurnaceBlock.LIT, lit));
        }
    }

    public int getAutoTarget() {
        return this.autoTarget;
    }

    public void setAutoTarget(int target) {
        this.autoTarget = Math.max(0, target);
        if (this.autoTarget > 0) {
            this.autoCompleted = false;
        }
        this.setChanged();
    }

    public void setAutoOwner(UUID owner) {
        this.autoOwner = owner;
        this.setChanged();
    }

    public boolean isAutoCompleted() {
        return this.autoCompleted;
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
        if (slot == SLOT_EQUIP) {
            this.autoCompleted = false;
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
        return switch (slot) {
            case SLOT_CARBON -> EnhanceLogic.isCarbon(stack);
            case SLOT_EQUIP -> EnhanceLogic.canEnhance(stack);
            case SLOT_PROTECT -> EnhanceLogic.isProtectionCharm(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        if (face == Direction.DOWN) {
            return new int[]{SLOT_EQUIP};
        }
        if (face == Direction.UP) {
            return new int[]{SLOT_EQUIP};
        }
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof EnhancementFurnaceBlock)) {
            return new int[0];
        }
        Direction facing = state.getValue(EnhancementFurnaceBlock.FACING);
        if (face == facing.getClockWise()) {
            return new int[]{SLOT_CARBON};
        }
        if (face == facing.getCounterClockWise()) {
            return new int[]{SLOT_PROTECT};
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (face == Direction.DOWN) return false;
        return this.canPlaceItem(slot, stack) && this.getSlotsForFace(face) != null
                && contains(this.getSlotsForFace(face), slot);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return face == Direction.DOWN && slot == SLOT_EQUIP && this.autoCompleted
                && !stack.isEmpty() && EnhanceLogic.getLevel(stack) > 0;
    }

    private static boolean contains(int[] slots, int slot) {
        for (int s : slots) {
            if (s == slot) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("AutoTarget", this.autoTarget);
        output.putBoolean("AutoCompleted", this.autoCompleted);
        if (this.autoOwner != null) {
            output.store("AutoOwner", UUIDUtil.CODEC, this.autoOwner);
        }
        output.putDouble("KaiLiLuck", this.kaiLiLuck);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.autoTarget = input.getIntOr("AutoTarget", 0);
        this.autoCompleted = input.getBooleanOr("AutoCompleted", false);
        this.autoOwner = input.read("AutoOwner", UUIDUtil.CODEC).orElse(null);
        this.kaiLiLuck = Math.max(0.0, Math.min(1.0, input.getDoubleOr("KaiLiLuck", 0.0)));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dnfenhance.enhancement_furnace");
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
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
            net.minecraft.world.entity.player.Inventory inventory) {
        return new com.xulai.dnfenhance.menu.EnhancementMenu(containerId, inventory, this, this.worldPosition);
    }

}
