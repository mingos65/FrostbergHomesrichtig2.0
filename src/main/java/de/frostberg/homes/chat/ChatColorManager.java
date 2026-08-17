package de.frostberg.homes.chat;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Speichert die individuelle Chat-Farbe (Legacy-Code/Hex/Verlauf, siehe
 * util/ColorUtil) und Fett-Einstellung jedes Spielers in
 * plugins/FrostbergHomes/chatcolors.yml. Ganz normale Datei-basierte
 * Verwaltung analog zu HomeManager/ClanManager, nur ohne Cache-Entladen
 * beim Quit - die Datenmenge pro Spieler ist minimal.
 */
public class ChatColorManager {

    private final FrostbergHomes plugin;
    private final File file;
    private final Map<UUID, String> colors = new HashMap<>();
    private final Set<UUID> bold = new HashSet<>();

    public ChatColorManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chatcolors.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection colorsSection = yaml.getConfigurationSection("colors");
        if (colorsSection != null) {
            for (String key : colorsSection.getKeys(false)) {
                try {
                    colors.put(UUID.fromString(key), colorsSection.getString(key));
                } catch (IllegalArgumentException ignored) {
                    // ungueltiger UUID-Eintrag - ueberspringen
                }
            }
        }

        for (String key : yaml.getStringList("bold")) {
            try {
                bold.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                // ungueltiger UUID-Eintrag - ueberspringen
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : colors.entrySet()) {
            yaml.set("colors." + entry.getKey(), entry.getValue());
        }

        List<String> boldList = new ArrayList<>();
        for (UUID uuid : bold) {
            boldList.add(uuid.toString());
        }
        yaml.set("bold", boldList);

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Konnte chatcolors.yml nicht speichern.", ex);
        }
    }

    public String getColor(UUID uuid) {
        return colors.get(uuid);
    }

    public void setColor(UUID uuid, String code) {
        colors.put(uuid, code);
        save();
    }

    public void clearColor(UUID uuid) {
        colors.remove(uuid);
        save();
    }

    public boolean isBold(UUID uuid) {
        return bold.contains(uuid);
    }

    public void setBold(UUID uuid, boolean value) {
        if (value) {
            bold.add(uuid);
        } else {
            bold.remove(uuid);
        }
        save();
    }
}
