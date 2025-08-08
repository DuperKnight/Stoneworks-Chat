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

                boolean rtl = (StoneworksChatClient.hudTextAlign == StoneworksChatClient.TextAlign.RIGHT_TO_LEFT);
                boolean center = (StoneworksChatClient.hudTextAlign == StoneworksChatClient.TextAlign.CENTER);
                int leftX;
                boolean useAnchorX = (StoneworksChatClient.hudAnchorX == StoneworksChatClient.AnchorX.CENTER)
                    || (StoneworksChatClient.hudOffsetX >= 0);
                if (useAnchorX) {
                    switch (StoneworksChatClient.hudAnchorX) {
                        case RIGHT: leftX = screenW - StoneworksChatClient.hudOffsetX - bgW; break;
                        case CENTER: leftX = (screenW / 2) - (bgW / 2) + StoneworksChatClient.hudOffsetX; break;
                        case LEFT: default: leftX = StoneworksChatClient.hudOffsetX; break;
                    }
                } else {
                    if (StoneworksChatClient.hudPosXFrac >= 0f) {
                        xAnchor = Math.round(StoneworksChatClient.hudPosXFrac * screenW);
                    }
                    leftX = rtl ? (xAnchor - bgW) : (center ? (xAnchor - bgW / 2) : xAnchor);
                }

                boolean useAnchorY = (StoneworksChatClient.hudAnchorY == StoneworksChatClient.AnchorY.CENTER)
                    || (StoneworksChatClient.hudOffsetY >= 0);
                if (useAnchorY) {
                    switch (StoneworksChatClient.hudAnchorY) {
                        case BOTTOM: y = screenH - StoneworksChatClient.hudOffsetY - bgH; break;
                        case CENTER: y = (screenH / 2) - (bgH / 2) + StoneworksChatClient.hudOffsetY; break;
                        case TOP: default: y = StoneworksChatClient.hudOffsetY; break;
                    }
                } else if (StoneworksChatClient.hudPosYFrac >= 0f) {
                    y = Math.round(StoneworksChatClient.hudPosYFrac * screenH);
                }

                drawContext.fill(leftX, y, leftX + bgW, y + bgH, 0x80000000);

                int textX;
                if (rtl) {
                    textX = leftX + bgW - paddingX - textW;
                } else if (center) {
                    textX = leftX + Math.max(0, (bgW - textW) / 2);
                } else {
                    textX = leftX + paddingX;
                }
                int textY = y + Math.round((bgH - textH) / 2f) + 1;
                drawContext.drawText(tr, text, textX, textY, colorCode, true);
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