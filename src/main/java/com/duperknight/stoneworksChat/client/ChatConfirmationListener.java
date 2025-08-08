package com.duperknight.stoneworksChat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.duperknight.stoneworksChat.client.config.ChatConfig;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

public class ChatConfirmationListener {
    private static final Pattern CONFIRM_PATTERN =
            Pattern.compile("You are now talking on ([A-Za-z0-9_ ]+)!",
                            Pattern.CASE_INSENSITIVE);

    private static boolean collectingChannels = false;
    private static final List<String> channelStatuses = new ArrayList<>();
    private static boolean channelListPending = false;
    private static long lastChannelListTime = 0;
    private static final long CHANNEL_LIST_COOLDOWN = 200;
    private static long collectingStartTime = 0L;
    private static long lastStatusLineTime = 0L;
    private static final long LIST_IDLE_PROCESS_MS = 200;
    private static final long LIST_EMPTY_THRESHOLD_MS = 800;

    private static boolean muteStatusLinesActive = false;
    private static final Set<String> expectedNormalized = new HashSet<>();
    private static final Set<String> seenNormalized = new HashSet<>();
    private static long statusMuteStartTime = 0L;
    private static final long STATUS_MUTE_TIMEOUT_MS = 2000;
    private static final Set<String> CANONICAL_KEYS = new HashSet<>(Arrays.asList(
        "public", "LocalChat", "TradeChat", "RPChat", "StaffChat2", "AdminChat"
    ));

    public static void register() {

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {

            if (StoneworksChatClient.modTriggeredChannelsList) {
                return;
            }

            String content = message.getString().trim();

            if (content.equalsIgnoreCase("Channels list:") || content.equalsIgnoreCase("Channel list:")) {
                collectingChannels = true;
                channelStatuses.clear();
                collectingStartTime = System.currentTimeMillis();
                lastStatusLineTime = collectingStartTime;
                return;
            }

            if (collectingChannels
                    && (content.endsWith(" Status: Receiving")
                        || content.endsWith(" Status: Muted")
                        || content.endsWith(" Status: Transmitting"))) {
                channelStatuses.add(content);
                lastStatusLineTime = System.currentTimeMillis();
                if (content.endsWith(" Status: Transmitting")) {
                    collectingChannels = false;
                    processChannelList();
                    return;
                }
                return;
            }

            if (collectingChannels) {
                collectingChannels = false;
                processChannelList();
                return;
            }

            Matcher matcher = CONFIRM_PATTERN.matcher(content);
            if (matcher.find()) {
                String confirmedChannel = matcher.group(1).trim();

                if (StoneworksChatClient.pendingChannel != null
                        && confirmedChannel.equalsIgnoreCase(
                                StoneworksChatClient.pendingChannel)) {

                    setCurrentChannel(StoneworksChatClient.pendingChannel);

                } else {
                    String best = findBestKeyForRaw(confirmedChannel);
                    if (best != null) {
                        setCurrentChannel(best);
                        return;
                    }
                    if (!confirmedChannel.equalsIgnoreCase(StoneworksChatClient.currentChannel)) {
                        String key = resolveOrCreateChannelKey(confirmedChannel);
                        setCurrentChannel(key);
                    }
                }
            }
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {

            String content = message.getString().trim();

            if (StoneworksChatClient.modTriggeredChannelsList) {

                if (content.equalsIgnoreCase("Channels list:") || content.equalsIgnoreCase("Channel list:")) {
                    collectingChannels = true;
                    channelStatuses.clear();
                    collectingStartTime = System.currentTimeMillis();
                    lastStatusLineTime = collectingStartTime;
                    beginStatusMute();
                    return false;
                }

                if (collectingChannels
                        && (content.endsWith(" Status: Receiving")
                            || content.endsWith(" Status: Muted")
                            || content.endsWith(" Status: Transmitting"))) {

                    channelStatuses.add(content);
                    lastStatusLineTime = System.currentTimeMillis();
                    if (content.endsWith(" Status: Transmitting")) {
                        collectingChannels = false;
                        processChannelList();
                    }
                    trackSeenFromStatusLine(content);
                    return false;
                }
            }

            if (muteStatusLinesActive && content.contains(" Status: ")) {
                trackSeenFromStatusLine(content);
                return false;
            }
            return true;
        });
    }

    public static boolean canRequestUpdate() {
        long currentTime = System.currentTimeMillis();
        return !channelListPending && (currentTime - lastChannelListTime) >= CHANNEL_LIST_COOLDOWN;
    }

    public static boolean requestChannelListUpdate() {
        long currentTime = System.currentTimeMillis();
        if (channelListPending) {
            return false;
        }
        if (currentTime - lastChannelListTime < CHANNEL_LIST_COOLDOWN) {
            return false;
        }
        channelListPending = true;
        lastChannelListTime = currentTime;
        StoneworksChatClient.modTriggeredChannelsList = true;
        StoneworksChatClient.sendCommand("channels list");
        return true;
    }

