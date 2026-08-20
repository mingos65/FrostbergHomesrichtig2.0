package de.frostberg.homes.rucksack;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Einseitiger Extra-Rucksack pro Spieler, gespeichert in rucksack.yml -
 * gleiches Speicherprinzip wie EnderchestManager (nur pro Slot-Index statt
 * als Liste, damit Luecken durch leere Slots keine Probleme machen), aber
 * ohne Mehrseiten-Verwaltung.
 */
public class RucksackManager {

    private final FrostbergHomes plugin;
    private final File file;
    private final FileConfiguration config;

    public RucksackManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rucksack.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Konnte rucksack.yml nicht anlegen: " + ex.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public int getSize() {
        int size = plugin.getConfig().getInt("settings.rucksack-size", 54);
        // Muss ein Vielfaches von 9 zwischen 9 und 54 sein (Bukkit-Inventar-Beschraenkung)
        size = Math.max(9, Math.min(54, size));
        return (size / 9) * 9;
    }

    public ItemStack[] load(UUID uuid) {
        ItemStack[] contents = new ItemStack[getSize()];
        ConfigurationSection section = config.getConfigurationSection(uuid + ".items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int index = Integer.parseInt(key);
                    if (index >= 0 && index < contents.length) {
                        contents[index] = section.getItemStack(key);
                    }
                } catch (NumberFormatException ignored) {
                    // Kein gueltiger Slot-Index, ueberspringen
                }
            }
        }
        return contents;
    }

    public void save(UUID uuid, ItemStack[] contents) {
        String base = uuid + ".items";
        config.set(base, null);
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                config.set(base + "." + i, contents[i]);
            }
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte rucksack.yml nicht speichern: " + ex.getMessage());
        }
    }
}
