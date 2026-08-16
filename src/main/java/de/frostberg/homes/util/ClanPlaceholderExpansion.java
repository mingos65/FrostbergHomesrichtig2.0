package de.frostberg.homes.util;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.clan.model.Clan;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * Stellt %frostbergclans_...%-Platzhalter fuer TAB, Scoreboard und Chat-
 * Plugins (z.B. FancyChat) bereit. Wird nur registriert, wenn
 * PlaceholderAPI installiert ist (siehe FrostbergHomes#onEnable). Ersetzt
 * den bisher unaufgeloesten %betterteams_name%-Platzhalter in der
 * TAB-Config - die Umstellung dort ist reine Server-Config, kein Code.
 *
 * Verfuegbare Platzhalter:
 * %frostbergclans_name%        - Clan-Name oder leer
 * %frostbergclans_tag%         - Clan-Tag oder leer
 * %frostbergclans_role%        - LEADER/MOD/MEMBER oder leer
 * %frostbergclans_membercount% - Mitgliederanzahl oder leer
 */
public class ClanPlaceholderExpansion extends PlaceholderExpansion {

    private final FrostbergHomes plugin;

    public ClanPlaceholderExpansion(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "frostbergclans";
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
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }

        Clan clan = plugin.getClanManager().getClanOf(player.getUniqueId()).orElse(null);
        if (clan == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "name" -> clan.getName();
            case "tag" -> clan.getTag() != null ? clan.getTag() : "";
            case "role" -> {
                Clan.Role role = clan.getRole(player.getUniqueId());
                yield role != null ? role.name() : "";
            }
            case "membercount" -> String.valueOf(clan.getMemberCount());
            default -> "";
        };
    }
}
