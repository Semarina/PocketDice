package me.sepehrhn.pocketdice.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import me.sepehrhn.pocketdice.update.UpdateCheckResult;
import me.sepehrhn.pocketdice.update.UpdateCheckStatus;

public class PocketDiceFabric implements ModInitializer {

    public static final me.sepehrhn.pocketdice.fabric.update.FabricUpdateChecker UPDATE_CHECKER = new me.sepehrhn.pocketdice.fabric.update.FabricUpdateChecker();
    public static final me.sepehrhn.pocketdice.util.AntiSpamService ANTI_SPAM = new me.sepehrhn.pocketdice.util.AntiSpamService();

    public static void debug(String message) {
        if (FabricConfig.DATA.debug) {
            System.out.println("[PocketDice DEBUG] " + message);
        }
    }

    @Override
    public void onInitialize() {
        FabricConfig.load();
        me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.reload();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FabricRollCommand.register(dispatcher);
            FabricAdminCommand.register(dispatcher);
        });

        UPDATE_CHECKER.start();
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (FabricConfig.DATA.updates.enabled && FabricConfig.DATA.updates.notify_admins_on_join) {
                UpdateCheckResult result = UPDATE_CHECKER.getLastResult();
                if (result != null && result.getStatus() == UpdateCheckStatus.UPDATE_AVAILABLE) {
                    ServerPlayer player = handler.player;
                    String playerName = player.getName().getString();
                    boolean isOp = java.util.stream.Stream.of(server.getPlayerList().getOpNames()).anyMatch(name -> name.equalsIgnoreCase(playerName));
                    if (isOp) {
                        String url = result.getUrl() != null && !result.getUrl().isBlank() ? result.getUrl() : "https://modrinth.com/plugin/pocketdice";
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("current", result.getCurrentVersion());
                        placeholders.put("latest", result.getLatestVersion());
                        placeholders.put("url", url);
                        
                        String localeCode = player.clientInformation().language();
                        net.minecraft.network.chat.Component message = me.sepehrhn.pocketdice.fabric.locale.FabricLocaleManager.getText(server.registryAccess(), localeCode, "messages.update.available_admin", placeholders);
                        player.sendSystemMessage(message);
                    }
                }
            }
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
             UPDATE_CHECKER.shutdown();
        });
    }
}
