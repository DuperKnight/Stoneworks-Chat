package com.duperknight.stoneworksChat.client;

import java.util.Map;

import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;

import com.duperknight.stoneworksChat.client.config.ChatConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HudConfigScreen extends Screen {
    private final Screen parent;

    private int currentX;
    private int currentY;
    private StoneworksChatClient.TextAlign currentAlign;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private int previewW = 0;
    private int previewH = 0;

    private static final int HANDLE_SIZE = 8;
    private static final float SCALE_SNAP_INCREMENT = 0.01f;
    private static final float MIN_SCALE = StoneworksChatClient.HUD_MIN_SCALE;
    private static final float MAX_SCALE = StoneworksChatClient.HUD_MAX_SCALE;
    private boolean resizing = false;
    private boolean hoveringTL = false;
    private boolean hoveringTR = false;
    private boolean hoveringBL = false;
    private boolean hoveringBR = false;
    private enum Handle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    private Handle activeHandle = null;
    private float startScale = 1.0f;
    private double resizeOriginMouseDist = 0.0;
    private int resizeAnchorX = 0;
    private int resizeAnchorY = 0;

    private boolean showTutorialPopup;
    private ButtonWidget tutorialOkBtn;
    private ButtonWidget tutorialDontShowBtn;
    private ButtonWidget alignmentBtn;
    private ButtonWidget doneBtn;

    private static final Identifier ARROWS_TEX = Identifier.of(StoneworksChatClient.MOD_ID, "tutorial/arrow_keys.png");
    private static final Identifier SHIFT_TEX = Identifier.of(StoneworksChatClient.MOD_ID, "tutorial/shift_key.png");

    public HudConfigScreen(Screen parent) {
    super(Text.translatable("screen.stoneworks_chat.hud_config.title"));
        this.parent = parent;
    this.currentX = StoneworksChatClient.hudPosX;
    this.currentY = StoneworksChatClient.hudPosY;
        this.currentAlign = StoneworksChatClient.hudTextAlign;
    }

    @Override
    protected void init() {
    showTutorialPopup = StoneworksChatClient.showHudTutorial;
        int centerX = this.width / 2;
        int btnW = 200;
        int btnH = 20;
        int spacing = 6;

        try {
            Map<String, Object> channelInfo = StoneworksChatClient.channels.get(StoneworksChatClient.currentChannel);
            String display = channelInfo != null ? (String) channelInfo.get("display") : "";
            Text previewText = Text.translatable("stoneworks_chat.chat_prefix", (display != null ? display : ""));
            int paddingX = 4;
            int paddingY = 3;
            int textW = this.textRenderer.getWidth(previewText);
            int textH = this.textRenderer.fontHeight;
            int bgW = textW + paddingX * 2;
            int bgH = textH + paddingY * 2;
            this.previewW = bgW;
            this.previewH = bgH;

            if (StoneworksChatClient.hudAnchorX != null && StoneworksChatClient.hudAnchorY != null &&
                StoneworksChatClient.hudOffsetX >= 0 && StoneworksChatClient.hudOffsetY >= 0) {
                int anchorCoordX;
                switch (StoneworksChatClient.hudAnchorX) {
                    case RIGHT -> anchorCoordX = this.width - StoneworksChatClient.hudOffsetX;
                    case CENTER -> anchorCoordX = (this.width / 2) + StoneworksChatClient.hudOffsetX;
                    default -> anchorCoordX = StoneworksChatClient.hudOffsetX;
                }

                int topY;
                switch (StoneworksChatClient.hudAnchorY) {
                    case TOP -> topY = StoneworksChatClient.hudOffsetY;
                    case CENTER -> topY = (this.height / 2) + StoneworksChatClient.hudOffsetY - bgH / 2;
                    case BOTTOM -> topY = this.height - StoneworksChatClient.hudOffsetY - bgH;
                    default -> topY = StoneworksChatClient.hudPosY;
                }

                this.currentX = anchorCoordX;
                this.currentY = topY;
            }
        } catch (Exception ignored) {
        }

        alignmentBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.stoneworks_chat.hud_config.alignment", alignLabelText()), b -> {
            toggleAlign();
            b.setMessage(Text.translatable("screen.stoneworks_chat.hud_config.alignment", alignLabelText()));
        }).dimensions(centerX - btnW / 2, this.height - (btnH * 2 + spacing + 12), btnW, btnH).build());

    doneBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> {
        // Preserve alignment behavior but store raw alignment coordinate & top-left reference
        StoneworksChatClient.hudTextAlign = currentAlign;

        // Store currentX/currentY directly; disable anchor offsets to avoid scale drift
    StoneworksChatClient.hudPosX = currentX; // alignment line (left / center / right edge depending on alignment)
    StoneworksChatClient.hudPosY = currentY; // top edge
    StoneworksChatClient.hudPosXFrac = -1f;
    StoneworksChatClient.hudPosYFrac = -1f;
    StoneworksChatClient.hudOffsetX = -1; // signal: use raw pos fields
    StoneworksChatClient.hudOffsetY = -1;
    // Null anchors so re-opening config uses raw stored coords
    StoneworksChatClient.hudAnchorX = null;
    StoneworksChatClient.hudAnchorY = null;
        ChatConfig.save();
        close();
    }).dimensions(centerX - btnW / 2, this.height - (btnH + 12), btnW, btnH).build());

        tutorialOkBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.stoneworks_chat.hud_config.tutorial.ok"), b -> {
            this.showTutorialPopup = false;
            hideTutorialButtons();
        }).dimensions(centerX - 105,  this.height / 2 + 60, 100, 20).build());

        tutorialDontShowBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.stoneworks_chat.hud_config.tutorial.dont_show"), b -> {
            StoneworksChatClient.showHudTutorial = false;
            ChatConfig.save();
            this.showTutorialPopup = false;
            hideTutorialButtons();
        }).dimensions(centerX + 5, this.height / 2 + 60, 100, 20).build());

        updateTutorialButtons();
    }

    private void toggleAlign() {
        int boxW = Math.round(previewW * StoneworksChatClient.hudScale);
        boolean wasRtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
        boolean wasCenter = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
        int leftXBefore = wasRtl ? (currentX - boxW) : (wasCenter ? (currentX - boxW / 2) : currentX);

        switch (currentAlign) {
            case LEFT_TO_RIGHT -> currentAlign = StoneworksChatClient.TextAlign.CENTER;
            case CENTER -> currentAlign = StoneworksChatClient.TextAlign.RIGHT_TO_LEFT;
            case RIGHT_TO_LEFT -> currentAlign = StoneworksChatClient.TextAlign.LEFT_TO_RIGHT;
        }

        switch (currentAlign) {
            case LEFT_TO_RIGHT -> currentX = leftXBefore;
            case CENTER -> currentX = leftXBefore + (boxW / 2);
            case RIGHT_TO_LEFT -> currentX = leftXBefore + boxW;
        }
    }

    private Text alignLabelText() {
        return switch (currentAlign) {
            case LEFT_TO_RIGHT -> Text.translatable("screen.stoneworks_chat.hud_config.align.left_to_right");
            case CENTER -> Text.translatable("screen.stoneworks_chat.hud_config.align.center");
            case RIGHT_TO_LEFT -> Text.translatable("screen.stoneworks_chat.hud_config.align.right_to_left");
        };
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        renderBackground(drawContext, mouseX, mouseY, delta);
        if (showTutorialPopup) {
            if (alignmentBtn != null) alignmentBtn.visible = false;
            if (doneBtn != null) doneBtn.visible = false;

            boolean okPrevVis = tutorialOkBtn != null && tutorialOkBtn.visible;
            boolean dontPrevVis = tutorialDontShowBtn != null && tutorialDontShowBtn.visible;
            if (tutorialOkBtn != null) tutorialOkBtn.visible = false;
            if (tutorialDontShowBtn != null) tutorialDontShowBtn.visible = false;
            super.render(drawContext, mouseX, mouseY, delta);

            renderTutorialPanel(drawContext);

            if (tutorialOkBtn != null) { tutorialOkBtn.visible = true; tutorialOkBtn.render(drawContext, mouseX, mouseY, delta); }
            if (tutorialDontShowBtn != null) { tutorialDontShowBtn.visible = true; tutorialDontShowBtn.render(drawContext, mouseX, mouseY, delta); }

            if (!okPrevVis && tutorialOkBtn != null) tutorialOkBtn.visible = false;
            if (!dontPrevVis && tutorialDontShowBtn != null) tutorialDontShowBtn.visible = false;
            return;
        }

        if (alignmentBtn != null) alignmentBtn.visible = true;
        if (doneBtn != null) doneBtn.visible = true;
        super.render(drawContext, mouseX, mouseY, delta);

        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(0.0F, 0.0F, 1000.0F);
        drawContext.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);

        Map<String, Object> channelInfo = StoneworksChatClient.channels.get(StoneworksChatClient.currentChannel);
        if (channelInfo != null) {
            String display = (String) channelInfo.get("display");
            String color = (String) channelInfo.get("color");
            Text text = Text.translatable("stoneworks_chat.chat_prefix", (display != null ? display : ""));
            var tr = MinecraftClient.getInstance().textRenderer;
            int paddingX = 4;
            int paddingY = 3;
            int textW = tr.getWidth(text);
            int textH = tr.fontHeight;
            int bgW = textW + paddingX * 2;
            int bgH = textH + paddingY * 2;
            float scale = StoneworksChatClient.hudScale;
            int scaledBgW = Math.round(bgW * scale);
            int scaledBgH = Math.round(bgH * scale);
            previewW = bgW;
            previewH = bgH;
            boolean rtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
            boolean centerAlign = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
            boolean shiftDown = hasShiftDown();
            if (dragging) {
                int leftXBefore = rtl ? (currentX - scaledBgW) : (centerAlign ? (currentX - scaledBgW / 2) : currentX);
                int clampedLeftX = clamp(leftXBefore, 0, this.width - scaledBgW);
                int clampedY = clamp(currentY, 0, this.height - scaledBgH);
                int leftX = clampedLeftX;
                int yPos = clampedY;
                if (!shiftDown) {
                    final int margin = 10; final int tolerance = 3;
                    int screenCenterX = this.width / 2; int screenCenterY = this.height / 2;
                    int centerLeftX = screenCenterX - (scaledBgW / 2); int rightLeftX = this.width - margin - scaledBgW;
                    if (Math.abs(leftX - margin) <= tolerance) leftX = margin;
                    else if (Math.abs((leftX + scaledBgW) - (this.width - margin)) <= tolerance) leftX = rightLeftX;
                    else if (Math.abs((leftX + scaledBgW / 2) - screenCenterX) <= tolerance) leftX = centerLeftX;
                    int bottomY = this.height - margin - scaledBgH; int centerTopY = screenCenterY - (scaledBgH / 2);
                    if (Math.abs(yPos - margin) <= tolerance) yPos = margin;
                    else if (Math.abs((yPos + scaledBgH) - (this.height - margin)) <= tolerance) yPos = bottomY;
                    else if (Math.abs((yPos + scaledBgH / 2) - screenCenterY) <= tolerance) yPos = centerTopY;
                }
                currentY = yPos;
                currentX = rtl ? (leftX + scaledBgW) : (centerAlign ? (leftX + scaledBgW / 2) : leftX);
            }
            if (dragging && !shiftDown) drawGuides(drawContext, scaledBgW, scaledBgH);
            int leftX = rtl ? (currentX - scaledBgW) : (centerAlign ? (currentX - scaledBgW / 2) : currentX);
            int bgColor = StoneworksChatClient.hudVisible ? 0x80000000 : 0x40404040;
            drawContext.fill(leftX, currentY, leftX + scaledBgW, currentY + scaledBgH, bgColor);
            int borderColor = StoneworksChatClient.hudVisible ? 0x80FFFFFF : 0x60AAAAAA;
            drawContext.fill(leftX, currentY, leftX + scaledBgW, currentY + 1, borderColor);
            drawContext.fill(leftX, currentY + scaledBgH - 1, leftX + scaledBgW, currentY + scaledBgH, borderColor);
            drawContext.fill(leftX, currentY, leftX + 1, currentY + scaledBgH, borderColor);
            drawContext.fill(leftX + scaledBgW - 1, currentY, leftX + scaledBgW, currentY + scaledBgH, borderColor);
            int anchorXpx = switch (currentAlign) {
                case LEFT_TO_RIGHT -> leftX;
                case CENTER -> leftX + (scaledBgW / 2);
                case RIGHT_TO_LEFT -> leftX + scaledBgW;
            };
            int markerTop = Math.max(0, currentY - 4);
            drawContext.fill(anchorXpx, markerTop, anchorXpx + 1, currentY, 0xFF80C0FF);

            int textXLogical = rtl ? (leftX + scaledBgW - Math.round(paddingX * scale) - Math.round(textW * scale))
                                   : (centerAlign ? (leftX + Math.max(0, (scaledBgW - Math.round(textW * scale)) / 2))
                                                  : (leftX + Math.round(paddingX * scale)));
            int textY = currentY + Math.round((scaledBgH - Math.round(textH * scale)) / 2f) + 1;
            int colorCode = HudOverlayRenderer.getColorCode(color);
            if (StoneworksChatClient.hudVisible) {
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(textXLogical, textY, 0);
                drawContext.getMatrices().scale(scale, scale, 1.0f);
                drawContext.drawText(tr, text, 0, 0, colorCode, true);
                drawContext.getMatrices().pop();
            }

            int tlX = leftX - HANDLE_SIZE / 2;            int tlY = currentY - HANDLE_SIZE / 2;
            int trX = leftX + scaledBgW - HANDLE_SIZE / 2; int trY = currentY - HANDLE_SIZE / 2;
            int blX = leftX - HANDLE_SIZE / 2;            int blY = currentY + scaledBgH - HANDLE_SIZE / 2;
            int brX = leftX + scaledBgW - HANDLE_SIZE / 2; int brY = currentY + scaledBgH - HANDLE_SIZE / 2;
            if (!resizing) {
                hoveringTL = isPointInRect(mouseX, mouseY, tlX, tlY, HANDLE_SIZE, HANDLE_SIZE);
                hoveringTR = isPointInRect(mouseX, mouseY, trX, trY, HANDLE_SIZE, HANDLE_SIZE);
                hoveringBL = isPointInRect(mouseX, mouseY, blX, blY, HANDLE_SIZE, HANDLE_SIZE);
                hoveringBR = isPointInRect(mouseX, mouseY, brX, brY, HANDLE_SIZE, HANDLE_SIZE);
            }

            if (hoverOrActive(Handle.TOP_LEFT)) drawHandle(drawContext, tlX, tlY, hoverOrActive(Handle.TOP_LEFT));
            else if (hoverOrActive(Handle.TOP_RIGHT)) drawHandle(drawContext, trX, trY, hoverOrActive(Handle.TOP_RIGHT));
            else if (hoverOrActive(Handle.BOTTOM_LEFT)) drawHandle(drawContext, blX, blY, hoverOrActive(Handle.BOTTOM_LEFT));
            else if (hoverOrActive(Handle.BOTTOM_RIGHT)) drawHandle(drawContext, brX, brY, hoverOrActive(Handle.BOTTOM_RIGHT));

            if (resizing && activeHandle != null) {
                String scaleStr = String.format("%.2fx", StoneworksChatClient.hudScale);
                int labelY = switch (activeHandle) {
                    case TOP_LEFT, TOP_RIGHT -> currentY + scaledBgH + 4;
                    case BOTTOM_LEFT, BOTTOM_RIGHT -> currentY - 10 - textRenderer.fontHeight;
                };
                int labelX = leftX + scaledBgW / 2;
                drawContext.drawCenteredTextWithShadow(textRenderer, Text.literal(scaleStr), labelX, labelY, 0xFFFFFF);
            }
        }
        drawContext.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.stoneworks_chat.hud_config.drag_help"), this.width / 2, this.height - 84, 0xAAAAAA);
        Text vis = Text.translatable(StoneworksChatClient.hudVisible ? "screen.stoneworks_chat.hud_config.visible" : "screen.stoneworks_chat.hud_config.hidden");
        drawContext.drawCenteredTextWithShadow(textRenderer, vis, this.width / 2, this.height - 72, 0xCCCCCC);
        drawContext.getMatrices().pop();
    }

    private Integer arrowsWCache, arrowsHCache, shiftWCache, shiftHCache;

    private void ensureTextureSizes() {
        if (arrowsWCache != null) return;
        var mc = MinecraftClient.getInstance();
        var rm = mc.getResourceManager();
        try {
            var res = rm.getResource(ARROWS_TEX);
            res.ifPresent(r -> {
                try (var is = r.getInputStream()) {
                    var img = net.minecraft.client.texture.NativeImage.read(is);
                    arrowsWCache = img.getWidth(); arrowsHCache = img.getHeight();
                } catch (Exception ignored) {}
            });
            var res2 = rm.getResource(SHIFT_TEX);
            res2.ifPresent(r -> {
                try (var is = r.getInputStream()) {
                    var img = net.minecraft.client.texture.NativeImage.read(is);
                    shiftWCache = img.getWidth(); shiftHCache = img.getHeight();
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        if (arrowsWCache == null) { arrowsWCache = 64; arrowsHCache = 32; }
        if (shiftWCache == null) { shiftWCache = 48; shiftHCache = 32; }
    }

    private void renderTutorialPanel(DrawContext ctx) {
        ensureTextureSizes();
        int arrowsW = arrowsWCache; int arrowsH = arrowsHCache; int shiftW = shiftWCache; int shiftH = shiftHCache;
    int lineSpacing = 8;

    String arrowsLine = "- " + Text.translatable("screen.stoneworks_chat.hud_config.tutorial.move").getString();
    String shiftLine  = "- " + Text.translatable("screen.stoneworks_chat.hud_config.tutorial.fast").getString();
    int arrowsTextW = textRenderer.getWidth(arrowsLine);
    int shiftTextW  = textRenderer.getWidth(shiftLine);
    int spaceBetween = 8; // gap between image and text
    int arrowsLineW = arrowsW + spaceBetween + arrowsTextW;
    int shiftLineW  = shiftW + spaceBetween + shiftTextW;
    int contentBlockW = Math.max(arrowsLineW, shiftLineW);
    int popupW = Math.max(340, contentBlockW + 32);
    int textBlockH = Math.max(arrowsH, textRenderer.fontHeight) + lineSpacing + Math.max(shiftH, textRenderer.fontHeight);
    int popupH = 30 + textBlockH + 54;
        int x = (this.width - popupW) / 2;
        int y = (this.height - popupH) / 2;

        ctx.fill(0, 0, this.width, this.height, 0xA0000000);

        ctx.fill(x, y, x + popupW, y + popupH, 0xC0151515);
        ctx.fill(x, y, x + popupW, y + 1, 0xFFFFFFFF);
        ctx.fill(x, y + popupH - 1, x + popupW, y + popupH, 0xFFFFFFFF);
        ctx.fill(x, y, x + 1, y + popupH, 0xFFFFFFFF);
        ctx.fill(x + popupW - 1, y, x + popupW, y + popupH, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.stoneworks_chat.hud_config.tutorial.title"), x + popupW / 2, y + 6, 0xFFFFFF);
    int contentY = y + 22;
    int blockStartX = x + (popupW - contentBlockW) / 2;

    int arrowsImgX = blockStartX;
    ctx.drawTexture(RenderLayer::getGuiTextured, ARROWS_TEX, arrowsImgX, contentY, 0F, 0F, arrowsW, arrowsH, arrowsW, arrowsH);
    int arrowsTextX = arrowsImgX + arrowsW + spaceBetween;
    int arrowsTextY = contentY + (arrowsH - textRenderer.fontHeight) / 2;
    ctx.drawTextWithShadow(textRenderer, Text.literal(arrowsLine), arrowsTextX, arrowsTextY, 0xDDDDDD);

    int secondY = contentY + Math.max(arrowsH, textRenderer.fontHeight) + lineSpacing;
    int shiftImgX = blockStartX;
    ctx.drawTexture(RenderLayer::getGuiTextured, SHIFT_TEX, shiftImgX, secondY, 0F, 0F, shiftW, shiftH, shiftW, shiftH);
    int shiftTextX = shiftImgX + shiftW + spaceBetween;
    int shiftTextYPos = secondY + (shiftH - textRenderer.fontHeight) / 2;
    ctx.drawTextWithShadow(textRenderer, Text.literal(shiftLine), shiftTextX, shiftTextYPos, 0xDDDDDD);
        if (tutorialOkBtn != null) tutorialOkBtn.setPosition(x + popupW / 2 - 105, y + popupH - 26);
        if (tutorialDontShowBtn != null) tutorialDontShowBtn.setPosition(x + popupW / 2 + 5, y + popupH - 26);
        updateTutorialButtons();
    }


    private void updateTutorialButtons() {
        if (tutorialOkBtn != null) tutorialOkBtn.visible = showTutorialPopup;
        if (tutorialDontShowBtn != null) tutorialDontShowBtn.visible = showTutorialPopup;
    }

    private void hideTutorialButtons() {
        if (tutorialOkBtn != null) tutorialOkBtn.visible = false;
        if (tutorialDontShowBtn != null) tutorialDontShowBtn.visible = false;
    }

    private void drawGuides(DrawContext ctx, int boxW, int boxH) {
    final int tolerance = 3;
    final int margin = 10;

        int screenW = this.width;
        int screenH = this.height;

    boolean rtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
    boolean centerAlign = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
    int leftX = rtl ? (currentX - boxW) : (centerAlign ? (currentX - boxW / 2) : currentX);

        int boxCenterX = leftX + boxW / 2;
        int boxCenterY = currentY + boxH / 2;

        int screenCenterX = screenW / 2;
        int screenCenterY = screenH / 2;

    int centerColor = 0x80FF4080;
    int edgeColor   = 0x80FFAA00;

        if (Math.abs(boxCenterX - screenCenterX) <= tolerance) {
            ctx.fill(screenCenterX, 0, screenCenterX + 1, screenH, centerColor);
        }
        if (Math.abs(boxCenterY - screenCenterY) <= tolerance) {
            ctx.fill(0, screenCenterY, screenW, screenCenterY + 1, centerColor);
        }

    if (Math.abs(leftX - margin) <= tolerance) {
            ctx.fill(margin, 0, margin + 1, screenH, edgeColor);
        }
        int rightX = screenW - margin;
    if (Math.abs((leftX + boxW) - rightX) <= tolerance) {
            ctx.fill(rightX, 0, rightX + 1, screenH, edgeColor);
        }
        if (Math.abs(currentY - margin) <= tolerance) {
            ctx.fill(0, margin, screenW, margin + 1, edgeColor);
        }
        int bottomY = screenH - margin;
        if (Math.abs((currentY + boxH) - bottomY) <= tolerance) {
            ctx.fill(0, bottomY, screenW, bottomY + 1, edgeColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showTutorialPopup) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (resizing && button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (button == 1) {
            if (hitPreview(mouseX, mouseY)) {
                StoneworksChatClient.hudVisible = !StoneworksChatClient.hudVisible;
                ChatConfig.save();
                return true;
            }
        }
        if (button == 0) {
            if (handleDoubleClick(mouseX, mouseY)) {
                StoneworksChatClient.hudScale = Math.round(1.0f / SCALE_SNAP_INCREMENT) * SCALE_SNAP_INCREMENT;
                ChatConfig.save();
                return true;
            }
            if (hoveringTL || hoveringTR || hoveringBL || hoveringBR) {
                resizing = true;
                startScale = StoneworksChatClient.hudScale;
                activeHandle = hoveringTL ? Handle.TOP_LEFT : hoveringTR ? Handle.TOP_RIGHT : hoveringBL ? Handle.BOTTOM_LEFT : Handle.BOTTOM_RIGHT;
                boolean rtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
                boolean centerAlign = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
                float scale = StoneworksChatClient.hudScale;
                int scaledBgW = Math.round(previewW * scale);
                int scaledBgH = Math.round(previewH * scale);
                int leftX = rtl ? (currentX - scaledBgW) : (centerAlign ? (currentX - scaledBgW / 2) : currentX);
                int topY = currentY;
                switch (activeHandle) {
                    case TOP_LEFT -> { resizeAnchorX = leftX + scaledBgW; resizeAnchorY = topY + scaledBgH; }
                    case TOP_RIGHT -> { resizeAnchorX = leftX; resizeAnchorY = topY + scaledBgH; }
                    case BOTTOM_LEFT -> { resizeAnchorX = leftX + scaledBgW; resizeAnchorY = topY; }
                    case BOTTOM_RIGHT -> { resizeAnchorX = leftX; resizeAnchorY = topY; }
                }
                double dx = mouseX - resizeAnchorX;
                double dy = mouseY - resizeAnchorY;
                resizeOriginMouseDist = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
                return true;
            }
            if (hitPreview(mouseX, mouseY)) {
                dragging = true;
                int leftX = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT)
                    ? (currentX - Math.round(previewW * StoneworksChatClient.hudScale))
                    : (currentAlign == StoneworksChatClient.TextAlign.CENTER ? (currentX - Math.round(previewW * StoneworksChatClient.hudScale) / 2) : currentX);
                dragOffsetX = (int)mouseX - leftX;
                dragOffsetY = (int)mouseY - currentY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    if (showTutorialPopup) return false;
        if (resizing && button == 0) {
            double curDx = mouseX - resizeAnchorX;
            double curDy = mouseY - resizeAnchorY;
            double curDist = Math.sqrt(curDx * curDx + curDy * curDy);
            double ratio = curDist / resizeOriginMouseDist;
            float newScale = (float)(startScale * ratio);
            newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
            newScale = Math.round(newScale / SCALE_SNAP_INCREMENT) * SCALE_SNAP_INCREMENT;

            if (newScale != StoneworksChatClient.hudScale) {
                StoneworksChatClient.hudScale = newScale;
                boolean rtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
                boolean centerAlign = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
                int newW = Math.round(previewW * newScale);
                int newH = Math.round(previewH * newScale);
                int anchorLeftX;
                int anchorTopY;

                switch (activeHandle) {
                    case TOP_LEFT -> { // anchor is bottom-right
                        anchorLeftX = resizeAnchorX - newW;
                        anchorTopY = resizeAnchorY - newH;
                    }
                    case TOP_RIGHT -> { // anchor bottom-left
                        anchorLeftX = resizeAnchorX;
                        anchorTopY = resizeAnchorY - newH;
                    }
                    case BOTTOM_LEFT -> { // anchor top-right
                        anchorLeftX = resizeAnchorX - newW;
                        anchorTopY = resizeAnchorY;
                    }
                    case BOTTOM_RIGHT -> { // anchor top-left
                        anchorLeftX = resizeAnchorX;
                        anchorTopY = resizeAnchorY;
                    }
                    default -> { anchorLeftX = 0; anchorTopY = 0; }
                }

                if (rtl) {
                    currentX = anchorLeftX + newW; // right edge
                } else if (centerAlign) {
                    currentX = anchorLeftX + newW / 2;
                } else { // LTR
                    currentX = anchorLeftX;
                }
                currentY = anchorTopY;
                if (rtl) {
                    currentX = clamp(currentX, newW, this.width);
                } else if (centerAlign) {
                    currentX = clamp(currentX, newW / 2, this.width - newW / 2);
                } else {
                    currentX = clamp(currentX, 0, this.width - newW);
                }
                currentY = clamp(currentY, 0, this.height - newH);
            }
            return true;
        }
        if (dragging && button == 0) {
            int leftX = (int)mouseX - dragOffsetX;
            currentY = (int)mouseY - dragOffsetY;

            if (null == currentAlign) {
                currentX = leftX;
                currentX = clamp(currentX, 0, this.width - previewW);
            } else switch (currentAlign) {
                case RIGHT_TO_LEFT -> {
                    int scaledW = Math.round(previewW * StoneworksChatClient.hudScale);
                    currentX = leftX + scaledW;
                    currentX = clamp(currentX, scaledW, this.width);
                }
                case CENTER -> {
                    int scaledW = Math.round(previewW * StoneworksChatClient.hudScale);
                    currentX = leftX + scaledW / 2;
                    currentX = clamp(currentX, scaledW / 2, this.width - scaledW / 2);
                }
                default -> {
                    int scaledW = Math.round(previewW * StoneworksChatClient.hudScale);
                    currentX = leftX;
                    currentX = clamp(currentX, 0, this.width - scaledW);
                }
            }
            int scaledH = Math.round(previewH * StoneworksChatClient.hudScale);
            currentY = clamp(currentY, 0, this.height - scaledH);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        if (resizing && button == 0) {
            resizing = false;
            activeHandle = null;
            ChatConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        var c = this.client;
        if (c != null) {
            c.setScreen(parent);
        }
    }

    private boolean hitPreview(double mouseX, double mouseY) {
        boolean rtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
        boolean centerAlign = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
    float scale = StoneworksChatClient.hudScale;
    int scaledW = Math.round(previewW * scale);
    int scaledH = Math.round(previewH * scale);
    int leftX = rtl ? (currentX - scaledW) : (centerAlign ? (currentX - scaledW / 2) : currentX);
    return mouseX >= leftX && mouseX <= leftX + scaledW &&
           mouseY >= currentY && mouseY <= currentY + scaledH;
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private boolean isPointInRect(double px, double py, int x, int y, int w, int h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    private long lastHandleClickTime = 0L;
    private static final long DOUBLE_CLICK_MS = 300; // threshold
    private boolean handleDoubleClick(double mouseX, double mouseY) {
        float scale = StoneworksChatClient.hudScale;
        int scaledBgW = Math.round(previewW * scale);
        int scaledBgH = Math.round(previewH * scale);
        boolean rtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
        boolean centerAlign = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
        int leftX = rtl ? (currentX - scaledBgW) : (centerAlign ? (currentX - scaledBgW / 2) : currentX);
        int topY = currentY;
        int tlX = leftX - HANDLE_SIZE / 2;            int tlY = topY - HANDLE_SIZE / 2;
        int trX = leftX + scaledBgW - HANDLE_SIZE / 2; int trY = topY - HANDLE_SIZE / 2;
        int blX = leftX - HANDLE_SIZE / 2;            int blY = topY + scaledBgH - HANDLE_SIZE / 2;
        int brX = leftX + scaledBgW - HANDLE_SIZE / 2; int brY = topY + scaledBgH - HANDLE_SIZE / 2;
        boolean overAny = isPointInRect(mouseX, mouseY, tlX, tlY, HANDLE_SIZE, HANDLE_SIZE) ||
                          isPointInRect(mouseX, mouseY, trX, trY, HANDLE_SIZE, HANDLE_SIZE) ||
                          isPointInRect(mouseX, mouseY, blX, blY, HANDLE_SIZE, HANDLE_SIZE) ||
                          isPointInRect(mouseX, mouseY, brX, brY, HANDLE_SIZE, HANDLE_SIZE);
        if (!overAny) return false;
        long now = System.currentTimeMillis();
        if (now - lastHandleClickTime <= DOUBLE_CLICK_MS) {
            lastHandleClickTime = 0L; // reset
            return true;
        }
        lastHandleClickTime = now;
        return false;
    }

    private void drawHandle(DrawContext ctx, int x, int y, boolean highlight) {
        int fill = highlight ? 0xFF40FF40 : 0xC020FF20;
        ctx.fill(x, y, x + HANDLE_SIZE, y + HANDLE_SIZE, fill);
        ctx.fill(x, y, x + HANDLE_SIZE, y + 1, 0xFFFFFFFF);
        ctx.fill(x, y + HANDLE_SIZE - 1, x + HANDLE_SIZE, y + HANDLE_SIZE, 0xFFFFFFFF);
        ctx.fill(x, y, x + 1, y + HANDLE_SIZE, 0xFFFFFFFF);
        ctx.fill(x + HANDLE_SIZE - 1, y, x + HANDLE_SIZE, y + HANDLE_SIZE, 0xFFFFFFFF);
    }

    private boolean hoverOrActive(Handle h) {
        return switch (h) {
            case TOP_LEFT -> hoveringTL || activeHandle == Handle.TOP_LEFT;
            case TOP_RIGHT -> hoveringTR || activeHandle == Handle.TOP_RIGHT;
            case BOTTOM_LEFT -> hoveringBL || activeHandle == Handle.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> hoveringBR || activeHandle == Handle.BOTTOM_RIGHT;
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showTutorialPopup) {
            if (keyCode == net.minecraft.client.util.InputUtil.GLFW_KEY_ESCAPE) {
                showTutorialPopup = false;
                hideTutorialButtons();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        int step = hasShiftDown() ? 5 : 1;
        boolean handled = false;

    if (keyCode == net.minecraft.client.util.InputUtil.GLFW_KEY_LEFT) {
            currentX -= step;
            handled = true;
    } else if (keyCode == net.minecraft.client.util.InputUtil.GLFW_KEY_RIGHT) {
            currentX += step;
            handled = true;
        }
    if (keyCode == net.minecraft.client.util.InputUtil.GLFW_KEY_UP) {
            currentY -= step;
            handled = true;
    } else if (keyCode == net.minecraft.client.util.InputUtil.GLFW_KEY_DOWN) {
            currentY += step;
            handled = true;
        }

        if (handled) {
            int scaledW = Math.round(previewW * StoneworksChatClient.hudScale);
            int scaledH = Math.round(previewH * StoneworksChatClient.hudScale);
            if (null == currentAlign) {
                currentX = clamp(currentX, 0, this.width - scaledW);
            } else currentX = switch (currentAlign) {
                case RIGHT_TO_LEFT -> clamp(currentX, scaledW, this.width);
                case CENTER -> clamp(currentX, scaledW / 2, this.width - scaledW / 2);
                default -> clamp(currentX, 0, this.width - scaledW);
            };
            currentY = clamp(currentY, 0, this.height - scaledH);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
