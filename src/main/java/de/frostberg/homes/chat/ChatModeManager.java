package de.frostberg.homes.chat;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Haelt fest, ob ein Spieler seinen Chat gerade in den Team- oder
 * Admin-Chat-Modus umgeschaltet hat (/tc, /ac). Nur einer der beiden Modi
 * gleichzeitig - Umschalten auf den einen deaktiviert automatisch den
 * anderen, da hier nur ein einzelnes Feld pro Spieler gesetzt wird.
 */
public class ChatModeManager {

    public enum Mode {
        NONE, TEAM, ADMIN
    }

    private final Map<UUID, Mode> modes = new ConcurrentHashMap<>();

    public Mode getMode(Player player) {
        return modes.getOrDefault(player.getUniqueId(), Mode.NONE);
    }

    public Mode toggleTeam(Player player) {
        Mode next = getMode(player) == Mode.TEAM ? Mode.NONE : Mode.TEAM;
        modes.put(player.getUniqueId(), next);
        return next;
    }

    public Mode toggleAdmin(Player player) {
        Mode next = getMode(player) == Mode.ADMIN ? Mode.NONE : Mode.ADMIN;
        modes.put(player.getUniqueId(), next);
        return next;
    }

    public void remove(UUID uuid) {
        modes.remove(uuid);
    }
}
