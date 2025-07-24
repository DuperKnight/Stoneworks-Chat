package com.duperknight.stoneworksChat.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import com.duperknight.stoneworksChat.client.config.ChatConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatConfirmationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneworksChatClient.MOD_ID);
    private static final Pattern CONFIRM_PATTERN = Pattern.compile("You are now talking on ([A-Za-z]+)!", Pattern.CASE_INSENSITIVE);
    private static boolean collectingChannels = false;
    private static java.util.List<String> channelStatuses = new java.util.ArrayList<>();

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String content = message.getString().trim();
            LOGGER.info("Received game message: {}", content);
            if (content.equals("Channel list:")) {
                collectingChannels = true;
                channelStatuses.clear();
                LOGGER.info("Started collecting channel list");
                return;
            }
            if (collectingChannels && (content.endsWith(" Status: Receiving") || content.endsWith(" Status: Muted") || content.endsWith(" Status: Transmitting"))) {
                channelStatuses.add(content);
                if (channelStatuses.size() >= 4) { // typical list length
                    collectingChannels = false;
                    processChannelList();
                }
                return;
            }
            if (collectingChannels && !content.isEmpty()) { // non-empty non-status ends list
                collectingChannels = false;
                processChannelList();
                return;
            }
            Matcher matcher = CONFIRM_PATTERN.matcher(content);
            if (matcher.find()) {
                LOGGER.info("Pattern found in game message");
                String confirmedChannel = matcher.group(1).trim();
                LOGGER.info("Extracted confirmedChannel: {}", confirmedChannel);
                if (StoneworksChatClient.pendingChannel != null && confirmedChannel.equalsIgnoreCase(StoneworksChatClient.pendingChannel)) {
                    StoneworksChatClient.currentChannel = StoneworksChatClient.pendingChannel;
                    StoneworksChatClient.pendingChannel = null;
                    ChatConfig.save();
                    LOGGER.info("Updated currentChannel to: {}", StoneworksChatClient.currentChannel);
                } else {
                    LOGGER.warn("Confirmation mismatch or no pending: {} (pending: {})", confirmedChannel, StoneworksChatClient.pendingChannel);
                }
            } else {
                LOGGER.debug("Pattern not found in game message: {}", content);
            }
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String content = message.getString().trim();

            // If the mod triggered "/channels list", parse the list *here* before hiding it from chat.
            if (StoneworksChatClient.modTriggeredChannelsList) {
                // Start of list
                if (content.equals("Channel list:")) {
                    collectingChannels = true;
                    channelStatuses.clear();
                    LOGGER.info("[HIDE] Started collecting channel list");
                    return false; // hide line
                }

                // Lines containing status information
                if (collectingChannels && (content.endsWith(" Status: Receiving") || content.endsWith(" Status: Muted") || content.endsWith(" Status: Transmitting"))) {
                    channelStatuses.add(content);
                    // Typical list length is 4 but be safe – process once we have all configured channels or 8 lines
                    if (channelStatuses.size() >= 4 && channelStatuses.size() >= StoneworksChatClient.channels.size()) {
                        collectingChannels = false;
                        processChannelList();
                    }
                    return false; // hide line
                }

                // Any other non-empty line ends the list
                if (collectingChannels && !content.isEmpty()) {
                    collectingChannels = false;
                    processChannelList();
                    return false;
                }

                // Fall-through: hide all list-related lines while flag is set
                if (collectingChannels) {
                    return false;
                }
            }

            // For all other messages, allow them to display.
            return true;
        });
    }

    private static void processChannelList() {
        LOGGER.info("Processing list of {} statuses", channelStatuses.size());
        String transmitting = null;
        for (String status : channelStatuses) {
            String[] parts = status.split(" Status: ");
            if (parts.length == 2 && parts[1].equals("Transmitting")) {
                String channel = parts[0];
                LOGGER.info("Found transmitting: {}", channel);
                for (String key : StoneworksChatClient.channels.keySet()) {
                    if (key.equalsIgnoreCase(channel)) {
                        transmitting = key;
                        LOGGER.info("Matched config key: {}", key);
                        break;
                    }
                }
            }
        }
        String newChannel = (transmitting != null) ? transmitting : "public";
        LOGGER.info("Selected newChannel: {}", newChannel);
        if (!newChannel.equals(StoneworksChatClient.currentChannel)) {
            StoneworksChatClient.currentChannel = newChannel;
            StoneworksChatClient.pendingChannel = null;
            ChatConfig.save();
            LOGGER.info("Updated from channel list: {}", newChannel);
        } else {
            LOGGER.info("Channel list confirms current: {}", newChannel);
        }
        StoneworksChatClient.modTriggeredChannelsList = false;
        channelStatuses.clear();
    }
} 