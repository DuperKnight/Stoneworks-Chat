package com.duperknight.stoneworksChat.client;

import com.duperknight.stoneworksChat.client.config.ChatConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class ClientCommands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("shc")
                    .then(literal("reset")
                        .executes(ctx -> {
                            boolean ok = ChatConfig.reset();
                            FabricClientCommandSource source = ctx.getSource();
                            if (source != null) {
                                if (ok) {
                                    source.sendFeedback(Text.literal("Stoneworks-Chat: Config reset and regenerated."));
                                } else {
                                    source.sendError(Text.literal("Stoneworks-Chat: Failed to reset config. Check logs."));
                                }
                            }
                            return 1;
                        })
                    )
                    .then(literal("reload")
                        .executes(ctx -> {
                            ChatConfig.load();
                            FabricClientCommandSource source = ctx.getSource();
                            if (source != null) {
                                source.sendFeedback(Text.literal("Stoneworks-Chat: Config reloaded."));
                            }
                            return 1;
                        })
                    )
            );
        });
    }
}


