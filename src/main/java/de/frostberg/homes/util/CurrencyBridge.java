package de.frostberg.homes.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Buendelt die Tokens-/Gold-Bruecken (gleiche Technik wie PayCommand und
 * ClanCommand#handleBankTokens/-Gold) an einer Stelle, damit QuestManager
 * Belohnungen auszahlen kann, ohne die Logik ein drittes Mal zu duplizieren.
 * Tokens laufen ueber PlayerPoints' eigenen Admin-Befehl per Konsole (keine
 * eigene Maven-Abhaengigkeit noetig), Gold direkt ueber die Vault Economy-API.
 */
public final class CurrencyBridge {

    private CurrencyBridge() {
    }

    public static boolean giveTokens(Player player, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            return false;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tokens give " + player.getName() + " " + amount);
        return true;
    }

    public static boolean takeTokens(Player player, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            return false;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tokens take " + player.getName() + " " + amount);
        return true;
    }

    public static boolean giveGold(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        return rsp.getProvider().depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Liest den aktuellen Tokens-Kontostand ueber PlaceholderAPI aus (keine
     * eigene PlayerPoints-API-Abhaengigkeit). Gibt -1 zurueck, wenn
     * PlaceholderAPI oder die PlayerPoints-Erweiterung nicht verfuegbar ist.
     */
    public static long readTokenBalance(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return -1;
        }
        try {
            String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%playerpoints_points%");
            if (raw == null) {
                return -1;
            }
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** Liest den aktuellen Gold-Kontostand direkt ueber die Vault Economy-API. Gibt -1 zurueck, wenn Vault fehlt. */
    public static double readGoldBalance(Player player) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return -1;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return -1;
        }
        return rsp.getProvider().getBalance(player);
    }
}
