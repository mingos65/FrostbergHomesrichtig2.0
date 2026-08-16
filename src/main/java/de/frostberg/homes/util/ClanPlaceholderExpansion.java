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
 * %frostbergclans_name%        - Clan-Name, ohne Clan messages.clan-placeholder-none ("Kein Clan")
 * %frostbergclans_tag%         - Clan-Tag, ohne Clan leer
 * %frostbergclans_role%        - LEADER/MOD/MEMBER, ohne Clan leer
 * %frostbergclans_membercount% - Mitgliederanzahl, ohne Clan leer
 * %frostbergclans_tagdisplay%  - Fertig formatiertes Tag inkl. Klammern
 *                                 (messages.clan-placeholder-tag-format),
 *                                 ohne Clan komplett leer (kein "[]") -
 *                                 gedacht zum direkten Einsetzen in TAB-
 *                                 Tabliste/Chat-Format, ohne eigene Klammern
 *                                 dort zu brauchen
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
            return "name".equalsIgnoreCase(params) ? MessageUtil.get(plugin.getMessages(), "clan-placeholder-none") : "";
        }

        return switch (params.toLowerCase()) {
            case "name" -> clan.getName();
            case "tag" -> clan.getTag() != null ? clan.getTag() : "";
            case "role" -> {
                Clan.Role role = clan.getRole(player.getUniqueId());
                yield role != null ? role.name() : "";
            }
            case "membercount" -> String.valueOf(clan.getMemberCount());
            case "tagdisplay" -> clan.getTag() != null
                    ? MessageUtil.get(plugin.getMessages(), "clan-placeholder-tag-format").replace("%tag%", clan.getTag())
                    : "";
            default -> "";
        };
    }
}
