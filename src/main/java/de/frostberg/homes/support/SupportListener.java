package de.frostberg.homes.support;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Raeumt offene/uebernommene Support-Tickets auf, wenn ein Beteiligter den Server verlaesst. */
public class SupportListener implements Listener {

    private final FrostbergHomes plugin;

    public SupportListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getSupportManager().removePlayer(event.getPlayer().getUniqueId());
    }
}
