package com.duperknight.stoneworksChat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.duperknight.stoneworksChat.client.config.ChatConfig;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class StoneworksChatClient implements ClientModInitializer {

    public static final String MOD_ID = "stoneworks-chat";
    public static String currentChannel = "public";
    public static String pendingChannel = null;
    public static Map<String, Map<String, Object>> channels = new HashMap<>();

    private static net.minecraft.client.gui.screen.Screen lastScreen = null;
    private static boolean sentChannelsList = false;
    public static boolean modTriggeredChannelsList = false;

    @Override
    public void onInitializeClient() {
        ChatConfig.load();
        ChatCommandListener.register();
        ChatConfirmationListener.register();
        ChannelGuiListener.register();
        HudOverlayRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            net.minecraft.client.gui.screen.Screen current = client.currentScreen;
            if (lastScreen instanceof GenericContainerScreen && lastScreen.getTitle().getString().equals("Channels") && current == null && !sentChannelsList) {
                modTriggeredChannelsList = true;
                client.player.networkHandler.sendChatCommand("channels list");
                sentChannelsList = true;
            } else if (current != null) {
                sentChannelsList = false;
            }
            lastScreen = current;
        });
    }
}
