package de.frostberg.homes.util;

import de.frostberg.homes.FrostbergHomes;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;

/**
 * Stellt den relationalen Platzhalter %rel_frostbergvanish_suffix% bereit:
 * haengt "&7(V)" an, aber NUR wenn der Betrachter (viewer) selbst
 * frostberg.vanish hat UND das Ziel (target) gerade vanished ist - normale
 * Spieler sehen also nie, dass jemand vanished ist, nur Team-Kollegen. Wird
 * nur registriert, wenn PlaceholderAPI installiert ist (siehe
 * FrostbergHomes#onEnable). Muss in plugins/TAB/config.yml manuell ins
 * Tabliste-/Nametag-Format eingebaut werden (server-seitig, nicht im Repo).
 */
public class VanishPlaceholderExpansion extends PlaceholderExpansion implements Relational {

    private final FrostbergHomes plugin;

    public VanishPlaceholderExpansion(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "frostbergvanish";
    }

    @Override
    public String getAuthor() {
        return "Frostberg";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String identifier) {
        if (!"suffix".equals(identifier)) {
            return "";
        }
        if (viewer == null || target == null) {
            return "";
        }
        if (!viewer.hasPermission("frostberg.vanish")) {
            return "";
        }
        if (!plugin.getVanishManager().isVanished(target)) {
            return "";
        }
        return MessageUtil.color(" &d(V)");
    }

    @Override
    public String onRequest(org.bukkit.OfflinePlayer player, String params) {
        return "";
    }
}
