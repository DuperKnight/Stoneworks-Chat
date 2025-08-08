package com.duperknight.stoneworksChat.client;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import java.util.Map;
import java.util.List;

public class ChatCommandListener {
    public static void register() {
        ClientSendMessageEvents.ALLOW_COMMAND.register((message) -> {
            String lowered = message.trim().toLowerCase();

            if (lowered.startsWith("talkon ")) {
                String channelInput = lowered.substring(7).trim();
                for (String key : StoneworksChatClient.channels.keySet()) {
                    if (key.equalsIgnoreCase(channelInput)) {
                        StoneworksChatClient.pendingChannel = key;
                        return true;
                    }
                }
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
                            return true;
                        }
                    }
                } else if (aliasesObj instanceof String[]) {
                    for (String alias : (String[]) aliasesObj) {
                        String aliasNoSlash = alias.startsWith("/") ? alias.substring(1) : alias;
                        if (lowered.equals(aliasNoSlash) || lowered.startsWith(aliasNoSlash + " ")) {
                            StoneworksChatClient.pendingChannel = key;
                            return true;
                        }
                    }
                }
            }
            return true;
        });
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            return true;
        });
    }
}