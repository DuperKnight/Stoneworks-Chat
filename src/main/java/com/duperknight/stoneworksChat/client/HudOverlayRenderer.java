package com.duperknight.stoneworksChat.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HudOverlayRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneworksChatClient.MOD_ID);
    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            Map<String, Object> channelInfo = StoneworksChatClient.channels.get(StoneworksChatClient.currentChannel);
            if (channelInfo != null) {
                String display = (String) channelInfo.get("display");
                String color = (String) channelInfo.get("color");
                String text = "Chat: " + display;
                int x = 10;
                int y = 10;
                int colorCode = getColorCode(color);
                drawContext.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(text), x, y, colorCode, true);
            }
        });
    }

    private static int getColorCode(String color) {
        return switch (color.toLowerCase()) {
            case "green" -> 0x00FF00;
            case "white" -> 0xFFFFFF;
            case "light_red" -> 0xFF5555;
            case "cyan" -> 0x00FFFF;
            default -> 0xFFFFFF;
        };
    }
} 