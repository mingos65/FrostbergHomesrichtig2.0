package de.frostberg.homes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Kleines Hilfswerkzeug fuer Chat-Nachrichten: wandelt &-Farbcodes um und
 * holt Texte aus der messages.yml (uebergeben als FileConfiguration, siehe
 * FrostbergHomes#getMessages), inklusive Ersetzung von %prefix%. Alle
 * weiteren Platzhalter (%nr%, %world%, ...) ersetzen die aufrufenden
 * Commands selbst per String.replace(...), da nur sie den Kontext dafuer
 * kennen.
 */
public final class MessageUtil {

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

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
     * Holt eine Nachricht aus der messages.yml, ersetzt %prefix% durch den
     * dortigen "prefix"-Eintrag und faerbt das Ergebnis ein. Fehlt der
     * Eintrag, wird ein auffaelliger Platzhalter zurueckgegeben, damit
     * fehlerhafte Configs sofort im Chat auffallen statt still zu versagen.
     */
    public static String get(FileConfiguration messages, String path) {
        String raw = messages.getString(path);
        if (raw == null) {
            return color("&c[Fehlende Nachricht: " + path + "]");
        }

        String prefix = messages.getString("prefix", "");
        raw = raw.replace("%prefix%", prefix);

        return color(raw);
    }

    /**
     * Wandelt einen bereits eingefaerbten Text (z.B. das Ergebnis von get())
     * in eine Adventure-Component um, damit Klick-/Hover-Events angehaengt
     * werden koennen (siehe TpaCommand fuer die [Annehmen]/[Ablehnen]-Buttons).
     */
    public static Component toComponent(String coloredText) {
        return LEGACY_SECTION.deserialize(coloredText == null ? "" : coloredText);
    }
}
