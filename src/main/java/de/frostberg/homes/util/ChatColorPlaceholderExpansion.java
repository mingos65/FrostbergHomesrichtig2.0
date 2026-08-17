package de.frostberg.homes.util;

import de.frostberg.homes.FrostbergHomes;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Stellt %frostbergchat_color% bereit - die per /chatcolor gewaehlte Farbe
 * eines Spielers, fertig als Farbcode-Praefix (kein Text drumherum). Gedacht
 * zum direkten Einsetzen kurz vor %message% im FancyChat-Format-String.
 *
 * Verlauf-Farben liefern hier bewusst "" (leer) zurueck - die koennen nicht
 * als einzelner Praefix-Code dargestellt werden (brauchen Zugriff auf den
 * Nachrichtentext selbst, um jedes Zeichen einzeln einzufaerben) und werden
 * stattdessen von chat/ChatFormatListener direkt im Nachrichtentext
 * angewendet, bevor FancyChat die Nachricht ueberhaupt sieht.
 */
public class ChatColorPlaceholderExpansion extends PlaceholderExpansion {

    private final FrostbergHomes plugin;

    public ChatColorPlaceholderExpansion(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "frostbergchat";
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
        if (player == null || !"color".equalsIgnoreCase(params)) {
            return "";
        }
        Player online = player.getPlayer();
        if (online == null) {
            return "";
        }

        boolean bold = plugin.getChatColorManager().isBold(online.getUniqueId()) && online.hasPermission("frostbergchat.color.bold");
        String code = plugin.getChatColorManager().getColor(online.getUniqueId());

        if (code == null) {
            return bold ? "§l" : "";
        }
        if (code.startsWith("gradient:")) {
            return bold ? "§l" : ""; // Verlauf selbst kommt ueber den Chat-Listener
        }
        if (code.startsWith("&#") && !online.hasPermission("frostbergchat.color.rgb")) {
            code = "&b"; // Permission zwischenzeitlich entzogen - sicherer Rueckfall
        }

        String colorCode = ColorUtil.applyColorCode(code, "");
        return (bold ? "§l" : "") + colorCode;
    }
}
