package de.frostberg.homes.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Kleines Hilfswerkzeug fuer Chat-Nachrichten: wandelt &-Farbcodes um und
 * holt Texte aus messages.&lt;path&gt; in der config.yml, inklusive Ersetzung
 * von %prefix%. Alle weiteren Platzhalter (%nr%, %world%, ...) ersetzen die
 * aufrufenden Commands selbst per String.replace(...), da nur sie den Kontext
 * dafuer kennen.
 */
public final class MessageUtil {

    private MessageUtil() {
    }

    /** Wandelt &-Farbcodes (z.B. &a, &l, &8) in echte Minecraft-Formatierung um. */
    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * Holt eine Nachricht aus messages.&lt;path&gt;, ersetzt %prefix% durch
     * settings.prefix und faerbt das Ergebnis ein. Fehlt der Eintrag in der
     * config.yml, wird ein auffaelliger Platzhalter zurueckgegeben, damit
     * fehlerhafte Configs sofort im Chat auffallen statt still zu versagen.
     */
    public static String get(FileConfiguration config, String path) {
        String raw = config.getString("messages." + path);
        if (raw == null) {
            return color("&c[Fehlende Nachricht: messages." + path + "]");
        }

        String prefix = config.getString("settings.prefix", "");
        raw = raw.replace("%prefix%", prefix);

        return color(raw);
    }
}
