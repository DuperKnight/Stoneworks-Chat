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
            if (screen instanceof GenericContainerScreen && "Channels".equals(screen.getTitle().getString())) {
                LOGGER.info("Channels GUI opened (listener)");
                channelsGuiActive = true;

                ScreenEvents.remove(screen).register((s) -> {
                    if (channelsGuiActive) {
                        channelsGuiActive = false;
                        LOGGER.info("Channels GUI closed (listener)");
                    }
                });
            }
        });
    }
}