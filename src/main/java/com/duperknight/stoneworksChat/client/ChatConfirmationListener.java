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
                if (content.endsWith(" Status: Transmitting")) {
                    collectingChannels = false;
                    processChannelList();
                }
                return;
            }
            if (collectingChannels && !content.isEmpty()) {
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
                    // Try to match by display name if direct match fails
                    boolean matchedByDisplay = false;
                    for (String key : StoneworksChatClient.channels.keySet()) {
                        Object displayObj = StoneworksChatClient.channels.get(key).get("display");
                        if (displayObj != null && displayObj.toString().equalsIgnoreCase(confirmedChannel)) {
                            StoneworksChatClient.currentChannel = key;
                            StoneworksChatClient.pendingChannel = null;
                            ChatConfig.save();
                            LOGGER.info("Updated currentChannel by display name: {} (key: {})", confirmedChannel, key);
                            matchedByDisplay = true;
                            break;
                        }
                    }
                    if (!matchedByDisplay) {
                        LOGGER.warn("Confirmation mismatch or no pending: {} (pending: {})", confirmedChannel, StoneworksChatClient.pendingChannel);
                    }
                }
            } else {
                LOGGER.debug("Pattern not found in game message: {}", content);
            }
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String content = message.getString().trim();

            // Always hide channel list messages, regardless of who triggered them
            if (content.equals("Channel list:")) {
                collectingChannels = true;
                channelStatuses.clear();
                return false; // hide line
            }

            // While collecting, hide all status lines and process if needed
            if (collectingChannels) {
                if (content.endsWith(" Status: Receiving") || content.endsWith(" Status: Muted") || content.endsWith(" Status: Transmitting")) {
                    channelStatuses.add(content);
                    if (content.endsWith(" Status: Transmitting")) {
                        processChannelList();
                    }
                    return false; // hide all status lines
                }
                // End of channel list: hide this line, then stop collecting
                collectingChannels = false;
                channelStatuses.clear();
                return false;
            }

            return true;
        });
    }

    private static void processChannelList() {
        LOGGER.info("Processing list of {} statuses", channelStatuses.size());
        String transmitting = null;
        
        // Log all channel statuses for debugging
        for (String status : channelStatuses) {
            LOGGER.info("Channel status: {}", status);
        }
        
        // Look for any channel with "Transmitting" status
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
        
        // If no channel is transmitting, we're on public channel
        String newChannel = (transmitting != null) ? transmitting : "public";
        LOGGER.info("Selected newChannel: {} (transmitting channel found: {})", newChannel, transmitting != null);
        
        if (!newChannel.equals(StoneworksChatClient.currentChannel)) {
            StoneworksChatClient.currentChannel = newChannel;
            StoneworksChatClient.pendingChannel = null;
            ChatConfig.save();
            LOGGER.info("Updated from channel list: {}", newChannel);
        } else {
            LOGGER.info("Channel list confirms current: {}", newChannel);
        }
        // Only reset the flag if it was set by the mod
        if (StoneworksChatClient.modTriggeredChannelsList) {
            StoneworksChatClient.modTriggeredChannelsList = false;
        }
        channelStatuses.clear();
    }
}