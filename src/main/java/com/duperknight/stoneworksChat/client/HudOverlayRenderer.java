package com.duperknight.stoneworksChat.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.Map;

public class HudOverlayRenderer {
    public static void register() {
        // TODO: For 1.21.6 this will no longer work
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            var mc = MinecraftClient.getInstance();
            if (mc != null && mc.currentScreen instanceof HudConfigScreen) {
                return;
            }
            if (!StoneworksChatClient.hudVisible) {
                return; // HUD hidden
            }
            Map<String, Object> channelInfo = StoneworksChatClient.channels.get(StoneworksChatClient.currentChannel);
            if (channelInfo != null) {
                String display = (String) channelInfo.get("display");
                String color = (String) channelInfo.get("color");
                Text text = Text.translatable("stoneworks_chat.chat_prefix", (display != null ? display : ""));

                int screenW = drawContext.getScaledWindowWidth();
                int screenH = drawContext.getScaledWindowHeight();

                int xAnchor = StoneworksChatClient.hudPosX;
                int y = StoneworksChatClient.hudPosY;
                int colorCode = getColorCode(color);
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

                boolean rtl = (StoneworksChatClient.hudTextAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
                boolean center = (StoneworksChatClient.hudTextAlign == StoneworksChatClient.TextAlign.CENTER);
                int leftX;
                boolean useAnchorX = (StoneworksChatClient.hudAnchorX == StoneworksChatClient.AnchorX.CENTER)
                    || (StoneworksChatClient.hudOffsetX >= 0);
                if (useAnchorX) {
                    int anchorCoordX;
                    switch (StoneworksChatClient.hudAnchorX) {
                        case RIGHT:
                            anchorCoordX = screenW - StoneworksChatClient.hudOffsetX;
                            break;
                        case CENTER:
                            anchorCoordX = (screenW / 2) + StoneworksChatClient.hudOffsetX;
                            break;
                        case LEFT:
                        default:
                            anchorCoordX = StoneworksChatClient.hudOffsetX;
                            break;
                    }
                    leftX = rtl ? (anchorCoordX - scaledBgW)
                                 : (center ? (anchorCoordX - (scaledBgW / 2))
                                           : anchorCoordX);
                } else {
                    if (StoneworksChatClient.hudPosXFrac >= 0f) {
                        xAnchor = Math.round(StoneworksChatClient.hudPosXFrac * screenW);
                    }
                    leftX = rtl ? (xAnchor - scaledBgW) : (center ? (xAnchor - scaledBgW / 2) : xAnchor);
                }

                boolean useAnchorY = (StoneworksChatClient.hudAnchorY == StoneworksChatClient.AnchorY.CENTER)
                    || (StoneworksChatClient.hudOffsetY >= 0);
                if (useAnchorY) {
                    switch (StoneworksChatClient.hudAnchorY) {
            case BOTTOM: y = screenH - StoneworksChatClient.hudOffsetY - scaledBgH; break;
            case CENTER: y = (screenH / 2) - (scaledBgH / 2) + StoneworksChatClient.hudOffsetY; break;
                        case TOP: default: y = StoneworksChatClient.hudOffsetY; break;
                    }
                } else if (StoneworksChatClient.hudPosYFrac >= 0f) {
                    y = Math.round(StoneworksChatClient.hudPosYFrac * screenH);
                }
        drawContext.fill(leftX, y, leftX + scaledBgW, y + scaledBgH, 0x80000000);

                int textX = rtl
                    ? leftX + scaledBgW - Math.round(paddingX * scale) - Math.round(textW * scale)
                    : (center
                        ? leftX + Math.max(0, (scaledBgW - Math.round(textW * scale)) / 2)
                        : leftX + Math.round(paddingX * scale));
                int textY = y + Math.round((scaledBgH - Math.round(textH * scale)) / 2f) + 1;
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(textX, textY, 0);
                drawContext.getMatrices().scale(scale, scale, 1.0f);
                drawContext.drawText(tr, text, 0, 0, colorCode, true);
                drawContext.getMatrices().pop();
            }
        });
    }

    public static int getColorCode(String color) {
        return switch (color.toLowerCase()) {
            case "green" -> 0x00FF00;
            case "white" -> 0xFFFFFF;
            case "light_red" -> 0xFF5555;
            case "cyan" -> 0x00FFFF;
            case "red" -> 0xFF0000;
            case "yellow" -> 0xFFFF00;
            default -> 0xFFFFFF;
        };
    }
}