package de.frostberg.homes.handel;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Raeumt offene (noch nicht angenommene) Handelsanfragen auf, wenn ein Beteiligter den Server verlaesst. Laufende Sessions werden bereits ueber InventoryCloseEvent in HandelGuiListener abgefangen. */
public class HandelListener implements Listener {

    private final FrostbergHomes plugin;

    public HandelListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTradeManager().cancelPending(event.getPlayer().getUniqueId());
    }
}
