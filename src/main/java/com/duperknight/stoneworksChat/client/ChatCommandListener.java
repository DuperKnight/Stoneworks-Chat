package com.duperknight.stoneworksChat.client;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatCommandListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneworksChatClient.MOD_ID);
    public static void register() {
        ClientSendMessageEvents.ALLOW_COMMAND.register((message) -> {
            LOGGER.info("Intercepting outgoing command: {}", message);
            String lowered = message.trim().toLowerCase();
            LOGGER.info("Normalized command string: {}", lowered);

            if (lowered.startsWith("talkon ")) {
                String channelInput = lowered.substring(7).trim();
                for (String key : StoneworksChatClient.channels.keySet()) {
                    if (key.equalsIgnoreCase(channelInput)) {
                        StoneworksChatClient.pendingChannel = key;
                        LOGGER.info("Detected /talkon {}, set pending: {}", channelInput, key);
                        return true;
                    }
                }
                // If not found, fall through to alias check
            }

            for (Map.Entry<String, Map<String, Object>> entry : StoneworksChatClient.channels.entrySet()) {
                String key = entry.getKey();
                Object aliasesObj = entry.getValue().get("aliases");
                if (aliasesObj instanceof List<?>) {
                    for (Object aliasObj : (List<?>) aliasesObj) {
                        String alias = aliasObj.toString();
                        String aliasNoSlash = alias.startsWith("/") ? alias.substring(1) : alias;
                        if (lowered.equals(aliasNoSlash) || lowered.startsWith(aliasNoSlash + " ")) {
                            StoneworksChatClient.pendingChannel = key;
                            LOGGER.info("Detected alias {}, set pending: {}", alias, StoneworksChatClient.pendingChannel);
                            return true;
                        }
                    }
                } else if (aliasesObj instanceof String[]) {
                    for (String alias : (String[]) aliasesObj) {
                        String aliasNoSlash = alias.startsWith("/") ? alias.substring(1) : alias;
                        if (lowered.equals(aliasNoSlash) || lowered.startsWith(aliasNoSlash + " ")) {
                            StoneworksChatClient.pendingChannel = key;
                            LOGGER.info("Detected alias {}, set pending: {}", alias, StoneworksChatClient.pendingChannel);
                            return true;
                        }
                    }
                }
            }
            LOGGER.debug("Unmatched command: {}", lowered);
            return true;
        });
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            LOGGER.info("Intercepting outgoing chat: {}", message);
            return true;
        });
    }
} 