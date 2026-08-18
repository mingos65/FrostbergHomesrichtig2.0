package de.frostberg.homes.util;

import de.frostberg.homes.FrostbergHomes;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Stellt %frostbergcurrency_...%-Platzhalter mit deutscher Tausenderpunkt-
 * Formatierung bereit (z.B. "12.345"), fuer die TAB-Scoreboard-Anzeige.
 * Wird nur registriert, wenn PlaceholderAPI installiert ist (siehe
 * FrostbergHomes#onEnable).
 *
 * Verfuegbare Platzhalter:
 * %frostbergcurrency_tokens%          - Tokens-Kontostand, formatiert (z.B. "12.345")
 * %frostbergcurrency_gold%            - Gold-Kontostand, formatiert ohne Nachkommastellen
 */
public class CurrencyPlaceholderExpansion extends PlaceholderExpansion {

    private final FrostbergHomes plugin;
    private final DecimalFormat format;

    public CurrencyPlaceholderExpansion(FrostbergHomes plugin) {
        this.plugin = plugin;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.GERMANY);
        this.format = new DecimalFormat("#,##0", symbols);
    }

    @Override
    public String getIdentifier() {
        return "frostbergcurrency";
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
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        Player player = (Player) offlinePlayer;
        String key = params.toLowerCase(Locale.ROOT);

        return switch (key) {
            case "tokens" -> {
                long tokens = CurrencyBridge.readTokenBalance(player);
                yield tokens < 0 ? "0" : format.format(tokens);
            }
            case "gold" -> {
                double gold = CurrencyBridge.readGoldBalance(player);
                yield gold < 0 ? "0" : format.format(gold);
            }
            default -> "";
        };
    }
}
