package de.frostberg.homes.stats;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Startet/beendet die Spielzeit-Session eines Spielers bei Join/Quit. */
public class PlaytimeListener implements Listener {

    private final FrostbergHomes plugin;

    public PlaytimeListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlaytimeManager().startSession(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlaytimeManager().endSession(event.getPlayer());
    }
}
