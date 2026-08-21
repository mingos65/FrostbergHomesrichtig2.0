package de.frostberg.homes.handel;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Raeumt offene (noch nicht angenommene) Handelsanfragen auf, wenn ein Beteiligter den Server
 * verlaesst. Laufende Sessions werden meist schon ueber InventoryCloseEvent in HandelGuiListener
 * abgefangen - nur falls jemand ausgerechnet waehrend einer offenen Chat-Betragseingabe (Fenster
 * also bewusst geschlossen) den Server verlaesst, greift stattdessen dieser Handler.
 */
public class HandelListener implements Listener {

    private final FrostbergHomes plugin;

    public HandelListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTradeManager().cancelPending(event.getPlayer().getUniqueId());
        plugin.getHandelGuiListener().handleQuit(event.getPlayer().getUniqueId());
    }
}
