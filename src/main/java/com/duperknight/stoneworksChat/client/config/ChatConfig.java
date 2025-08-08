package com.duperknight.stoneworksChat.client.config;

import com.duperknight.stoneworksChat.client.StoneworksChatClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneworksChatClient.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "stoneworks_chat.json");

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> config = GSON.fromJson(reader, type);
                Object cc = config.get("currentChannel");
                StoneworksChatClient.currentChannel = cc instanceof String ? (String) cc : "public";
                Object channelsObj = config.get("channels");
                Map<String, Map<String, Object>> loadedChannels = new HashMap<>();
                if (channelsObj instanceof Map<?, ?> rawMap) {
                    for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                        if (e.getKey() instanceof String key && e.getValue() instanceof Map<?, ?> inner) {
                            Map<String, Object> innerMap = new HashMap<>();
                            for (Map.Entry<?, ?> ie : inner.entrySet()) {
                                if (ie.getKey() instanceof String ik) {
                                    innerMap.put(ik, ie.getValue());
                                }
                            }
                            loadedChannels.put(key, innerMap);
                        }
                    }
                }
                StoneworksChatClient.channels = loadedChannels;

                // HUD position and alignment
                Object hudX = config.get("hudX");
                Object hudY = config.get("hudY");
                Object align = config.get("textAlign");
                Object fx = config.get("hudXFrac");
                Object fy = config.get("hudYFrac");
                Object anchorX = config.get("hudAnchorX");
                Object anchorY = config.get("hudAnchorY");
                Object offX = config.get("hudOffsetX");
                Object offY = config.get("hudOffsetY");
                if (hudX instanceof Number nX) StoneworksChatClient.hudPosX = nX.intValue();
                if (hudY instanceof Number nY) StoneworksChatClient.hudPosY = nY.intValue();
                if (fx instanceof Number nx) StoneworksChatClient.hudPosXFrac = nx.floatValue();
                if (fy instanceof Number ny) StoneworksChatClient.hudPosYFrac = ny.floatValue();
                if (anchorX instanceof String ax) {
                    try { StoneworksChatClient.hudAnchorX = StoneworksChatClient.AnchorX.valueOf(ax); } catch (Exception ignored) {}
                }
                if (anchorY instanceof String ay) {
                    try { StoneworksChatClient.hudAnchorY = StoneworksChatClient.AnchorY.valueOf(ay); } catch (Exception ignored) {}
                }
                if (offX instanceof Number ox) StoneworksChatClient.hudOffsetX = ox.intValue();
                if (offY instanceof Number oy) StoneworksChatClient.hudOffsetY = oy.intValue();
                if (align instanceof String s) {
                    if ("rtl".equalsIgnoreCase(s)) {
                        StoneworksChatClient.hudTextAlign = StoneworksChatClient.TextAlign.RIGHT_TO_LEFT;
                    } else if ("center".equalsIgnoreCase(s)) {
                        StoneworksChatClient.hudTextAlign = StoneworksChatClient.TextAlign.CENTER;
                    } else {
                        StoneworksChatClient.hudTextAlign = StoneworksChatClient.TextAlign.LEFT_TO_RIGHT;
                    }
                }
        LOGGER.info("Loaded config: currentChannel = {}", StoneworksChatClient.currentChannel);
            } catch (IOException e) {
                LOGGER.error("Failed to load config {}", CONFIG_FILE.getAbsolutePath(), e);
            }
        } else {
            StoneworksChatClient.channels = new HashMap<>();
        }
    ensureDefaultChannels();
    }

    public static void save() {
        Map<String, Object> config = new HashMap<>();
        config.put("currentChannel", StoneworksChatClient.currentChannel);
        config.put("channels", StoneworksChatClient.channels);
        config.put("hudX", StoneworksChatClient.hudPosX);
        config.put("hudY", StoneworksChatClient.hudPosY);
    String align = switch (StoneworksChatClient.hudTextAlign) {
            case LEFT_TO_RIGHT -> "ltr";
            case CENTER -> "center";
            case RIGHT_TO_LEFT -> "rtl";
        };
        config.put("textAlign", align);
    if (StoneworksChatClient.hudPosXFrac >= 0f) config.put("hudXFrac", StoneworksChatClient.hudPosXFrac);
    if (StoneworksChatClient.hudPosYFrac >= 0f) config.put("hudYFrac", StoneworksChatClient.hudPosYFrac);
        if (StoneworksChatClient.hudAnchorX != null) {
            config.put("hudAnchorX", StoneworksChatClient.hudAnchorX.name());
        }
        if (StoneworksChatClient.hudAnchorY != null) {
            config.put("hudAnchorY", StoneworksChatClient.hudAnchorY.name());
        }
    config.put("hudOffsetX", StoneworksChatClient.hudOffsetX);
    config.put("hudOffsetY", StoneworksChatClient.hudOffsetY);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            LOGGER.info("Saved config: currentChannel = {}", StoneworksChatClient.currentChannel);
        } catch (IOException e) {
            LOGGER.error("Failed to save config {}", CONFIG_FILE.getAbsolutePath(), e);
        }
    }

    private static void ensureDefaultChannels() {
        addDefaultChannel("public", "Global", "white", new String[]{"/g", "/global"});
        addDefaultChannel("LocalChat", "Local", "green", new String[]{"/l", "/local"});
        addDefaultChannel("TradeChat", "Trade", "cyan", new String[]{"/tradechat", "/tc"});
        addDefaultChannel("RPChat", "Roleplay", "light_red", new String[]{"/rpc"});
        //addDefaultChannel("StaffChat2", "Staff", "yellow", new String[]{"/staffc"});
        //addDefaultChannel("AdminChat", "Admin", "red", new String[]{"/adminc"});
    }

    private static void addDefaultChannel(String key, String display, String color, String[] aliases) {
        if (!StoneworksChatClient.channels.containsKey(key)) {
            Map<String, Object> map = new HashMap<>();
            map.put("display", display);
            map.put("color", color);
            map.put("aliases", aliases);
            StoneworksChatClient.channels.put(key, map);
        }
    }
} 