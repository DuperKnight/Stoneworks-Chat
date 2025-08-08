package com.duperknight.stoneworksChat.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.Map;

public class HudOverlayRenderer {
    public static void register() {
        // TODO: For 1.21.6 this will no longer work
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            Map<String, Object> channelInfo = StoneworksChatClient.channels.get(StoneworksChatClient.currentChannel);
            if (channelInfo != null) {
                String display = (String) channelInfo.get("display");
                String color = (String) channelInfo.get("color");
                String text = "Chat: " + display;

                int x = 10;
                int y = 10;
                int colorCode = getColorCode(color);
                var tr = MinecraftClient.getInstance().textRenderer;
                int paddingX = 4;
                int paddingY = 3;
                int textW = tr.getWidth(text);
                int textH = tr.fontHeight;
                int bgW = textW + paddingX * 2;
                int bgH = textH + paddingY * 2;

                drawContext.fill(x, y, x + bgW, y + bgH, 0x80000000);

                int textX = x + ((bgW - textW) / 2) + 1;
                int textY = y + ((bgH - textH) / 2) + 1;
                drawContext.drawText(tr, Text.literal(text), textX, textY, colorCode, true);
            }
        });
    }

    private static int getColorCode(String color) {
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