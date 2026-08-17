package de.frostberg.homes.staff;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rein speicherbasierte Vanish-Verwaltung (kein Neustart-Ueberdauern noetig -
 * Staff schaltet das bei Bedarf einfach wieder ein). Nutzt
 * Player#hidePlayer/showPlayer(Plugin, Player), damit vanishte Spieler fuer
 * alle ohne frostberg.vanish unsichtbar sind (auch in der Tabliste), Spieler
 * MIT dieser Permission sehen sie weiterhin.
 */
public class VanishManager {

    private final FrostbergHomes plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    /** Schaltet den Vanish-Status um, gibt den neuen Zustand zurueck. */
    public boolean toggle(Player player) {
        boolean newValue = !isVanished(player);
        setVanished(player, newValue);
        return newValue;
    }

    public void setVanished(Player player, boolean value) {
        UUID uuid = player.getUniqueId();
        if (value) {
            vanished.add(uuid);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) && !online.hasPermission("frostberg.vanish")) {
                    online.hidePlayer(plugin, player);
                }
            }
        } else {
            vanished.remove(uuid);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
        }
    }

    /**
     * Beim Join eines Spielers: alle aktuell vanishten Spieler vor ihm
     * verstecken, ausser er darf sie selbst sehen (frostberg.vanish).
     */
    public void applyVisibilityFor(Player joining) {
        if (joining.hasPermission("frostberg.vanish")) {
            return;
        }
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null) {
                joining.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    public void remove(UUID uuid) {
        vanished.remove(uuid);
    }
}
