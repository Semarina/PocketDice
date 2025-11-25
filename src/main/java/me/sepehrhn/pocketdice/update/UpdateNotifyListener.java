package me.sepehrhn.pocketdice.update;

import me.sepehrhn.pocketdice.PocketDice;
import me.sepehrhn.pocketdice.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

/** Notifies admins on join when an update is available. */
public class UpdateNotifyListener implements Listener {

    private final PocketDice plugin;

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

        String fallbackUrl = result.getUrl() != null && !result.getUrl().isBlank()
                ? result.getUrl()
                : "https://modrinth.com/plugin/pocketdice";
        String message = plugin.getLocaleManager().get(player, "messages.update.available_admin", Map.of(
                "current", result.getCurrentVersion(),
                "latest", result.getLatestVersion(),
                "url", fallbackUrl
        ));
        if (message == null || message.isBlank()) return;
        player.sendMessage(Text.toComponent(message));
    }
}
