package com.duperknight.stoneworksChat.client;


import com.duperknight.stoneworksChat.client.config.ChatConfig;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class StoneworksChatClient implements ClientModInitializer {

    public static final String MOD_ID = "stoneworks-chat";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static String currentChannel = "public";
    public static String pendingChannel = null;
    public static Map<String, Channel> channels = new HashMap<>();

    public static boolean modTriggeredChannelsList = false;

    private static boolean channelGuiWasOpen = false;
    private static long lastGuiCloseTime = 0;
    private static final long GUI_CLOSE_DELAY = 10;
    private static boolean delayedCommandScheduled = false;

    
    public enum TextAlign { LEFT_TO_RIGHT, CENTER, RIGHT_TO_LEFT }
    public static int hudPosX = 10;
    public static int hudPosY = 10;
    public static TextAlign hudTextAlign = TextAlign.LEFT_TO_RIGHT;

    public static float hudScale = 1.0f;

    public static final float HUD_MIN_SCALE = 0.75f;
    public static final float HUD_MAX_SCALE = 4.0f;
    

    
    public enum AnchorX { LEFT, CENTER, RIGHT }
    public static AnchorX hudAnchorX = null; // may be null when using raw coordinates
    public enum AnchorY { TOP, CENTER, BOTTOM }
    public static AnchorY hudAnchorY = null; // may be null when using raw coordinates
    public static int hudOffsetX = -1; 
    public static int hudOffsetY = -1; 
    public static boolean hudVisible = true;
    public static boolean showHudTutorial = true;

    
    private static KeyBinding OPEN_HUD_CONFIG_KEY;
    private static KeyBinding TOGGLE_HUD_VISIBILITY_KEY;

    @Override
    public void onInitializeClient() {
        ChatConfig.load();
        ChatCommandListener.register();
        ChatConfirmationListener.register();
        HudOverlayRenderer.register();

        
        OPEN_HUD_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.stoneworks_chat.open_hud_config",
                GLFW.GLFW_KEY_H,
                "key.categories.stoneworks_chat"
            )
        );

        TOGGLE_HUD_VISIBILITY_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.stoneworks_chat.toggle_hud_visibility",
                GLFW.GLFW_KEY_V,
                "key.categories.stoneworks_chat"
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatConfirmationListener.checkTimeout();

            
            while (OPEN_HUD_CONFIG_KEY != null && OPEN_HUD_CONFIG_KEY.wasPressed()) {
                var parent = client.currentScreen;
                client.setScreen(new HudConfigScreen(parent));
            }

            while (TOGGLE_HUD_VISIBILITY_KEY != null && TOGGLE_HUD_VISIBILITY_KEY.wasPressed()) {
                hudVisible = !hudVisible;
                ChatConfig.save();
            }

            Screen current = client.currentScreen;
            boolean isChannelGui = current instanceof GenericContainerScreen &&
                                 current.getTitle().getString().equals("Channels");

            if (isChannelGui && !channelGuiWasOpen) {
                channelGuiWasOpen = true;
            }

            else if (!isChannelGui && channelGuiWasOpen) {
                channelGuiWasOpen = false;
                lastGuiCloseTime = System.currentTimeMillis();
                delayedCommandScheduled = false;
            }

            else if (!isChannelGui && !channelGuiWasOpen && lastGuiCloseTime > 0) {
                long timeSinceClose = System.currentTimeMillis() - lastGuiCloseTime;

                if (timeSinceClose >= GUI_CLOSE_DELAY && !delayedCommandScheduled) {
                    if (ChatConfirmationListener.requestChannelListUpdate()) {
                        delayedCommandScheduled = true;
                    }
                    lastGuiCloseTime = 0;
                }
            }

            if (isChannelGui && !channelGuiWasOpen) {
                lastGuiCloseTime = 0;
                delayedCommandScheduled = false;
            }
        });
    }

    public static void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
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