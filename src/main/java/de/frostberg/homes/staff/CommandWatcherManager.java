package de.frostberg.homes.staff;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Haelt in Erinnerung, welche Team-Mitglieder den Command-Watcher-Live-Feed
 * gerade eingeschaltet haben (per /cw togglebar). Rein In-Memory, wie
 * VanishManager - kein Grund, das ueber Neustarts hinweg zu speichern.
 */
public class CommandWatcherManager {

    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();

    public boolean isEnabled(Player player) {
        return enabled.contains(player.getUniqueId());
    }

    /** Schaltet den Feed fuer den Spieler um, gibt den neuen Zustand zurueck. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (enabled.remove(id)) {
            return false;
        }
        enabled.add(id);
        return true;
    }

    public void remove(UUID uuid) {
        enabled.remove(uuid);
    }
}
