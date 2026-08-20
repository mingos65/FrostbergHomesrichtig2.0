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

    /** Zahlt genau "amount" Tokens vom Wallet auf die Bank ein. Liefert false bei ungueltigem Betrag/zu wenig Wallet-Guthaben. */
    public boolean depositTokens(Player player, long amount) {
        if (amount <= 0) {
            return false;
        }
        long balance = CurrencyBridge.readTokenBalance(player);
        if (balance < amount) {
            return false;
        }
        if (!CurrencyBridge.takeTokens(player, amount)) {
            return false;
        }
        setBankTokens(player.getUniqueId(), getBankTokens(player.getUniqueId()) + amount);
        return true;
    }

    /** Hebt genau "amount" Tokens von der Bank ab und zahlt sie ins Wallet aus. */
    public boolean withdrawTokens(Player player, long amount) {
        if (amount <= 0) {
            return false;
        }
        long stored = getBankTokens(player.getUniqueId());
        if (stored < amount) {
            return false;
        }
        if (!CurrencyBridge.giveTokens(player, amount)) {
            return false;
        }
        setBankTokens(player.getUniqueId(), stored - amount);
        return true;
    }

    public boolean depositGold(Player player, double amount) {
        if (amount <= 0) {
            return false;
        }
        double balance = CurrencyBridge.readGoldBalance(player);
        if (balance < amount) {
            return false;
        }
        if (!CurrencyBridge.takeGold(player, amount)) {
            return false;
        }
        setBankGold(player.getUniqueId(), getBankGold(player.getUniqueId()) + amount);
        return true;
    }

    public boolean withdrawGold(Player player, double amount) {
        if (amount <= 0) {
            return false;
        }
        double stored = getBankGold(player.getUniqueId());
        if (stored < amount) {
            return false;
        }
        if (!CurrencyBridge.giveGold(player, amount)) {
            return false;
        }
        setBankGold(player.getUniqueId(), stored - amount);
        return true;
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
