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

            if (StoneworksChatClient.hudAnchorX != null && StoneworksChatClient.hudAnchorY != null) {
                int anchorCoordX;
                switch (StoneworksChatClient.hudAnchorX) {
                    case RIGHT -> anchorCoordX = this.width - StoneworksChatClient.hudOffsetX;
                    case CENTER -> anchorCoordX = (this.width / 2) + StoneworksChatClient.hudOffsetX;
                    default -> anchorCoordX = StoneworksChatClient.hudOffsetX; // LEFT or fallback
                }

                int topY;
                switch (StoneworksChatClient.hudAnchorY) {
                    case TOP -> topY = StoneworksChatClient.hudOffsetY;
                    case CENTER -> topY = (this.height / 2) + StoneworksChatClient.hudOffsetY - bgH / 2;
                    case BOTTOM -> topY = this.height - StoneworksChatClient.hudOffsetY - bgH;
                    default -> topY = StoneworksChatClient.hudPosY; // fallback
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
            StoneworksChatClient.hudTextAlign = currentAlign;

                int screenW = this.width;
                int screenH = this.height;
                int boxW = previewW;
                int boxH = previewH;

                int leftX = switch (currentAlign) {
                    case RIGHT_TO_LEFT -> currentX - boxW;
                    case CENTER -> currentX - boxW / 2;
                    case LEFT_TO_RIGHT -> currentX;
                };

                int distLeft = leftX;
                int distCenter = Math.abs((leftX + boxW / 2) - (screenW / 2));
                int distRight = Math.abs((screenW - (leftX + boxW)));

                if (distCenter <= distLeft && distCenter <= distRight) {
                    StoneworksChatClient.hudAnchorX = StoneworksChatClient.AnchorX.CENTER;
                } else if (distLeft <= distRight) {
                    StoneworksChatClient.hudAnchorX = StoneworksChatClient.AnchorX.LEFT;
                } else {
                    StoneworksChatClient.hudAnchorX = StoneworksChatClient.AnchorX.RIGHT;
                }

                int anchorLineX = switch (currentAlign) {
                    case LEFT_TO_RIGHT -> leftX;                 // left edge of box
                    case CENTER -> leftX + (boxW / 2);           // center of box
                    case RIGHT_TO_LEFT -> leftX + boxW;          // right edge of box
                };

                switch (StoneworksChatClient.hudAnchorX) {
                    case CENTER -> StoneworksChatClient.hudOffsetX = anchorLineX - (screenW / 2);
                    case LEFT -> StoneworksChatClient.hudOffsetX = anchorLineX;
                    case RIGHT -> StoneworksChatClient.hudOffsetX = screenW - anchorLineX;
                }

                int topY = currentY;
                int distTop = topY;
                int distMid = Math.abs((topY + boxH / 2) - (screenH / 2));
                int distBot = Math.abs((screenH - (topY + boxH)));

                if (distMid <= distTop && distMid <= distBot) {
                    StoneworksChatClient.hudAnchorY = StoneworksChatClient.AnchorY.CENTER;
                    StoneworksChatClient.hudOffsetY = (topY + boxH / 2) - (screenH / 2);
                } else if (distTop <= distBot) {
                    StoneworksChatClient.hudAnchorY = StoneworksChatClient.AnchorY.TOP;
                    StoneworksChatClient.hudOffsetY = topY;
                } else {
                    StoneworksChatClient.hudAnchorY = StoneworksChatClient.AnchorY.BOTTOM;
                    StoneworksChatClient.hudOffsetY = screenH - (topY + boxH);
                }

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
        int boxW = previewW;
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
            case CENTER -> currentX = leftXBefore + (boxW / 2); // use integer half to preserve exact leftX
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
            previewW = bgW;
            previewH = bgH;
            boolean rtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
            boolean centerAlign = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
            boolean shiftDown = hasShiftDown();
            if (dragging) {
                int leftXBefore = rtl ? (currentX - bgW) : (centerAlign ? (currentX - bgW / 2) : currentX);
                int clampedLeftX = clamp(leftXBefore, 0, this.width - bgW);
                int clampedY = clamp(currentY, 0, this.height - bgH);
                int leftX = clampedLeftX;
                int yPos = clampedY;
                if (!shiftDown) {
                    final int margin = 10; final int tolerance = 3;
                    int screenCenterX = this.width / 2; int screenCenterY = this.height / 2;
                    int centerLeftX = screenCenterX - (bgW / 2); int rightLeftX = this.width - margin - bgW;
                    if (Math.abs(leftX - margin) <= tolerance) leftX = margin;
                    else if (Math.abs((leftX + bgW) - (this.width - margin)) <= tolerance) leftX = rightLeftX;
                    else if (Math.abs((leftX + bgW / 2) - screenCenterX) <= tolerance) leftX = centerLeftX;
                    int bottomY = this.height - margin - bgH; int centerTopY = screenCenterY - (bgH / 2);
                    if (Math.abs(yPos - margin) <= tolerance) yPos = margin;
                    else if (Math.abs((yPos + bgH) - (this.height - margin)) <= tolerance) yPos = bottomY;
                    else if (Math.abs((yPos + bgH / 2) - screenCenterY) <= tolerance) yPos = centerTopY;
                }
                currentY = yPos;
                currentX = rtl ? (leftX + bgW) : (centerAlign ? (leftX + bgW / 2) : leftX);
            }
            if (dragging && !shiftDown) drawGuides(drawContext, bgW, bgH);
            int leftX = rtl ? (currentX - bgW) : (centerAlign ? (currentX - bgW / 2) : currentX);
            int bgColor = StoneworksChatClient.hudVisible ? 0x80000000 : 0x40404040;
            drawContext.fill(leftX, currentY, leftX + bgW, currentY + bgH, bgColor);
            int borderColor = StoneworksChatClient.hudVisible ? 0x80FFFFFF : 0x60AAAAAA;
            drawContext.fill(leftX, currentY, leftX + bgW, currentY + 1, borderColor);
            drawContext.fill(leftX, currentY + bgH - 1, leftX + bgW, currentY + bgH, borderColor);
            drawContext.fill(leftX, currentY, leftX + 1, currentY + bgH, borderColor);
            drawContext.fill(leftX + bgW - 1, currentY, leftX + bgW, currentY + bgH, borderColor);
            int anchorXpx = switch (currentAlign) {
                case LEFT_TO_RIGHT -> leftX;
                case CENTER -> leftX + (bgW / 2);
                case RIGHT_TO_LEFT -> leftX + bgW;
            };
            int markerTop = Math.max(0, currentY - 4);
            drawContext.fill(anchorXpx, markerTop, anchorXpx + 1, currentY, 0xFF80C0FF);
            int textX = rtl ? (leftX + bgW - paddingX - textW) : (centerAlign ? (leftX + Math.max(0, (bgW - textW) / 2)) : (leftX + paddingX));
            int textY = currentY + Math.round((bgH - textH) / 2f) + 1;
            int colorCode = HudOverlayRenderer.getColorCode(color);
            if (StoneworksChatClient.hudVisible) drawContext.drawText(tr, text, textX, textY, colorCode, true);
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
    int popupW = Math.max(340, contentBlockW + 32); // padding safety
    int textBlockH = Math.max(arrowsH, textRenderer.fontHeight) + lineSpacing + Math.max(shiftH, textRenderer.fontHeight);
    int popupH = 30 + textBlockH + 54; // slight extra space
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
    int shiftImgX = blockStartX; // align images vertically
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
        if (button == 1) { // Right click toggles visibility when clicking the preview
            if (hitPreview(mouseX, mouseY)) {
                StoneworksChatClient.hudVisible = !StoneworksChatClient.hudVisible;
                ChatConfig.save();
                return true;
            }
        }
        if (button == 0) {
            if (hitPreview(mouseX, mouseY)) {
                dragging = true;
                int leftX = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT)
                    ? (currentX - previewW)
                    : (currentAlign == StoneworksChatClient.TextAlign.CENTER ? (currentX - previewW / 2) : currentX);
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
        if (dragging && button == 0) {
            int leftX = (int)mouseX - dragOffsetX;
            currentY = (int)mouseY - dragOffsetY;

            if (null == currentAlign) {
                currentX = leftX;
                currentX = clamp(currentX, 0, this.width - previewW);
            } else switch (currentAlign) {
                case RIGHT_TO_LEFT -> {
                    currentX = leftX + previewW;
                    currentX = clamp(currentX, previewW, this.width);
                }
                case CENTER -> {
                    currentX = leftX + previewW / 2;
                    currentX = clamp(currentX, previewW / 2, this.width - previewW / 2);
                }
                default -> {
                    currentX = leftX;
                    currentX = clamp(currentX, 0, this.width - previewW);
                }
            }
            currentY = clamp(currentY, 0, this.height - previewH);
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
        int leftX = rtl ? (currentX - previewW) : (centerAlign ? (currentX - previewW / 2) : currentX);
        return mouseX >= leftX && mouseX <= leftX + previewW &&
               mouseY >= currentY && mouseY <= currentY + previewH;
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showTutorialPopup) {
            // Allow ESC to close (just this session) but block movement keys
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
            if (null == currentAlign) {
                currentX = clamp(currentX, 0, this.width - previewW);
            } else currentX = switch (currentAlign) {
                case RIGHT_TO_LEFT -> clamp(currentX, previewW, this.width);
                case CENTER -> clamp(currentX, previewW / 2, this.width - previewW / 2);
                default -> clamp(currentX, 0, this.width - previewW);
            };
            currentY = clamp(currentY, 0, this.height - previewH);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
