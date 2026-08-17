package de.frostberg.homes.staff;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Wendet Vanish-Sichtbarkeit auf neu joinende Spieler an und unterdrueckt
 * Join-/Quit-Broadcasts fuer aktuell vanishte Spieler.
 */
public class VanishListener implements Listener {

    private final FrostbergHomes plugin;

    public VanishListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Bereits vanishte Spieler (z.B. weiterhin online seit vorherigem
        // Toggle) vor dem neuen Spieler verstecken.
        plugin.getVanishManager().applyVisibilityFor(player);

        if (plugin.getVanishManager().isVanished(player)) {
            event.setJoinMessage(null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getVanishManager().isVanished(player)) {
            event.setQuitMessage(null);
        }
        plugin.getVanishManager().remove(player.getUniqueId());
    }
}
