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

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            LOGGER.info("Saved config: currentChannel = {}", StoneworksChatClient.currentChannel);
        } catch (IOException e) {
            LOGGER.error("Failed to save config {}", CONFIG_FILE.getAbsolutePath(), e);
        }
    }

    private static void ensureDefaultChannels() {
        addDefaultChannel("public", "Global", "white", new String[]{"/g", "/global"}, "Global Chat");
        addDefaultChannel("LocalChat", "Local", "green", new String[]{"/l", "/local"}, "Local Chat");
        addDefaultChannel("TradeChat", "Trade", "cyan", new String[]{"/tradechat", "/tc"}, "Trade Chat");
        addDefaultChannel("RPChat", "Roleplay", "light_red", new String[]{"/rpc"}, "RP Chat");
        addDefaultChannel("StaffChat2", "Staff", "yellow", new String[]{"/staffc"}, "Staff Chat");
        addDefaultChannel("AdminChat", "Admin", "red", new String[]{"/adminc"}, "Admin Chat");
    }

    private static void addDefaultChannel(String key, String display, String color, String[] aliases, String uiName) {
        if (!StoneworksChatClient.channels.containsKey(key)) {
            Map<String, Object> map = new HashMap<>();
            map.put("display", display);
            map.put("color", color);
            map.put("aliases", aliases);
            map.put("uiName", uiName);
            StoneworksChatClient.channels.put(key, map);
        }
    }
} 