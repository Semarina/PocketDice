package me.sepehrhn.pocketdice.update;

import me.sepehrhn.pocketdice.PocketDice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Notifies admins on join when an update is available. */
public class UpdateNotifyListener implements Listener {

    private final PocketDice plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public UpdateNotifyListener(PocketDice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UpdateChecker checker = plugin.getUpdateChecker();
        if (checker == null || !checker.isEnabled() || !checker.isNotifyAdminsOnJoin()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission(checker.getAdminNotifyPermission())) return;

        UpdateCheckResult result = checker.getLastResult();
        if (result == null || result.getStatus() != UpdateCheckStatus.UPDATE_AVAILABLE) return;

        String template = checker.getAdminMessageTemplate();
        if (template == null || template.isBlank()) return;

        String message = checker.applyPlaceholders(template, result);
        if (message == null || message.isBlank()) return;

        try {
            player.sendMessage(miniMessage.deserialize(message));
        } catch (Exception ex) {
            player.sendMessage(message);
        }
    }
}
