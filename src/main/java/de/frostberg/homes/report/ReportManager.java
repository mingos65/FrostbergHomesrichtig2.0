package de.frostberg.homes.report;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Speichert Spieler-Meldungen dauerhaft in reports.yml (Top-Level-Key =
 * UUID des gemeldeten Spielers, Wert = Liste aus Melder/Grund/Zeitpunkt),
 * damit das Team sich per /reports auch nach einem Neustart die Historie
 * eines Spielers ansehen kann. Cooldown pro Melder nur im Speicher, muss
 * Neustart nicht ueberdauern.
 */
public class ReportManager {

    private final FrostbergHomes plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Long> lastReportAt = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public ReportManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reports.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Konnte reports.yml nicht anlegen: " + ex.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public int getCooldownSeconds() {
        return Math.max(0, plugin.getConfig().getInt("report.cooldown-seconds", 60));
    }

    public boolean isOnCooldown(UUID reporter) {
        return getRemainingCooldownSeconds(reporter) > 0;
    }

    public long getRemainingCooldownSeconds(UUID reporter) {
        Long last = lastReportAt.get(reporter);
        if (last == null) {
            return 0;
        }
        long elapsedMs = System.currentTimeMillis() - last;
        long remainingMs = getCooldownSeconds() * 1000L - elapsedMs;
        return Math.max(0, remainingMs / 1000);
    }

    public void addReport(Player reporter, Player reported, String reason) {
        lastReportAt.put(reporter.getUniqueId(), System.currentTimeMillis());

        String key = reported.getUniqueId().toString();
        List<Map<String, Object>> entries = readEntries(key);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("reporter", reporter.getName());
        entry.put("reason", reason);
        entry.put("time", dateFormat.format(new Date()));
        entries.add(entry);

        config.set(key, entries);
        save();
    }

    public List<Map<String, Object>> getReports(OfflinePlayer target) {
        return readEntries(target.getUniqueId().toString());
    }

    private List<Map<String, Object>> readEntries(String key) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<?, ?> raw : config.getMapList(key)) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                copy.put(String.valueOf(e.getKey()), e.getValue());
            }
            result.add(copy);
        }
        return result;
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte reports.yml nicht speichern: " + ex.getMessage());
        }
    }
}
