package com.xulai.dnfenhance.client;

import com.xulai.dnfenhance.DnfEnhanceMod;
import com.xulai.dnfenhance.enhance.EnhanceLogic;
import com.xulai.dnfenhance.menu.EnhancementMenu;
import com.xulai.dnfenhance.net.AutoEnhancePayload;
import com.xulai.dnfenhance.net.EnhanceRequestPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnhancementScreen extends AbstractContainerScreen<EnhancementMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(DnfEnhanceMod.MODID, "textures/gui/enhancement_furnace.png");

    private static final int INFO_X = 10;
    private static final int PANEL_TEXT_TOP = 79;
    private static final int PANEL_TEXT_RIGHT = 167;

    private static final int PANEL_TEXT_BOTTOM = 134;
    private static final int FONT_LINE_H = 9;
    private static final int PANEL_TEXT_HEIGHT = PANEL_TEXT_BOTTOM - PANEL_TEXT_TOP + 1;

    private static final float COMPACT_SCALE = 0.75f;

    private static final int COLOR_TITLE = 0xFF404040;
    private static final int COLOR_HINT = 0xFF8B8B8B;
    private static final int COLOR_NORMAL = 0xFF404040;
    private static final int COLOR_GOLD = 0xFFB8860B;
    private static final int COLOR_GOOD = 0xFF228B22;
    private static final int COLOR_BAD = 0xFFB03030;
    private static final int COLOR_COPPER = 0xFFB87333;
    private static final int COLOR_MUTED = 0xFF707070;
    private static final int COLOR_TEAL = 0xFF1B7F8F;
    private static final int COLOR_KAI_LI = 0xFF8B2FC9;
    private static final int COLOR_KAI_VALUE = 0xFFC0392B;

    private Button enhanceButton;
    private Button autoDecreaseButton;
    private Button autoIncreaseButton;
    private Button autoToggleButton;
    private EditBox autoTargetBox;

    public EnhancementScreen(EnhancementMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 228);
        this.titleLabelY = 6;
        this.inventoryLabelY = 142;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        this.enhanceButton = Button.builder(
                        Component.translatable("dnfenhance.gui.enhance"),
                        button -> ClientPacketDistributor.sendToServer(EnhanceRequestPayload.INSTANCE))
                .bounds(x + 61, y + 46, 54, 15)
                .build();
        this.addRenderableWidget(this.enhanceButton);

        this.autoDecreaseButton = Button.builder(Component.literal("<"), b -> stepTarget(-1))
                .bounds(x + 23, y + 62, 14, 15)
                .tooltip(Tooltip.create(Component.translatable("dnfenhance.gui.auto_decrease")))
                .build();
        this.addRenderableWidget(this.autoDecreaseButton);

        this.autoTargetBox = new EditBox(this.font, x + 41, y + 62, 34, 15,
                Component.translatable("dnfenhance.gui.auto_target"));
        int persisted = this.menu.getAutoTarget();
        this.autoTargetBox.setValue(String.valueOf(persisted > 0 ? persisted : defaultAutoTarget()));
        this.autoTargetBox.setMaxLength(2);
        this.autoTargetBox.setHint(Component.literal("10"));
        this.autoTargetBox.setResponder(s -> clampTargetBox());
        this.addRenderableWidget(this.autoTargetBox);

        this.autoIncreaseButton = Button.builder(Component.literal(">"), b -> stepTarget(1))
                .bounds(x + 79, y + 62, 14, 15)
                .tooltip(Tooltip.create(Component.translatable("dnfenhance.gui.auto_increase")))
                .build();
        this.addRenderableWidget(this.autoIncreaseButton);

        this.autoToggleButton = Button.builder(autoButtonLabel(), this::toggleAutoEnhance)
                .bounds(x + 97, y + 62, 56, 15)
                .build();
        this.addRenderableWidget(this.autoToggleButton);
    }

    private int defaultAutoTarget() {
        int current = EnhanceLogic.getLevel(this.menu.getEquipStack());
        return Mth.clamp(current + 1, 1, EnhanceLogic.maxLevel());
    }

    private Component autoButtonLabel() {
        return this.menu.getAutoTarget() > 0
                ? Component.translatable("dnfenhance.gui.auto_stop")
                : Component.translatable("dnfenhance.gui.auto_start");
    }

    private void stepTarget(int delta) {
        int value = parseTarget() + delta;
        value = Mth.clamp(value, 1, EnhanceLogic.maxLevel());
        this.autoTargetBox.setValue(String.valueOf(value));
    }

    private int parseTarget() {
        try {
            return Integer.parseInt(this.autoTargetBox.getValue().trim());
        } catch (NumberFormatException e) {
            return defaultAutoTarget();
        }
    }

    private void clampTargetBox() {
        String value = this.autoTargetBox.getValue().trim();
        if (value.isEmpty()) return;
        try {
            int v = Integer.parseInt(value);
            int clamped = Mth.clamp(v, 1, EnhanceLogic.maxLevel());
            if (clamped != v) {
                this.autoTargetBox.setValue(String.valueOf(clamped));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void toggleAutoEnhance(Button button) {
        int target = this.menu.getAutoTarget() > 0 ? 0 : parseTarget();
        this.autoTargetBox.setValue(String.valueOf(target > 0 ? target : defaultAutoTarget()));
        ClientPacketDistributor.sendToServer(new AutoEnhancePayload(target));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.enhanceButton.active = this.canAttempt();
        this.autoToggleButton.setMessage(autoButtonLabel());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.autoToggleButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, autoToggleTooltip(), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    private List<Component> autoToggleTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("dnfenhance.gui.redstone_required").withStyle(ChatFormatting.GRAY));
        lines.add(hasRedstonePower()
                ? Component.translatable("dnfenhance.gui.redstone_on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("dnfenhance.gui.redstone_off").withStyle(ChatFormatting.RED));
        return lines;
    }

    private boolean hasRedstonePower() {
        var furnace = this.menu.getFurnace();
        return furnace != null && furnace.getLevel() != null
                && furnace.getLevel().hasNeighborSignal(furnace.getBlockPos());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.autoTargetBox.isFocused()) {
            if (event.isEscape()) {
                this.autoTargetBox.setFocused(false);
                return true;
            }
            return this.autoTargetBox.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    private boolean canAttempt() {
        ItemStack equip = this.menu.getEquipStack();
        if (!EnhanceLogic.canEnhance(equip)) return false;
        int target = EnhanceLogic.getLevel(equip) + 1;
        if (target > EnhanceLogic.maxLevel()) return false;
        return this.menu.getCarbonStack().getCount() >= EnhanceLogic.carbonCost(target);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, COLOR_TITLE, false);

        ItemStack equip = this.menu.getEquipStack();
        if (!EnhanceLogic.canEnhance(equip)) {
            drawPanelLines(graphics, List.of(
                    PanelLine.single(Component.translatable("dnfenhance.gui.hint_equip"), COLOR_HINT),
                    PanelLine.single(Component.translatable("dnfenhance.gui.hint_equip2"), COLOR_HINT)));
            return;
        }

        int current = EnhanceLogic.getLevel(equip);
        int target = current + 1;
        if (target > EnhanceLogic.maxLevel()) {
            drawPanelLines(graphics, List.of(
                    PanelLine.single(Component.translatable("dnfenhance.gui.level", current, current), COLOR_NORMAL),
                    PanelLine.single(Component.translatable("dnfenhance.gui.max_level"), COLOR_GOLD)));
            return;
        }

        boolean hasProtection = EnhanceLogic.isProtectionCharm(this.menu.getProtectStack());
        boolean advanced = EnhanceLogic.isAdvancedCarbon(this.menu.getCarbonStack());
        double rate = EnhanceLogic.successRate(target)
                + (advanced ? com.xulai.dnfenhance.enhance.EnhanceConfig.ADVANCED_BONUS.get() : 0.0);
        if (this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.hasEffect(MobEffects.LUCK)) {
            rate += com.xulai.dnfenhance.enhance.EnhanceConfig.LUCK_BONUS.get();
        }
        rate = Mth.clamp(rate, 0.0, 1.0);
        var nearby = com.xulai.dnfenhance.enhance.KaiLiPigHelper
                .scanAura(this.menu.getFurnace().getLevel(), this.menu.getFurnacePos());
        var aura = EnhanceLogic.aura(nearby.variant(), nearby.count());
        rate = aura.mustFail() ? 0.0 : EnhanceLogic.applyAura(rate, aura.count());
        int cost = EnhanceLogic.carbonCost(target);
        int owned = this.menu.getCarbonStack().getCount();

        Component levelText = Component.translatable("dnfenhance.gui.level", current, target);
        int rateColor = rate >= 0.7 ? COLOR_GOOD : rate >= 0.4 ? COLOR_GOLD : COLOR_BAD;
        List<PanelLine> lines = new ArrayList<>();

        lines.add(new PanelLine(List.of(
                new PanelSpan(levelText, 0, COLOR_NORMAL),
                new PanelSpan(Component.translatable("dnfenhance.gui.rate", Math.round(rate * 100)),
                        this.font.width(levelText) + 4, rateColor))));

        lines.add(PanelLine.single(
                Component.translatable(advanced ? "dnfenhance.gui.cost_advanced" : "dnfenhance.gui.cost", cost, owned),
                owned >= cost ? COLOR_NORMAL : COLOR_BAD));

        EnhanceLogic.Penalty penalty = EnhanceLogic.penaltyType(target);
        switch (penalty) {
            case DESTROY -> lines.add(PanelLine.single(
                    Component.translatable("dnfenhance.gui.penalty_destroy"), COLOR_BAD));
            case DOWNGRADE -> lines.add(PanelLine.single(
                    Component.translatable("dnfenhance.gui.penalty_down", EnhanceLogic.penaltyDowngrade(target)),
                    COLOR_COPPER));
            default -> lines.add(PanelLine.single(
                    Component.translatable("dnfenhance.gui.penalty_none"), COLOR_MUTED));
        }

        if (penalty == EnhanceLogic.Penalty.DESTROY) {
            lines.add(PanelLine.single(
                    Component.translatable(hasProtection
                            ? "dnfenhance.gui.protect_ready"
                            : "dnfenhance.gui.protect_warn"),
                    hasProtection ? COLOR_TEAL : COLOR_BAD));
        }

        if (aura.present()) {
            if (aura.variant() == com.xulai.dnfenhance.enhance.KaiLiPigHelper.KaiLiVariant.MASTER) {
                lines.add(PanelLine.single(
                        Component.translatable("dnfenhance.gui.kai_li_master"), COLOR_GOLD));
            } else if (aura.variant() == com.xulai.dnfenhance.enhance.KaiLiPigHelper.KaiLiVariant.BLACKENED) {
                lines.add(PanelLine.single(
                        Component.translatable("dnfenhance.gui.kai_li_blackened"), COLOR_BAD));
            } else {
                int dropPercent = (int) Math.round((1.0 - EnhanceLogic.auraMultiplier(aura.count())) * 100);
                Component kaiLead = Component.translatable("dnfenhance.gui.kai_li_label", aura.count());
                Component kaiValue = Component.translatable("dnfenhance.gui.kai_li_warn", dropPercent);
                lines.add(new PanelLine(List.of(
                        new PanelSpan(kaiLead, 0, 0x8B2FC9),
                        new PanelSpan(kaiValue, this.font.width(kaiLead), 0xC0392B))));
            }
        }

        drawPanelLines(graphics, lines);
    }

    private record PanelSpan(Component text, int x, int color) {}

    private record PanelLine(List<PanelSpan> spans) {
        static PanelLine single(Component text, int color) {
            return new PanelLine(List.of(new PanelSpan(text, 0, color)));
        }
    }

    private void drawPanelLines(GuiGraphicsExtractor graphics, List<PanelLine> lines) {
        if (lines.isEmpty()) {
            return;
        }
        int count = lines.size();
        float scale = count > 4 ? compactScale() : 1.0f;
        int lineH = Math.max(1, Math.round(FONT_LINE_H * scale));

        int n = count + 1;
        int slack = PANEL_TEXT_HEIGHT - count * lineH;
        int base = slack / n;
        int extra = slack % n;
        int y = PANEL_TEXT_TOP;
        for (int i = 0; i < count; i++) {
            y += base + (i < extra ? 1 : 0);
            drawPanelLine(graphics, lines.get(i), y, scale);
            y += lineH;
        }
    }

    private void drawPanelLine(GuiGraphicsExtractor graphics, PanelLine line, int py, float scale) {
        int rowRight = INFO_X;
        for (PanelSpan span : line.spans()) {
            rowRight = Math.max(rowRight, INFO_X + span.x() + this.font.width(span.text()));
        }
        float fit = 1.0f;
        int maxWidth = PANEL_TEXT_RIGHT + 1;
        if (rowRight * scale > maxWidth) {
            fit = Math.max(0.65f, maxWidth / (float) rowRight);
        }
        float total = scale * fit;

        if (total >= 0.999f) {
            for (PanelSpan span : line.spans()) {
                graphics.text(this.font, span.text(), INFO_X + span.x(), py, span.color(), false);
            }
            return;
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) INFO_X, (float) py);
        pose.scale(total, total);
        for (PanelSpan span : line.spans()) {
            graphics.text(this.font, span.text(), span.x(), 0, span.color(), false);
        }
        pose.popMatrix();
    }

    private float compactScale() {
        int guiScale = 1;
        if (this.minecraft != null) {
            guiScale = this.minecraft.getWindow().getGuiScale();
        }
        if (guiScale <= 0) {
            return COMPACT_SCALE;
        }
        float best = COMPACT_SCALE;
        float bestDiff = Float.MAX_VALUE;
        for (int unit = 1; unit <= guiScale; unit++) {
            float candidate = unit / (float) guiScale;
            if (candidate < 0.6f || candidate > 0.85f) {
                continue;
            }
            float diff = Math.abs(candidate - COMPACT_SCALE);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = candidate;
            }
        }
        return best;
    }
}