    private static void setCurrentChannel(String newChannel) {
        if (newChannel == null) {
            return;
        }

        if (!newChannel.equals(StoneworksChatClient.currentChannel)) {
            StoneworksChatClient.currentChannel = newChannel;
            StoneworksChatClient.pendingChannel = null;

            ChatConfig.save();
        }
    }

    private static void processChannelList() {

        String transmittingKey = null;

        for (String status : channelStatuses) {

            String[] parts = status.split(" Status: ");
            if (parts.length != 2) continue;

            if ("Transmitting".equals(parts[1])) {
                String rawChannel = parts[0].trim();
                transmittingKey = findBestKeyForRaw(rawChannel);
                if (transmittingKey == null) {
                    transmittingKey = resolveOrCreateChannelKey(rawChannel);
                }
            }
        }

        if (transmittingKey != null) {
            setCurrentChannel(transmittingKey);
        } else {
            setCurrentChannel("public");
        }

        if (StoneworksChatClient.modTriggeredChannelsList) {
            StoneworksChatClient.modTriggeredChannelsList = false;
        }
        channelListPending = false;

        channelStatuses.clear();
    }

    private static String normalizeName(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase();
        t = t.replace(" ", "");
        if (t.endsWith("chat")) {
            t = t.substring(0, t.length() - 4);
        }
        return t;
    }

    public static void checkTimeout() {
        long now = System.currentTimeMillis();

        if (collectingChannels) {
            if (!channelStatuses.isEmpty()) {
                if (now - lastStatusLineTime >= LIST_IDLE_PROCESS_MS) {
                    collectingChannels = false;
                    processChannelList();
                    return;
                }
            } else {
                if (now - collectingStartTime >= LIST_EMPTY_THRESHOLD_MS) {
                    collectingChannels = false;
                    processChannelList();
                    return;
                }
            }
        }

        if (channelListPending && (now - lastChannelListTime > 5000)) {
            channelListPending = false;
            StoneworksChatClient.modTriggeredChannelsList = false;
        }

        if (muteStatusLinesActive) {
            boolean allSeen = !expectedNormalized.isEmpty() && seenNormalized.containsAll(expectedNormalized);
            boolean timedOut = (now - statusMuteStartTime) > STATUS_MUTE_TIMEOUT_MS;
            if (allSeen || timedOut) {
                muteStatusLinesActive = false;
                expectedNormalized.clear();
                seenNormalized.clear();
            }
        }
    }

    private static String resolveOrCreateChannelKey(String rawName) {
        if (rawName == null || rawName.isEmpty()) return "public";
        String best = findBestKeyForRaw(rawName);
        if (best != null) return best;
        String key = rawName;
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("display", rawName);
        info.put("color", "white");
        info.put("aliases", new String[]{});
        info.put("uiName", rawName);
        StoneworksChatClient.channels.put(key, info);
        return key;
    }

    private static String resolveChannelKeyNoCreate(String rawName) {
        return findBestKeyForRaw(rawName);
    }

    private static void beginStatusMute() {
        muteStatusLinesActive = true;
        expectedNormalized.clear();
        seenNormalized.clear();
        statusMuteStartTime = System.currentTimeMillis();
        for (String key : StoneworksChatClient.channels.keySet()) {
            if (!"public".equalsIgnoreCase(key)) {
                expectedNormalized.add(normalizeName(key));
            }
        }
    }

    private static void trackSeenFromStatusLine(String content) {
        int idx = content.indexOf(" Status: ");
        if (idx > 0) {
            String raw = content.substring(0, idx).trim();
            String resolved = findBestKeyForRaw(raw);
            if (resolved != null) {
                seenNormalized.add(normalizeName(resolved));
            } else {
                seenNormalized.add(normalizeName(raw));
            }
        }
    }

    private static boolean isCanonical(String key) {
        return CANONICAL_KEYS.contains(key);
    }

    private static String findBestKeyForRaw(String rawName) {
        if (rawName == null || rawName.isEmpty()) return null;
        String normalizedRaw = normalizeName(rawName);
        for (String key : StoneworksChatClient.channels.keySet()) {
            if (!isCanonical(key)) continue;
            if (normalizeName(key).equals(normalizedRaw)) return key;
            Object disp = StoneworksChatClient.channels.get(key).get("display");
            if (disp != null && normalizeName(disp.toString()).equals(normalizedRaw)) return key;
            Object ui = StoneworksChatClient.channels.get(key).get("uiName");
            if (ui != null && normalizeName(ui.toString()).equals(normalizedRaw)) return key;
        }
        for (String key : StoneworksChatClient.channels.keySet()) {
            if (normalizeName(key).equals(normalizedRaw)) return key;
            Object disp = StoneworksChatClient.channels.get(key).get("display");
            if (disp != null && normalizeName(disp.toString()).equals(normalizedRaw)) return key;
            Object ui = StoneworksChatClient.channels.get(key).get("uiName");
            if (ui != null && normalizeName(ui.toString()).equals(normalizedRaw)) return key;
        }
        return null;
    }
}