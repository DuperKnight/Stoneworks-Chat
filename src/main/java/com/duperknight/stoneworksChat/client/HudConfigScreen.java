package com.duperknight.stoneworksChat.client;

import java.util.Map;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import com.duperknight.stoneworksChat.client.config.ChatConfig;

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

    public HudConfigScreen(Screen parent) {
    super(Text.translatable("screen.stoneworks_chat.hud_config.title"));
        this.parent = parent;
    this.currentX = StoneworksChatClient.hudPosX;
    this.currentY = StoneworksChatClient.hudPosY;
        this.currentAlign = StoneworksChatClient.hudTextAlign;
    }

    @Override
    protected void init() {
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
                int leftX;
                switch (StoneworksChatClient.hudAnchorX) {
                    case LEFT -> leftX = StoneworksChatClient.hudOffsetX;
                    case CENTER -> leftX = (this.width / 2) + StoneworksChatClient.hudOffsetX - bgW / 2;
                    case RIGHT -> leftX = this.width - StoneworksChatClient.hudOffsetX - bgW;
                    default -> leftX = StoneworksChatClient.hudPosX; // fallback
                }
                int topY;
                switch (StoneworksChatClient.hudAnchorY) {
                    case TOP -> topY = StoneworksChatClient.hudOffsetY;
                    case CENTER -> topY = (this.height / 2) + StoneworksChatClient.hudOffsetY - bgH / 2;
                    case BOTTOM -> topY = this.height - StoneworksChatClient.hudOffsetY - bgH;
                    default -> topY = StoneworksChatClient.hudPosY; // fallback
                }
                switch (currentAlign) {
                    case RIGHT_TO_LEFT -> this.currentX = leftX + bgW;
                    case CENTER -> this.currentX = leftX + bgW / 2;
                    case LEFT_TO_RIGHT -> this.currentX = leftX;
                }
                this.currentY = topY;
            }
        } catch (Exception ignored) {
        }

        addDrawableChild(
            ButtonWidget.builder(Text.translatable("screen.stoneworks_chat.hud_config.alignment", alignLabelText()), b -> {
                toggleAlign();
                b.setMessage(Text.translatable("screen.stoneworks_chat.hud_config.alignment", alignLabelText()));
            }).dimensions(centerX - btnW / 2, this.height - (btnH * 2 + spacing + 12), btnW, btnH).build()
        );

        addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done"), b -> {
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
                    StoneworksChatClient.hudOffsetX = (leftX + boxW / 2) - (screenW / 2);
                } else if (distLeft <= distRight) {
                    StoneworksChatClient.hudAnchorX = StoneworksChatClient.AnchorX.LEFT;
                    StoneworksChatClient.hudOffsetX = leftX;
                } else {
                    StoneworksChatClient.hudAnchorX = StoneworksChatClient.AnchorX.RIGHT;
                    StoneworksChatClient.hudOffsetX = screenW - (leftX + boxW);
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
            }).dimensions(centerX - btnW / 2, this.height - (btnH + 12), btnW, btnH).build()
        );
    }

    private void toggleAlign() {
        // Preserve the current box's leftX across alignment toggles to avoid 1px jumps on odd widths
        int boxW = previewW;
        boolean wasRtl = (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
        boolean wasCenter = (currentAlign == StoneworksChatClient.TextAlign.CENTER);
        int leftXBefore = wasRtl ? (currentX - boxW) : (wasCenter ? (currentX - boxW / 2) : currentX);

        // Advance alignment
        switch (currentAlign) {
            case LEFT_TO_RIGHT -> currentAlign = StoneworksChatClient.TextAlign.CENTER;
            case CENTER -> currentAlign = StoneworksChatClient.TextAlign.RIGHT_TO_LEFT;
            case RIGHT_TO_LEFT -> currentAlign = StoneworksChatClient.TextAlign.LEFT_TO_RIGHT;
        }

        // Recompute currentX for new alignment so leftX remains identical
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
                    final int margin = 10;
                    final int tolerance = 3;
                    int screenCenterX = this.width / 2;
                    int screenCenterY = this.height / 2;

                    int centerLeftX = screenCenterX - (bgW / 2);
                    int rightLeftX = this.width - margin - bgW;

                    if (Math.abs(leftX - margin) <= tolerance) {
                        leftX = margin;
                    } else if (Math.abs((leftX + bgW) - (this.width - margin)) <= tolerance) {
                        leftX = rightLeftX;
                    } else if (Math.abs((leftX + bgW / 2) - screenCenterX) <= tolerance) {
                        leftX = centerLeftX;
                    }

                    int bottomY = this.height - margin - bgH;
                    int centerTopY = screenCenterY - (bgH / 2);

                    if (Math.abs(yPos - margin) <= tolerance) {
                        yPos = margin;
                    } else if (Math.abs((yPos + bgH) - (this.height - margin)) <= tolerance) {
                        yPos = bottomY;
                    } else if (Math.abs((yPos + bgH / 2) - screenCenterY) <= tolerance) {
                        yPos = centerTopY;
                    }
                }

                currentY = yPos;
                currentX = rtl ? (leftX + bgW) : (centerAlign ? (leftX + bgW / 2) : leftX);
            }

            if (dragging && !shiftDown) {
                drawGuides(drawContext, bgW, bgH);
            }

            int leftX = rtl ? (currentX - bgW) : (centerAlign ? (currentX - bgW / 2) : currentX);

            drawContext.fill(leftX, currentY, leftX + bgW, currentY + bgH, 0x80000000);

            int borderColor = 0x80FFFFFF;
            drawContext.fill(leftX, currentY, leftX + bgW, currentY + 1, borderColor);
            drawContext.fill(leftX, currentY + bgH - 1, leftX + bgW, currentY + bgH, borderColor);
            drawContext.fill(leftX, currentY, leftX + 1, currentY + bgH, borderColor);
            drawContext.fill(leftX + bgW - 1, currentY, leftX + bgW, currentY + bgH, borderColor);

            int textX;
            if (rtl) {
                textX = leftX + bgW - paddingX - textW;
            } else if (centerAlign) {
                textX = leftX + Math.max(0, (bgW - textW) / 2);
            } else {
                textX = leftX + paddingX;
            }
            int textY = currentY + Math.round((bgH - textH) / 2f) + 1;

            int colorCode = HudOverlayRenderer.getColorCode(color);
            drawContext.drawText(tr, text, textX, textY, colorCode, true);
        }

        drawContext.drawCenteredTextWithShadow(
            textRenderer,
            Text.translatable("screen.stoneworks_chat.hud_config.drag_help"),
            this.width / 2,
            this.height - 84,
            0xAAAAAA
        );

        drawContext.getMatrices().pop();
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
        if (dragging && button == 0) {
            int leftX = (int)mouseX - dragOffsetX;
            currentY = (int)mouseY - dragOffsetY;

            if (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT) {
                currentX = leftX + previewW;
                currentX = clamp(currentX, previewW, this.width);
            } else if (currentAlign == StoneworksChatClient.TextAlign.CENTER) {
                currentX = leftX + previewW / 2;
                currentX = clamp(currentX, previewW / 2, this.width - previewW / 2);
            } else {
                currentX = leftX;
                currentX = clamp(currentX, 0, this.width - previewW);
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
        if (this.client != null) {
            this.client.setScreen(parent);
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
        int step = hasShiftDown() ? 5 : 1;
        boolean handled = false;

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            currentX -= step;
            handled = true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            currentX += step;
            handled = true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            currentY -= step;
            handled = true;
        } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
            currentY += step;
            handled = true;
        }

        if (handled) {
            if (currentAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT) {
                currentX = clamp(currentX, previewW, this.width);
            } else if (currentAlign == StoneworksChatClient.TextAlign.CENTER) {
                currentX = clamp(currentX, previewW / 2, this.width - previewW / 2);
            } else {
                currentX = clamp(currentX, 0, this.width - previewW);
            }
            currentY = clamp(currentY, 0, this.height - previewH);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
