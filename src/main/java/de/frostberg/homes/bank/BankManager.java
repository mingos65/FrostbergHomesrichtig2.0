package de.frostberg.homes.bank;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.CurrencyBridge;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Verwaltet ein zweites, getrenntes Guthaben pro Spieler (Bank-Konto), das
 * unabhaengig vom normalen Wallet (PlayerPoints/Vault) in bank.yml
 * gespeichert wird. Ein-/Auszahlen verschiebt tatsaechlich Betraege
 * zwischen Wallet und Bank ueber CurrencyBridge, faelscht also nirgends
 * Guthaben, sondern verschiebt es nur.
 */
public class BankManager {

    private final FrostbergHomes plugin;
    private final File file;
    private final FileConfiguration config;

    public BankManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bank.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Konnte bank.yml nicht anlegen: " + ex.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public long getBankTokens(UUID uuid) {
        return config.getLong(uuid + ".tokens", 0);
    }

    public double getBankGold(UUID uuid) {
        return config.getDouble(uuid + ".gold", 0);
    }

    public boolean isHidden(UUID uuid) {
        return config.getBoolean(uuid + ".hidden", false);
    }

    public void setHidden(UUID uuid, boolean hidden) {
        config.set(uuid + ".hidden", hidden);
        save();
    }

    /** Zahlt "amount" Tokens vom Wallet auf die Bank ein. Gibt den tatsaechlich eingezahlten Betrag zurueck (0 = nichts zu tun/fehlgeschlagen). */
    public long depositAllTokens(Player player) {
        long balance = CurrencyBridge.readTokenBalance(player);
        if (balance <= 0) {
            return 0;
        }
        if (!CurrencyBridge.takeTokens(player, balance)) {
            return 0;
        }
        setBankTokens(player.getUniqueId(), getBankTokens(player.getUniqueId()) + balance);
        return balance;
    }

    /** Zahlt das komplette Bank-Tokens-Guthaben zurueck ins Wallet aus. */
    public long withdrawAllTokens(Player player) {
        long stored = getBankTokens(player.getUniqueId());
        if (stored <= 0) {
            return 0;
        }
        if (!CurrencyBridge.giveTokens(player, stored)) {
            return 0;
        }
        setBankTokens(player.getUniqueId(), 0);
        return stored;
    }

    public double depositAllGold(Player player) {
        double balance = CurrencyBridge.readGoldBalance(player);
        if (balance <= 0) {
            return 0;
        }
        if (!CurrencyBridge.takeGold(player, balance)) {
            return 0;
        }
        setBankGold(player.getUniqueId(), getBankGold(player.getUniqueId()) + balance);
        return balance;
    }

    public double withdrawAllGold(Player player) {
        double stored = getBankGold(player.getUniqueId());
        if (stored <= 0) {
            return 0;
        }
        if (!CurrencyBridge.giveGold(player, stored)) {
            return 0;
        }
        setBankGold(player.getUniqueId(), 0);
        return stored;
    }

    private void setBankTokens(UUID uuid, long amount) {
        config.set(uuid + ".tokens", amount);
        save();
    }

    private void setBankGold(UUID uuid, double amount) {
        config.set(uuid + ".gold", amount);
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte bank.yml nicht speichern: " + ex.getMessage());
        }
    }
}
