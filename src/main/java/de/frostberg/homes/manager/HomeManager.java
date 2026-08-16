package de.frostberg.homes.manager;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.Home;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Verwaltet Homes fuer alle Spieler: Laden/Speichern als YAML unter
 * plugins/FrostbergHomes/playerdata/&lt;UUID&gt;.yml, Home-Limits (LuckPerms-
 * kompatibel ueber normale Bukkit-Permissions) und den Teleport-Cooldown.
 *
 * Warmup/Countdown und Safe-Teleport liegen bewusst NICHT hier, sondern in den
 * Commands - der HomeManager kuemmert sich ausschliesslich um Daten.
 */
public class HomeManager {

    private final FrostbergHomes plugin;
    private final File playerDataFolder;

    // UUID -> (Home-Nummer -> Home). Cache im Speicher, Datei ist die Quelle der Wahrheit.
    private final Map<UUID, Map<Integer, Home>> homesCache = new HashMap<>();

    // UUID -> Zeitpunkt (System.currentTimeMillis) des letzten erfolgreichen Home-Teleports
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public HomeManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    // ---------------------------------------------------------------
    // Laden / Speichern
    // ---------------------------------------------------------------

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, uuid.toString() + ".yml");
    }

    /**
     * Laedt die Homes eines Spielers aus der YAML-Datei in den Cache.
     * Passiert nichts, wenn der Spieler schon im Cache ist - daher gefahrlos
     * mehrfach aufrufbar (z.B. beim Join und zusaetzlich lazy in getHomes()).
     */
    public void loadHomes(UUID uuid) {
        if (homesCache.containsKey(uuid)) {
            return;
        }

        Map<Integer, Home> homes = new LinkedHashMap<>();
        File file = getPlayerFile(uuid);

        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = yaml.getConfigurationSection("homes");

            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        int nr = Integer.parseInt(key);
                        String path = "homes." + key + ".";
                        String world = yaml.getString(path + "world", "world");
                        double x = yaml.getDouble(path + "x");
                        double y = yaml.getDouble(path + "y");
                        double z = yaml.getDouble(path + "z");
                        float yaw = (float) yaml.getDouble(path + "yaw");
                        float pitch = (float) yaml.getDouble(path + "pitch");
                        Home home = new Home(nr, world, x, y, z, yaw, pitch);
                        home.setName(yaml.getString(path + "name"));
                        homes.put(nr, home);
                    } catch (NumberFormatException ex) {
                        plugin.getLogger().warning("Ungueltiger Home-Schluessel '" + key
                                + "' in " + file.getName() + " - wird uebersprungen.");
                    }
                }
            }
        }

        homesCache.put(uuid, homes);
    }

    /**
     * Entfernt einen Spieler aus dem Cache (z.B. beim Quit), um Speicher zu sparen.
     * Die Daten sind zu diesem Zeitpunkt bereits gespeichert (siehe saveHomes).
     */
    public void unloadHomes(UUID uuid) {
        homesCache.remove(uuid);
        cooldowns.remove(uuid);
    }

    /**
     * Schreibt die Homes eines Spielers sofort auf die Platte.
     */
    public void saveHomes(UUID uuid) {
        Map<Integer, Home> homes = homesCache.get(uuid);
        if (homes == null) {
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (Home home : homes.values()) {
            String path = "homes." + home.getNumber() + ".";
            yaml.set(path + "world", home.getWorldName());
            yaml.set(path + "x", home.getX());
            yaml.set(path + "y", home.getY());
            yaml.set(path + "z", home.getZ());
            yaml.set(path + "yaw", home.getYaw());
            yaml.set(path + "pitch", home.getPitch());
            yaml.set(path + "name", home.getName());
        }

        try {
            yaml.save(getPlayerFile(uuid));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Homes fuer " + uuid + " nicht speichern.", ex);
        }
    }

    /**
     * Speichert alle aktuell im Cache befindlichen Spieler, z.B. beim onDisable().
     */
    public void saveAll() {
        for (UUID uuid : homesCache.keySet()) {
            saveHomes(uuid);
        }
    }

    // ---------------------------------------------------------------
    // Home-Verwaltung
    // ---------------------------------------------------------------

    /**
     * Liefert eine unveraenderliche Sicht auf alle Homes eines Spielers,
     * sortiert nach Nummer (LinkedHashMap-Einfuegereihenfolge beim Laden ist
     * nicht garantiert sortiert, daher hier zusaetzlich sortiert zurueckgeben).
     */
    public Map<Integer, Home> getHomes(UUID uuid) {
        loadHomes(uuid);
        Map<Integer, Home> sorted = new LinkedHashMap<>();
        homesCache.get(uuid).values().stream()
                .sorted((a, b) -> Integer.compare(a.getNumber(), b.getNumber()))
                .forEach(home -> sorted.put(home.getNumber(), home));
        return Collections.unmodifiableMap(sorted);
    }

    public Optional<Home> getHome(UUID uuid, int number) {
        loadHomes(uuid);
        return Optional.ofNullable(homesCache.get(uuid).get(number));
    }

    public int getHomeCount(UUID uuid) {
        loadHomes(uuid);
        return homesCache.get(uuid).size();
    }

    public boolean hasHome(UUID uuid, int number) {
        loadHomes(uuid);
        return homesCache.get(uuid).containsKey(number);
    }

    /**
     * Setzt (oder ueberschreibt) ein Home und speichert sofort.
     * Gibt true zurueck, wenn dabei ein vorhandenes Home ueberschrieben wurde.
     */
    public boolean setHome(Player player, int number, Location location) {
        UUID uuid = player.getUniqueId();
        loadHomes(uuid);

        Map<Integer, Home> homes = homesCache.get(uuid);
        boolean overwritten = homes.containsKey(number);

        homes.put(number, Home.fromLocation(number, location));
        saveHomes(uuid);

        return overwritten;
    }

    /**
     * Setzt den Anzeigenamen eines Homes (z.B. ueber das /homes-GUI) und
     * speichert sofort. Gibt false zurueck, wenn das Home nicht existiert.
     */
    public boolean renameHome(UUID uuid, int number, String name) {
        loadHomes(uuid);
        Home home = homesCache.get(uuid).get(number);
        if (home == null) {
            return false;
        }

        home.setName(name);
        saveHomes(uuid);
        return true;
    }

    /**
     * Loescht ein Home und speichert sofort. Gibt true zurueck, wenn es existierte.
     */
    public boolean deleteHome(UUID uuid, int number) {
        loadHomes(uuid);
        Map<Integer, Home> homes = homesCache.get(uuid);

        if (homes.remove(number) != null) {
            saveHomes(uuid);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Limits (kompatibel mit LuckPerms ueber normale Bukkit-Permissions)
    // ---------------------------------------------------------------

    /**
     * Ermittelt das Home-Limit eines Spielers:
     * - homes.limit.unlimited  -> unbegrenzt
     * - hoechste zutreffende   homes.limit.&lt;n&gt; (1-28, entspricht den 2
     *   Seiten a 14 Slots im /homes-GUI)
     * - sonst Fallback auf settings.default-home-limit aus der config.yml
     */
    public int getHomeLimit(Player player) {
        if (player.hasPermission("homes.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }

        int highest = -1;
        for (int i = 1; i <= 28; i++) {
            if (player.hasPermission("homes.limit." + i)) {
                highest = Math.max(highest, i);
            }
        }

        if (highest != -1) {
            return highest;
        }

        return plugin.getConfig().getInt("settings.default-home-limit", 1);
    }

    // ---------------------------------------------------------------
    // Cooldown (rein im Speicher - muss keinen Server-Neustart ueberleben)
    // ---------------------------------------------------------------

    /**
     * Verbleibende Cooldown-Zeit in ganzen Sekunden (0, wenn kein Cooldown aktiv
     * ist oder der Spieler homes.bypass.cooldown besitzt).
     */
    public long getRemainingCooldown(Player player) {
        if (player.hasPermission("homes.bypass.cooldown")) {
            return 0;
        }

        long cooldownSeconds = plugin.getConfig().getLong("settings.cooldown-seconds", 0);
        if (cooldownSeconds <= 0) {
            return 0;
        }

        Long last = cooldowns.get(player.getUniqueId());
        if (last == null) {
            return 0;
        }

        long elapsedSeconds = (System.currentTimeMillis() - last) / 1000L;
        long remaining = cooldownSeconds - elapsedSeconds;
        return Math.max(remaining, 0);
    }

    /**
     * Setzt den Cooldown-Zeitpunkt eines Spielers auf jetzt.
     * Sollte erst NACH einem erfolgreichen Teleport (also nach dem Warmup) aufgerufen werden.
     */
    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void clearCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
