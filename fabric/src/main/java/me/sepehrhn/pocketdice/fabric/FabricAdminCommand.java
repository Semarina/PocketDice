package me.sepehrhn.pocketdice.fabric;

import com.mojang.brigadier.CommandDispatcher;
import me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.ServerCommandSource;
import java.util.stream.Stream;

public class FabricAdminCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var adminNode = dispatcher.register(CommandManager.literal("pocketdice")
            .executes(context -> {
                String localeCode = "en_US";
                if (context.getSource().isExecutedByPlayer() && context.getSource().getPlayer() != null) {
                    localeCode = context.getSource().getPlayer().getClientOptions().language();
                }
                context.getSource().sendMessage(FabricLocaleManager.getText(context.getSource().getRegistryManager(), localeCode, "messages.command.pocketdice_help"));
                return 1;
            })
            .then(CommandManager.literal("help")
                .executes(context -> {
                    String localeCode = "en_US";
                    if (context.getSource().isExecutedByPlayer() && context.getSource().getPlayer() != null) {
                        localeCode = context.getSource().getPlayer().getClientOptions().language();
                    }
                    context.getSource().sendMessage(FabricLocaleManager.getText(context.getSource().getRegistryManager(), localeCode, "messages.command.pocketdice_help"));
                    return 1;
                })
            )
            .then(CommandManager.literal("version")
                .executes(context -> {
                    String localeCode = "en_US";
                    if (context.getSource().isExecutedByPlayer() && context.getSource().getPlayer() != null) {
                        localeCode = context.getSource().getPlayer().getClientOptions().language();
                    }
                    String version = "Unknown";
                    var modContainer = FabricLoader.getInstance().getModContainer("pocketdice");
                    if (modContainer.isPresent()) {
                        version = modContainer.get().getMetadata().getVersion().getFriendlyString();
                    }
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("version", version);
                    context.getSource().sendMessage(FabricLocaleManager.getText(context.getSource().getRegistryManager(), localeCode, "messages.command.pocketdice_version", map));
                    return 1;
                })
            )
            .then(CommandManager.literal("reload")
                .executes(context -> {
                    String localeCode = "en_US";
                    if (context.getSource().isExecutedByPlayer() && context.getSource().getPlayer() != null) {
                        localeCode = context.getSource().getPlayer().getClientOptions().language();
                        var player = context.getSource().getPlayer();
                        var server = context.getSource().getServer();
                        String playerName = player.getName().getString();
                        
                        boolean isOp = Stream.of(server.getPlayerManager().getOpNames()).anyMatch(name -> name.equalsIgnoreCase(playerName));
                        boolean isSinglePlayer = server.isSingleplayer();
                        
                        if (!isOp && !isSinglePlayer) {
                             context.getSource().sendError(FabricLocaleManager.getText(context.getSource().getRegistryManager(), localeCode, "messages.command.no_permission"));
                             return 0;
                        }
                    }
                    try {
                        FabricConfig.load();
                        FabricLocaleManager.reload();
                        PocketDiceFabric.UPDATE_CHECKER.shutdown();
                        PocketDiceFabric.UPDATE_CHECKER.start();
                        context.getSource().sendMessage(FabricLocaleManager.getText(context.getSource().getRegistryManager(), localeCode, "messages.command.reload_success"));
                    } catch (Exception e) {
                        PocketDiceFabric.debug("Failed to reload Fabric environment: " + e.getMessage());
                        context.getSource().sendError(FabricLocaleManager.getText(context.getSource().getRegistryManager(), localeCode, "messages.command.reload_failure"));
                    }
                    return 1;
                })
            )
        );

        dispatcher.register(CommandManager.literal("pd").redirect(adminNode));
    }
}
