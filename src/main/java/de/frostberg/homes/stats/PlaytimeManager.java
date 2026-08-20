package de.frostberg.homes.stats;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verfolgt die gespielte Zeit pro Spieler dauerhaft in playtime.yml. Waehrend
 * ein Spieler online ist, laeuft die aktuelle Session nur im Speicher
 * (sessionStart) und wird erst beim Quit auf den gespeicherten Gesamtwert
 * aufaddiert - getTotalSeconds() rechnet die laufende Session aber live mit
 * ein, damit /stats waehrend des Spielens korrekte Werte zeigt.
 */
public class PlaytimeManager {

    private final FrostbergHomes plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public PlaytimeManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playtime.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Konnte playtime.yml nicht anlegen: " + ex.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void startSession(Player player) {
        sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
        config.set(player.getUniqueId() + ".name", player.getName());
    }

    public void endSession(Player player) {
        Long start = sessionStart.remove(player.getUniqueId());
        if (start == null) {
            return;
        }
        long elapsedSeconds = (System.currentTimeMillis() - start) / 1000L;
        long total = getStoredSeconds(player.getUniqueId()) + elapsedSeconds;
        config.set(player.getUniqueId() + ".seconds", total);
        config.set(player.getUniqueId() + ".name", player.getName());
        save();
    }

    private long getStoredSeconds(UUID uuid) {
        return config.getLong(uuid + ".seconds", 0);
    }

    /** Gespeicherter Wert PLUS laufende Session, falls der Spieler gerade online ist. */
    public long getTotalSeconds(UUID uuid) {
        long total = getStoredSeconds(uuid);
        Long start = sessionStart.get(uuid);
        if (start != null) {
            total += (System.currentTimeMillis() - start) / 1000L;
        }
        return total;
    }

    public String getStoredName(UUID uuid, String fallback) {
        return config.getString(uuid + ".name", fallback);
    }

    /** Die "limit" Spieler mit der meisten Spielzeit, absteigend sortiert. */
    public List<UUID> getTop(int limit) {
        List<UUID> uuids = new ArrayList<>();
        for (String key : config.getKeys(false)) {
            try {
                uuids.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                // Kein gueltiger UUID-Key, ueberspringen
            }
        }
        uuids.sort(Comparator.comparingLong(this::getTotalSeconds).reversed());
        return uuids.subList(0, Math.min(limit, uuids.size()));
    }

    /** Formatiert Sekunden als "XT YStd ZMin" (nur die Einheiten, die > 0 sind). */
    public String format(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("T ");
        }
        if (hours > 0 || days > 0) {
            builder.append(hours).append("Std ");
        }
        builder.append(minutes).append("Min");
        return builder.toString();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte playtime.yml nicht speichern: " + ex.getMessage());
        }
    }
}
