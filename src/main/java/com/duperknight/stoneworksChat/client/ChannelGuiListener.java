package com.duperknight.stoneworksChat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

public class ChannelGuiListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneworksChatClient.MOD_ID);
    private static boolean channelsGuiActive = false;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GenericContainerScreen generic && screen.getTitle().getString().equals("Channels") && generic.getScreenHandler().getRows() == 4) {
                if (StoneworksChatClient.modTriggeredChannelsList) {
                    LOGGER.warn("Channel list update already pending, ignoring GUI open.");
                    return;
                }
                LOGGER.info("Channels GUI opened");
                channelsGuiActive = true;
                StoneworksChatClient.modTriggeredChannelsList = true;
                ScreenEvents.remove(screen).register((s) -> {
                    if (channelsGuiActive) {
                        channelsGuiActive = false;
                        client.player.networkHandler.sendChatCommand("channels list");
                        LOGGER.info("GUI closed - sent /channels list");
                    }
                });
            }
        });
    }
} 