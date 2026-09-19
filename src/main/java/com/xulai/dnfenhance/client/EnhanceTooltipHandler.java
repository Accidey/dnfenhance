package com.xulai.dnfenhance.client;

import com.mojang.datafixers.util.Either;
import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = DnfEnhanceMod.MODID, value = Dist.CLIENT)
public final class EnhanceTooltipHandler {

    private EnhanceTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ProjectileWeaponItem)) return;

        int level = EnhanceLogic.getLevel(stack);
        if (level <= 0) return;

        int percent = (int) Math.round(EnhanceLogic.bonusForLevel(level) * 100);
        if (percent <= 0) return;

        event.getToolTip().add(Component.translatable("dnfenhance.tooltip.arrow", percent).withStyle(ChatFormatting.GRAY));
    }

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        int level = EnhanceLogic.prefixLevel(stack);
        if (level <= 0) return;

        boolean ticket = EnhanceLogic.isEnhanceTicket(stack);
        if (!ticket && stack.get(DataComponents.CUSTOM_NAME) == null) return;

        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        if (elements.isEmpty()) return;
        FormattedText line0 = elements.get(0).left().orElse(null);
        if (line0 == null) return;

        MutableComponent rebuilt = Component.empty().setStyle(EnhanceLogic.plainNameStyle());
        rebuilt.append(EnhanceNameFx.buildPrefix(level, Util.getMillis()));
        rebuilt.append(Component.literal(" ").setStyle(EnhanceLogic.plainNameStyle()));

        if (line0 instanceof Component node) {
            rebuilt.append(stripStaticPrefix(node, level));
        } else {
            rebuilt.append(Component.literal(line0.getString()));
        }
        elements.set(0, Either.left(rebuilt));
    }

    private static Component stripStaticPrefix(Component node, int level) {
        List<Component> siblings = node.getSiblings();
        if (siblings.isEmpty()) return node;

        int prefixLength = ("+" + level).length();
        if (siblings.size() == prefixLength + 1
                && siblings.get(0).getString().equals("+")
                && node.getString().startsWith("+")) {
            MutableComponent base = Component.empty().setStyle(EnhanceLogic.plainNameStyle());
            for (int i = prefixLength; i < siblings.size(); i++) {
                Component part = siblings.get(i);
                if (i == prefixLength) {
                    String text = part.getString();
                    if (text.startsWith(" ")) text = text.substring(1);
                    part = Component.literal(text).setStyle(EnhanceLogic.plainNameStyle());
                }
                base.append(part);
            }
            return base;
        }

        MutableComponent copy = null;
        for (int i = 0; i < siblings.size(); i++) {
            Component stripped = stripStaticPrefix(siblings.get(i), level);
            if (stripped != siblings.get(i)) {
                if (copy == null) {
                    copy = MutableComponent.create(node.getContents()).setStyle(node.getStyle());
                    for (int j = 0; j < i; j++) copy.append(siblings.get(j));
                }
                copy.append(stripped);
            } else if (copy != null) {
                copy.append(siblings.get(i));
            }
        }
        return copy != null ? copy : node;
    }
}
