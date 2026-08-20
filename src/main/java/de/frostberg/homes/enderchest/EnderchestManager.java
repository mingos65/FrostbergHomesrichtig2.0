package de.frostberg.homes.enderchest;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Eigene, vom vanilla Enderchest komplett unabhaengige Mehrseiten-
 * Enderchest pro Spieler, gespeichert in enderchest.yml. Jede Seite hat
 * 27 Slots und einen eigenen (umbenennbaren) Namen. Seitenlimit analog zu
 * HomeManager#getHomeLimit ueber ec.pages.&lt;n&gt;-Permissions.
 */
public class EnderchestManager {

    public static final int PAGE_SIZE = 27;
    private static final int MAX_PAGES = 9;

    private final FrostbergHomes plugin;
    private final File file;
    private final FileConfiguration config;

    public EnderchestManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "enderchest.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Konnte enderchest.yml nicht anlegen: " + ex.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /** Wie viele Seiten (1-MAX_PAGES) der Spieler nutzen darf. */
    public int getPageLimit(Player player) {
        int highest = -1;
        for (int i = 1; i <= MAX_PAGES; i++) {
            if (player.hasPermission("ec.pages." + i)) {
                highest = Math.max(highest, i);
            }
        }
        if (highest != -1) {
            return highest;
        }
        return Math.max(1, Math.min(MAX_PAGES, plugin.getConfig().getInt("settings.default-ec-pages", 1)));
    }

    public String getPageName(UUID uuid, int page) {
        return config.getString(uuid + ".pages." + page + ".name", "Seite " + (page + 1));
    }

    public void setPageName(UUID uuid, int page, String name) {
        config.set(uuid + ".pages." + page + ".name", name);
        save();
    }

    public ItemStack[] loadPage(UUID uuid, int page) {
        ItemStack[] contents = new ItemStack[PAGE_SIZE];
        ConfigurationSection section = config.getConfigurationSection(uuid + ".pages." + page + ".items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int index = Integer.parseInt(key);
                    if (index >= 0 && index < PAGE_SIZE) {
                        contents[index] = section.getItemStack(key);
                    }
                } catch (NumberFormatException ignored) {
                    // Kein gueltiger Slot-Index, ueberspringen
                }
            }
        }
        return contents;
    }

    public void savePage(UUID uuid, int page, ItemStack[] contents) {
        String base = uuid + ".pages." + page + ".items";
        config.set(base, null);
        for (int i = 0; i < contents.length && i < PAGE_SIZE; i++) {
            if (contents[i] != null) {
                config.set(base + "." + i, contents[i]);
            }
        }
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte enderchest.yml nicht speichern: " + ex.getMessage());
        }
    }
}
