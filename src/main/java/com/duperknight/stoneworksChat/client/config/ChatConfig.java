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
                StoneworksChatClient.currentChannel = (String) config.getOrDefault("currentChannel", "public");
                StoneworksChatClient.channels = (Map<String, Map<String, Object>>) config.getOrDefault("channels", new HashMap<>());
                LOGGER.info("Loaded config: currentChannel = {}", StoneworksChatClient.currentChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Default channels
            Map<String, Object> publicChat = new HashMap<>();
            publicChat.put("display", "Global");
            publicChat.put("color", "white");
            publicChat.put("aliases", new String[]{"/g", "/global"});
            publicChat.put("uiName", "Global Chat");

            Map<String, Object> localChat = new HashMap<>();
            localChat.put("display", "Local");
            localChat.put("color", "green");
            localChat.put("aliases", new String[]{"/l", "/local"});
            localChat.put("uiName", "Local Chat");

            Map<String, Object> rpChat = new HashMap<>();
            rpChat.put("display", "Role-play");
            rpChat.put("color", "light_red");
            rpChat.put("aliases", new String[]{"/rpc"});
            rpChat.put("uiName", "RP Chat");

            Map<String, Object> tradeChat = new HashMap<>();
            tradeChat.put("display", "Trade");
            tradeChat.put("color", "cyan");
            tradeChat.put("aliases", new String[]{"/tradechat", "/tc"});
            tradeChat.put("uiName", "Trade Chat");

            StoneworksChatClient.channels.put("public", publicChat);
            StoneworksChatClient.channels.put("LocalChat", localChat);
            StoneworksChatClient.channels.put("RPChat", rpChat);
            StoneworksChatClient.channels.put("TradeChat", tradeChat);

            save();
            LOGGER.info("Created default config");
        }
    }

    public static void save() {
        Map<String, Object> config = new HashMap<>();
        config.put("currentChannel", StoneworksChatClient.currentChannel);
        config.put("channels", StoneworksChatClient.channels);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            LOGGER.info("Saved config: currentChannel = {}", StoneworksChatClient.currentChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 