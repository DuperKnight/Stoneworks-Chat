package com.duperknight.stoneworksChat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.duperknight.stoneworksChat.client.config.ChatConfig;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class StoneworksChatClient implements ClientModInitializer {

    public static final String MOD_ID = "stoneworks-chat";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static String currentChannel = "public";
    public static String pendingChannel = null;
    public static Map<String, Map<String, Object>> channels = new HashMap<>();

    public static boolean modTriggeredChannelsList = false;

    private static boolean channelGuiWasOpen = false;
    private static long lastGuiCloseTime = 0;
    private static final long GUI_CLOSE_DELAY = 10;
    private static boolean guiTriggeredUpdate = false;
    private static boolean delayedCommandScheduled = false;

    
    public enum TextAlign { LEFT_TO_RIGHT, CENTER, RIGHT_TO_LEFT }
    public static int hudPosX = 10;
    public static int hudPosY = 10;
    public static TextAlign hudTextAlign = TextAlign.LEFT_TO_RIGHT;
    
    public static float hudPosXFrac = -1f;
    public static float hudPosYFrac = -1f;

    
    public enum AnchorX { LEFT, CENTER, RIGHT }
    public static AnchorX hudAnchorX = AnchorX.LEFT;
    public enum AnchorY { TOP, CENTER, BOTTOM }
    public static AnchorY hudAnchorY = AnchorY.TOP;
    public static int hudOffsetX = -1; 
    public static int hudOffsetY = -1; 

    
    private static KeyBinding OPEN_HUD_CONFIG_KEY;

    @Override
    public void onInitializeClient() {
        ChatConfig.load();
        ChatCommandListener.register();
        ChatConfirmationListener.register();
        ChannelGuiListener.register();
        HudOverlayRenderer.register();

        
        OPEN_HUD_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.stoneworks_chat.open_hud_config",
                GLFW.GLFW_KEY_H,
                "key.categories.stoneworks_chat"
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatConfirmationListener.checkTimeout();

            
            while (OPEN_HUD_CONFIG_KEY != null && OPEN_HUD_CONFIG_KEY.wasPressed()) {
                var parent = client.currentScreen;
                client.setScreen(new HudConfigScreen(parent));
            }

            net.minecraft.client.gui.screen.Screen current = client.currentScreen;
            boolean isChannelGui = current instanceof GenericContainerScreen &&
                                 current.getTitle().getString().equals("Channels");

            if (isChannelGui && !channelGuiWasOpen) {
                channelGuiWasOpen = true;
                guiTriggeredUpdate = false;
            }

            else if (!isChannelGui && channelGuiWasOpen) {
                channelGuiWasOpen = false;
                lastGuiCloseTime = System.currentTimeMillis();
                delayedCommandScheduled = false;
            }

            else if (!isChannelGui && !channelGuiWasOpen && lastGuiCloseTime > 0) {
                long timeSinceClose = System.currentTimeMillis() - lastGuiCloseTime;

                if (timeSinceClose >= GUI_CLOSE_DELAY && !guiTriggeredUpdate && !delayedCommandScheduled) {
                    if (ChatConfirmationListener.requestChannelListUpdate()) {
                        delayedCommandScheduled = true;
                    }
                    lastGuiCloseTime = 0;
                }
                else if (guiTriggeredUpdate) {
                    lastGuiCloseTime = 0;
                }
            }

            if (isChannelGui && !channelGuiWasOpen) {
                lastGuiCloseTime = 0;
                delayedCommandScheduled = false;
            }
        });
    }

    public static void markGuiUpdateTriggered() {
        guiTriggeredUpdate = true;
    }
    public static void sendCommand(String command) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            var player = client.player;
            var handler = player != null ? player.networkHandler : null;
            if (handler != null) {
                handler.sendChatCommand(command);
            } else {
                LOGGER.warn("Cannot send command, networkHandler is null");
            }
        } else {
            LOGGER.warn("Cannot send command, client or player is null");
        }
    }
}