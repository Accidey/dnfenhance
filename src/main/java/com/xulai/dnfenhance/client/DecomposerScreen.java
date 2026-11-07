package com.xulai.dnfenhance.client;

import com.xulai.dnfenhance.enhance.DecomposeConfig;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import com.xulai.dnfenhance.menu.DecomposerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DecomposerScreen extends AbstractContainerScreen<DecomposerMenu> {
    private static final int EQUIP_X = 48;
    private static final int EQUIP_Y = 20;
    private static final int BAR_X = 70;
    private static final int BAR_Y = 26;
    private static final int LAMP_X = 160;
    private static final int LAMP_Y = 22;
    private static final int INFO_Y = 42;
    private static final int BUFFER_LABEL_Y = 52;
    private static final int BUFFER_Y = 62;

    public DecomposerScreen(DecomposerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.titleLabelY = 6;
        this.inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        drawPanel(graphics, x, y, this.imageWidth, this.imageHeight);

        drawSlot(graphics, x + EQUIP_X, y + EQUIP_Y);
        for (int i = 0; i < DecomposerMenu.MACHINE_SLOTS - 1; i++) {
            drawSlot(graphics, x + 8 + i * 18, y + BUFFER_Y);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 8 + col * 18, y + 106 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 8 + col * 18, y + 178);
        }

        int max = Math.max(1, this.menu.getWorkTicks());
        int width = (int) ((this.menu.getProgress() / (float) max) * 34.0f);
        graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + 36, y + BAR_Y + 8, 0xFF555555);
        graphics.fill(x + BAR_X + 1, y + BAR_Y + 1, x + BAR_X + 35, y + BAR_Y + 7, 0xFF373737);
        if (width > 0) {
            graphics.fill(x + BAR_X + 1, y + BAR_Y + 1, x + BAR_X + 1 + width, y + BAR_Y + 7, 0xFFFFFFFF);
        }

        boolean powered = this.menu.isPowered();
        graphics.fill(x + LAMP_X - 1, y + LAMP_Y - 1, x + LAMP_X + 9, y + LAMP_Y + 9, 0xFF373737);
        graphics.fill(x + LAMP_X, y + LAMP_Y, x + LAMP_X + 8, y + LAMP_Y + 8,
                powered ? 0xFF3BDE3B : 0xFF8B3B3B);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w, y + h, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFC6C6C6);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        graphics.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFFFFF);
        graphics.fill(x + 16, y - 1, x + 17, y + 17, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        ItemStack equip = this.menu.getEquipStack();
        if (equip.isEmpty()) {
            graphics.drawString(this.font,
                    Component.translatable("dnfenhance.gui.decompose_hint"), 8, INFO_Y, 0x6B6B6B, false);
        } else if (DecomposeConfig.outputsFor(EnhanceLogic.getLevel(equip)).isEmpty()) {
            graphics.drawString(this.font,
                    Component.translatable("dnfenhance.gui.decompose_no_output",
                            EnhanceLogic.getLevel(equip)), 8, INFO_Y, 0xB03030, false);
        } else {
            graphics.drawString(this.font,
                    Component.translatable("dnfenhance.gui.decompose_level",
                            EnhanceLogic.getLevel(equip)), 8, INFO_Y, 0x404040, false);
        }

        int progress = this.menu.getProgress();
        if (progress > 0) {
            int percent = progress * 100 / Math.max(1, this.menu.getWorkTicks());
            graphics.drawString(this.font,
                    Component.translatable("dnfenhance.gui.decompose_working", percent),
                    BAR_X + 40, BAR_Y, 0x228B22, false);
        }

        boolean powered = this.menu.isPowered();
        Component rsLine = Component.translatable(powered
                ? "dnfenhance.gui.decompose_redstone_on"
                : "dnfenhance.gui.decompose_redstone_off");
        graphics.drawString(this.font, rsLine,
                this.imageWidth - 8 - this.font.width(rsLine), INFO_Y,
                powered ? 0x1B7F2B : 0xB03030, false);

        graphics.drawString(this.font,
                Component.translatable("dnfenhance.gui.decompose_materials"), 8, BUFFER_LABEL_Y, 0x404040, false);

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        Slot hovered = this.hoveredSlot;
        if (hovered != null && hovered.hasItem() && hovered.getSlotIndex() == 0) {
            ItemStack equip = hovered.getItem();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("dnfenhance.gui.decompose_products_title")
                    .withStyle(ChatFormatting.GOLD));
            List<ItemStack> outputs = DecomposeConfig.outputsFor(EnhanceLogic.getLevel(equip));
            if (outputs.isEmpty()) {
                lines.add(Component.translatable("dnfenhance.gui.decompose_no_products")
                        .withStyle(ChatFormatting.RED));
            } else {
                for (ItemStack out : outputs) {
                    lines.add(Component.literal(out.getHoverName().getString() + " x" + out.getCount())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            graphics.renderComponentTooltip(this.font, lines, x, y);
            return;
        }
        super.renderTooltip(graphics, x, y);
    }
}
