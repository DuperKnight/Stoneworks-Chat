package com.duperknight.stoneworksChat.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import com.duperknight.stoneworksChat.client.config.ChatConfig;
import java.util.Map;
import net.minecraft.screen.slot.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelGuiListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneworksChatClient.MOD_ID);
    private static boolean channelsGuiActive = false;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GenericContainerScreen generic && screen.getTitle().getString().equals("Channels") && generic.getScreenHandler().getRows() == 4) {
                LOGGER.info("Channels GUI opened – scanning for selected lime dye");
                int index = 0;
                boolean anyMatch = false;
                for (Slot slot : generic.getScreenHandler().slots) {
                    ItemStack stack = slot.getStack();
                    if (stack.getItem() == Items.LIME_DYE) {
                        String uiName = stack.getName().getString();
                        LOGGER.info("Lime dye [{}]: {}", index, uiName);
                        boolean matched = false;
                        for (Map.Entry<String, Map<String, Object>> entry : StoneworksChatClient.channels.entrySet()) {
                            String key = entry.getKey();
                            String configUiName = (String) entry.getValue().get("uiName");
                            if (configUiName != null && uiName.toLowerCase().contains(configUiName.toLowerCase())) {
                                StoneworksChatClient.currentChannel = key;
                                StoneworksChatClient.pendingChannel = null;
                                ChatConfig.save();
                                LOGGER.info("Updated currentChannel from GUI to: {} (matched '{}')", key, configUiName);
                                matched = true;
                                break;
                            }
                        }
                        if (matched) {
                            anyMatch = true;
                            break; // stop scanning after successful match
                        }
                    }
                    index++;
                }
                if (!anyMatch) {
                    // dump first 36 slots for debugging
                    StringBuilder sb = new StringBuilder("Top 36 slot contents: ");
                    for (int i = 0; i < Math.min(36, generic.getScreenHandler().slots.size()); i++) {
                        ItemStack st = generic.getScreenHandler().slots.get(i).getStack();
                        sb.append(i).append(':').append(st.getItem().toString()).append(',');
                    }
                    LOGGER.info(sb.toString());
                }
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