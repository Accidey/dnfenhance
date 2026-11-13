package com.xulai.dnfenhance.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.enhance.EnhanceConfig;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = DnfEnhanceMod.MODID)
public final class EnhanceCommands {
    private EnhanceCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dnfenhance")
                .then(Commands.literal("enhance")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 99))
                                .executes(ctx -> enhanceHeld(
                                        ctx.getSource(), IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("attributes")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> listAttributes(ctx.getSource())))
                .then(Commands.literal("mobBlacklist")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("add")
                                .executes(ctx -> blacklistMutate(ctx.getSource(), true)))
                        .then(Commands.literal("remove")
                                .executes(ctx -> blacklistMutate(ctx.getSource(), false)))
                        .then(Commands.literal("list")
                                .executes(ctx -> blacklistList(ctx.getSource()))))
                .then(itemFilterBranch("itemBlacklist", false))
                .then(itemFilterBranch("itemWhitelist", true)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> itemFilterBranch(String name, boolean whitelist) {
        return Commands.literal(name)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("add")
                        .executes(ctx -> itemFilterMutate(ctx.getSource(), whitelist, true, null))
                        .then(Commands.argument("pattern", StringArgumentType.word())
                                .executes(ctx -> itemFilterMutate(ctx.getSource(), whitelist, true,
                                        StringArgumentType.getString(ctx, "pattern")))))
                .then(Commands.literal("remove")
                        .executes(ctx -> itemFilterMutate(ctx.getSource(), whitelist, false, null))
                        .then(Commands.argument("pattern", StringArgumentType.word())
                                .executes(ctx -> itemFilterMutate(ctx.getSource(), whitelist, false,
                                        StringArgumentType.getString(ctx, "pattern")))))
                .then(Commands.literal("list")
                        .executes(ctx -> itemFilterList(ctx.getSource(), whitelist)));
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
                    .map(key -> key.identifier().toString())
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
                    .withClickEvent(new ClickEvent.CopyToClipboard(id))
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.literal("点击复制 " + id)))));
            player.sendSystemMessage(Component.empty());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int blacklistMutate(CommandSourceStack source, boolean add) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("dnfenhance.command.mobBlacklist.onlyPlayer"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        EntityType<?> type = SpawnEggItem.getType(stack);
        if (type == null) {
            source.sendFailure(Component.translatable("dnfenhance.command.mobBlacklist.needEgg"));
            return 0;
        }
        Identifier loc = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (loc == null) {
            source.sendFailure(Component.translatable("dnfenhance.command.mobBlacklist.needEgg"));
            return 0;
        }
        String id = loc.toString();
        List<String> entries = new ArrayList<>();
        for (String raw : EnhanceConfig.MOB_GEAR_BLACKLIST.get()) {
            if (raw != null && !raw.isBlank()) entries.add(raw.trim());
        }
        if (add) {
            if (entries.contains(id)) {
                source.sendFailure(Component.translatable("dnfenhance.command.mobBlacklist.already", id));
                return 0;
            }
            entries.add(id);
        } else {
            if (!entries.remove(id)) {
                source.sendFailure(Component.translatable("dnfenhance.command.mobBlacklist.notFound", id));
                return 0;
            }
        }
        EnhanceConfig.MOB_GEAR_BLACKLIST.set(entries);
        EnhanceConfig.SPEC.save();
        String finalId = id;
        if (add) {
            source.sendSuccess(() -> Component.translatable("dnfenhance.command.mobBlacklist.added", finalId), true);
        } else {
            source.sendSuccess(() -> Component.translatable("dnfenhance.command.mobBlacklist.removed", finalId), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int blacklistList(CommandSourceStack source) {
        List<? extends String> entries = EnhanceConfig.MOB_GEAR_BLACKLIST.get();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("dnfenhance.command.mobBlacklist.empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return Command.SINGLE_SUCCESS;
        }
        source.sendSuccess(() -> Component.translatable("dnfenhance.command.mobBlacklist.header")
                .withStyle(ChatFormatting.GOLD), false);
        for (String entry : entries) {
            source.sendSuccess(() -> Component.literal(entry).withStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent.CopyToClipboard(entry))
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.translatable("dnfenhance.command.mobBlacklist.copyHint", entry)))), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static List<String> itemFilterEntries(boolean whitelist) {
        List<String> entries = new ArrayList<>();
        List<? extends String> configEntries = whitelist
                ? EnhanceConfig.MOB_ITEM_WHITELIST.get()
                : EnhanceConfig.MOB_ITEM_BLACKLIST.get();
        for (String raw : configEntries) {
            if (raw != null && !raw.isBlank()) entries.add(raw.trim());
        }
        return entries;
    }

    private static Component listName(boolean whitelist) {
        return Component.translatable(whitelist
                ? "dnfenhance.command.itemFilter.listName.whitelist"
                : "dnfenhance.command.itemFilter.listName.blacklist");
    }

    private static void saveItemFilter(boolean whitelist, List<String> entries) {
        if (whitelist) {
            EnhanceConfig.MOB_ITEM_WHITELIST.set(entries);
        } else {
            EnhanceConfig.MOB_ITEM_BLACKLIST.set(entries);
        }
        EnhanceConfig.SPEC.save();
    }

    private static int itemFilterMutate(CommandSourceStack source, boolean whitelist, boolean add, String pattern) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("dnfenhance.command.itemFilter.onlyPlayer"));
            return 0;
        }
        List<String> entries = itemFilterEntries(whitelist);
        if (add) {
            String entry;
            if (pattern == null) {
                ItemStack stack = player.getMainHandItem();
                if (stack.isEmpty()) {
                    source.sendFailure(Component.translatable("dnfenhance.command.itemFilter.needItem"));
                    return 0;
                }
                Identifier loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (loc == null) {
                    source.sendFailure(Component.translatable("dnfenhance.command.itemFilter.needItem"));
                    return 0;
                }
                entry = loc.toString();
                for (String existing : entries) {
                    if (EnhanceLogic.itemEntryMatches(existing, stack)) {
                        source.sendFailure(Component.translatable(
                                "dnfenhance.command.itemFilter.coveredBy", entry, existing));
                        return 0;
                    }
                }
            } else {
                if (!EnhanceLogic.isValidItemEntry(pattern)) {
                    source.sendFailure(Component.translatable(
                            "dnfenhance.command.itemFilter.invalidPattern", pattern));
                    return 0;
                }
                entry = pattern.trim().toLowerCase(Locale.ROOT);
            }
            if (entries.contains(entry)) {
                String finalEntry = entry;
                source.sendFailure(Component.translatable(
                        "dnfenhance.command.itemFilter.already", finalEntry, listName(whitelist)));
                return 0;
            }
            entries.add(entry);
            saveItemFilter(whitelist, entries);
            String added = entry;
            source.sendSuccess(() -> Component.translatable(
                    "dnfenhance.command.itemFilter.added", added, listName(whitelist)), true);
            return Command.SINGLE_SUCCESS;
        }

        List<String> removed = new ArrayList<>();
        if (pattern == null) {
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                source.sendFailure(Component.translatable("dnfenhance.command.itemFilter.needItem"));
                return 0;
            }
            for (String existing : entries) {
                if (EnhanceLogic.itemEntryMatches(existing, stack)) removed.add(existing);
            }
            if (removed.isEmpty()) {
                source.sendFailure(Component.translatable("dnfenhance.command.itemFilter.noMatch"));
                return 0;
            }
            entries.removeAll(removed);
        } else {
            String entry = pattern.trim().toLowerCase(Locale.ROOT);
            if (!entries.remove(entry)) {
                source.sendFailure(Component.translatable(
                        "dnfenhance.command.itemFilter.notFound", entry, listName(whitelist)));
                return 0;
            }
            removed.add(entry);
        }
        saveItemFilter(whitelist, entries);
        for (String removedEntry : removed) {
            source.sendSuccess(() -> Component.translatable(
                    "dnfenhance.command.itemFilter.removed", removedEntry, listName(whitelist)), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int itemFilterList(CommandSourceStack source, boolean whitelist) {
        List<String> entries = itemFilterEntries(whitelist);
        Component mode = Component.translatable("dnfenhance.command.itemFilter.mode."
                + EnhanceLogic.itemFilterMode().name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GOLD);
        source.sendSuccess(() -> mode, false);
        Component name = listName(whitelist);
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("dnfenhance.command.itemFilter.empty", name)
                    .withStyle(ChatFormatting.GRAY), false);
            return Command.SINGLE_SUCCESS;
        }
        source.sendSuccess(() -> Component.translatable("dnfenhance.command.itemFilter.header", name)
                .withStyle(ChatFormatting.GOLD), false);
        for (String entry : entries) {
            source.sendSuccess(() -> Component.literal(entry).withStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent.CopyToClipboard(entry))
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.translatable("dnfenhance.command.mobBlacklist.copyHint", entry)))), false);
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
