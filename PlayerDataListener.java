package de.frostberg.homes.listener;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Laedt die Homes eines Spielers beim Join in den Cache des HomeManagers und
 * entfernt sie beim Quit wieder daraus. Die Datei ist zu dem Zeitpunkt immer
 * aktuell, da HomeManager bei jeder Aenderung (set/delete) sofort speichert.
 */
public class PlayerDataListener implements Listener {

    private final FrostbergHomes plugin;

    public PlayerDataListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getHomeManager().loadHomes(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getHomeManager().unloadHomes(player.getUniqueId());
    }
}
