package com.xulai.dnfenhance.enhance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class EnhanceTicketItem extends Item {
    private final int ticketLevel;

    public EnhanceTicketItem(int ticketLevel, Item.Properties properties) {
        super(properties);
        this.ticketLevel = ticketLevel;
    }

    public int getTicketLevel() {
        return this.ticketLevel;
    }

    @Override
    public Component getName(ItemStack stack) {
        String baseName = Component.translatable("item.dnfenhance.enhance_ticket").getString();
        return EnhanceLogic.buildDisplayName(this.ticketLevel, baseName);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack ticket = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack equip = player.getItemInHand(otherHand);

        if (!EnhanceLogic.canEnhance(equip)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("dnfenhance.msg.ticket_no_equip")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(ticket);
        }
        if (EnhanceLogic.getLevel(equip) >= this.ticketLevel) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("dnfenhance.msg.ticket_too_low")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(ticket);
        }

        player.getCooldowns().addCooldown(this, 10);

        if (!level.isClientSide) {
            ticket.shrink(1);

            KaiLiPigHelper.NearbyAura nearby = KaiLiPigHelper.scanAura(level, player.blockPosition());
            EnhanceLogic.AuraResult aura = EnhanceLogic.aura(nearby.variant(), nearby.count());
            double rate = aura.mustFail()
                    ? 0.0
                    : EnhanceLogic.applyAura(EnhanceLogic.ticketSuccessRate(this.ticketLevel), aura.count());
            rate = Math.max(0.0, Math.min(1.0, rate));

            boolean success = player.getRandom().nextDouble() < rate;
            if (success) {
                EnhanceLogic.applyLevel(equip, this.ticketLevel);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.1F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.6F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
                }
                player.displayClientMessage(Component.translatable("dnfenhance.msg.ticket_success", this.ticketLevel)
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.6F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
                }
                player.displayClientMessage(Component.translatable("dnfenhance.msg.ticket_failed")
                        .withStyle(ChatFormatting.RED), false);
            }
        }
        return InteractionResultHolder.sidedSuccess(ticket, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        String rate;
        try {
            rate = Math.round(EnhanceLogic.ticketSuccessRate(this.ticketLevel) * 100.0) + "%";
        } catch (IllegalStateException ex) {
            rate = "100%";
        }
        tooltip.add(Component.translatable("dnfenhance.item.enhance_ticket.tooltip", rate, this.ticketLevel)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("dnfenhance.item.enhance_ticket.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("dnfenhance.item.enhance_ticket.upgrade")
                .withStyle(ChatFormatting.GRAY));
    }
}
