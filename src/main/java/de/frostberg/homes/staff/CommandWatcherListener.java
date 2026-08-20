package de.frostberg.homes.staff;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Sendet jeden von einem Spieler eingegebenen Befehl an alle Team-Mitglieder,
 * die den Command-Watcher-Feed per /cw eingeschaltet haben (siehe
 * CommandWatcherManager). MONITOR-Prioritaet und liest nur mit, greift nie
 * ein (kein setCancelled) - andere Plugins/Permissions entscheiden weiter
 * regulaer, ob der Befehl ueberhaupt ausgefuehrt wird.
 */
public class CommandWatcherListener implements Listener {

    private final FrostbergHomes plugin;

    public CommandWatcherListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player actor = event.getPlayer();
        // "/" abschneiden, wie es im Chat eingegeben wurde
        String command = event.getMessage().substring(1);

        String broadcast = MessageUtil.get(plugin.getMessages(), "cw-broadcast")
                .replace("%player%", actor.getName())
                .replace("%command%", command);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(actor)) {
                continue;
            }
            if (!viewer.hasPermission("cw.use") || !plugin.getCommandWatcherManager().isEnabled(viewer)) {
                continue;
            }
            viewer.sendMessage(broadcast);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCommandWatcherManager().remove(event.getPlayer().getUniqueId());
    }
}
