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

            for (Map.Entry<String, Channel> entry : StoneworksChatClient.channels.entrySet()) {
                String key = entry.getKey();
                Channel channel = entry.getValue();
                List<String> aliases = channel.aliases();
                if (aliases != null) {
                    for (String alias : aliases) {
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