package com.xulai.dnfenhance.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = DnfEnhanceMod.MODID)
public final class EnhanceCommands {
    private EnhanceCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dnfenhance")
                .then(Commands.literal("enhance")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 99))
                                .executes(ctx -> enhanceHeld(
                                        ctx.getSource(), IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("attributes")
                        .executes(ctx -> listAttributes(ctx.getSource()))));
    }

    private static int enhanceHeld(CommandSourceStack source, int requested) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("该命令只能由玩家在游戏内执行"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("请先在主手持有要强化的装备"));
            return 0;
        }

        int level = Math.min(requested, EnhanceLogic.maxLevel());
        EnhanceLogic.applyLevel(stack, level);
        String itemName = stack.getHoverName().getString();
        int finalLevel = level;

        if (finalLevel <= 0) {
            source.sendSuccess(() -> Component.literal("已清除强化：" + itemName), true);
        } else {
            source.sendSuccess(() -> Component.literal("已将 " + itemName + " 直接强化至 +" + finalLevel), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listAttributes(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("该命令只能由玩家在游戏内执行"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            stack = player.getOffhandItem();
        }
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("请先在手上持有要查看的装备")
                    .withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("—— " + stack.getHoverName().getString()
                + " 的属性修饰符（点击任意一行复制其 ID）——").withStyle(ChatFormatting.GOLD));

        List<ItemAttributeModifiers.Entry> entries = stack.getAttributeModifiers().modifiers();
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.literal("该物品没有任何属性修饰符")
                    .withStyle(ChatFormatting.GRAY));
            return Command.SINGLE_SUCCESS;
        }

        for (ItemAttributeModifiers.Entry entry : entries) {
            Holder<Attribute> attribute = entry.attribute();
            AttributeModifier modifier = entry.modifier();
            String id = attribute.unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("unknown");
            String displayName = Component.translatable(attribute.value().getDescriptionId()).getString();

            String state;
            ChatFormatting color;
            if (EnhanceLogic.isAttributeBlacklisted(attribute)) {
                state = "[已禁用]";
                color = ChatFormatting.DARK_GRAY;
            } else if (modifier.amount() <= 0.0) {
                state = "[非正值·不强化]";
                color = ChatFormatting.GRAY;
            } else {
                state = "[已强化]";
                color = ChatFormatting.AQUA;
            }

            String text = amount(modifier) + " " + displayName
                    + "  § " + id
                    + "  § " + modifier.operation().name()
                    + "  " + state;

            player.sendSystemMessage(Component.literal(text).withStyle(Style.EMPTY
                    .withColor(color)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal("点击复制 " + id)))));
            player.sendSystemMessage(Component.empty());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String amount(AttributeModifier modifier) {
        double value = modifier.amount();
        if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                || modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            return trim(value * 100.0) + "%";
        }
        return trim(value);
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
