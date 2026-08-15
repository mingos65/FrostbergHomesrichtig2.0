package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.Home;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * /home [nr]
 *
 * Ablauf: Home suchen -> Cooldown pruefen -> Ziel-Location aufloesen
 * (inkl. Safe-Teleport) -> Warmup-Countdown im Chat -> Teleport + Effekte.
 *
 * Implementiert zusaetzlich Listener, um bei PlayerQuitEvent einen laufenden
 * Countdown sauber abzubrechen (siehe FrostbergHomes#registerCommands).
 */
public class HomeCommand implements CommandExecutor, TabCompleter, Listener {

    private final FrostbergHomes plugin;

    // Spieler mit laufendem Countdown -> die geplante Bukkit-Task (zum Abbrechen)
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    public HomeCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        int number = 1;
        if (args.length >= 1) {
            try {
                number = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtil.get(plugin.getConfig(), "invalid-number"));
                return true;
            }
        }

        Optional<Home> homeOptional = plugin.getHomeManager().getHome(player.getUniqueId(), number);
        if (homeOptional.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "home-not-found")
                    .replace("%nr%", String.valueOf(number)));
            return true;
        }

        long remainingCooldown = plugin.getHomeManager().getRemainingCooldown(player);
        if (remainingCooldown > 0) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "cooldown-active")
                    .replace("%seconds%", String.valueOf(remainingCooldown)));
            return true;
        }

        Location target = resolveTargetLocation(player, homeOptional.get());
        if (target == null) {
            return true; // passende Fehlermeldung wurde bereits gesendet
        }

        startTeleport(player, number, target);
        return true;
    }

    /**
     * Prueft ob die Welt geladen ist und sucht bei aktiviertem safe-teleport
     * eine sichere Position um das Home herum. Sendet bei Fehlern selbst die
     * passende Nachricht und gibt dann null zurueck.
     */
    private Location resolveTargetLocation(Player player, Home home) {
        if (!home.isWorldLoaded()) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "world-not-loaded"));
            return null;
        }

        Location raw = home.toLocation();

        if (!plugin.getConfig().getBoolean("settings.safe-teleport", true)) {
            return raw;
        }

        Location safe = findSafeLocation(raw);
        if (safe == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "safe-teleport-not-found"));
            return null;
        }

        return safe;
    }

    // ---------------------------------------------------------------
    // Warmup-Countdown (im Chat)
    // ---------------------------------------------------------------

    private void startTeleport(Player player, int number, Location target) {
        UUID uuid = player.getUniqueId();

        // Falls schon ein Countdown laeuft (z.B. Befehl doppelt ausgefuehrt), zuerst abbrechen
        cancelPending(uuid);

        int warmupSeconds = plugin.getConfig().getInt("settings.warmup-seconds", 0);
        boolean bypassWarmup = player.hasPermission("homes.bypass.warmup");

        if (warmupSeconds <= 0 || bypassWarmup) {
            teleportNow(player, number, target);
            return;
        }

        boolean cancelOnMove = plugin.getConfig().getBoolean("settings.cancel-warmup-on-move", true);
        Location startLocation = player.getLocation();

        if (plugin.getConfig().getBoolean("effects.sound-on-warmup-start", true)) {
            playConfiguredSound(player, player.getLocation(), "effects.sound-warmup",
                    "effects.sound-warmup-volume", "effects.sound-warmup-pitch");
        }

        // Zaehler als Array, damit die Lambda-Task ihn zwischen den Ticks veraendern kann
        int[] secondsLeft = {warmupSeconds};

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelPending(uuid);
                return;
            }

            if (cancelOnMove && hasMoved(startLocation, player.getLocation())) {
                player.sendMessage(MessageUtil.get(plugin.getConfig(), "teleport-cancelled-move"));
                cancelPending(uuid);
                return;
            }

            if (secondsLeft[0] <= 0) {
                cancelPending(uuid);
                teleportNow(player, number, target);
                return;
            }

            player.sendMessage(MessageUtil.get(plugin.getConfig(), "teleport-warmup")
                    .replace("%seconds%", String.valueOf(secondsLeft[0])));
            secondsLeft[0]--;
        }, 0L, 20L);

        pendingTeleports.put(uuid, task);
    }

    private boolean hasMoved(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return true;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private void cancelPending(UUID uuid) {
        BukkitTask task = pendingTeleports.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelPending(event.getPlayer().getUniqueId());
    }

    // ---------------------------------------------------------------
    // Teleport + Effekte
    // ---------------------------------------------------------------

    private void teleportNow(Player player, int number, Location target) {
        player.teleport(target);
        plugin.getHomeManager().setCooldown(player);

        player.sendMessage(MessageUtil.get(plugin.getConfig(), "teleport-success")
                .replace("%nr%", String.valueOf(number)));

        if (plugin.getConfig().getBoolean("effects.sound-on-teleport", true)) {
            playConfiguredSound(player, target, "effects.sound-teleport",
                    "effects.sound-teleport-volume", "effects.sound-teleport-pitch");
        }

        if (plugin.getConfig().getBoolean("effects.particles-on-teleport", true)) {
            spawnConfiguredParticle(target);
        }

        if (plugin.getConfig().getBoolean("effects.title-on-teleport", true)) {
            showConfiguredTitle(player, number);
        }
    }

    private void playConfiguredSound(Player player, Location location, String soundPath, String volumePath, String pitchPath) {
        String soundName = plugin.getConfig().getString(soundPath);
        if (soundName == null || soundName.isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) plugin.getConfig().getDouble(volumePath, 1.0);
            float pitch = (float) plugin.getConfig().getDouble(pitchPath, 1.0);
            player.playSound(location, sound, volume, pitch);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Ungueltiger Sound-Name in config.yml (" + soundPath + "): " + soundName);
        }
    }

    private void spawnConfiguredParticle(Location location) {
        String particleName = plugin.getConfig().getString("effects.particle-teleport");
        if (particleName == null || particleName.isEmpty()) {
            return;
        }

        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            int amount = plugin.getConfig().getInt("effects.particle-amount", 30);
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location.clone().add(0, 1, 0), amount, 0.5, 0.5, 0.5, 0.02);
            }
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Ungueltiger Partikel-Name in config.yml: " + particleName);
        }
    }

    private void showConfiguredTitle(Player player, int number) {
        String title = MessageUtil.get(plugin.getConfig(), "teleport-title").replace("%nr%", String.valueOf(number));
        String subtitle = MessageUtil.get(plugin.getConfig(), "teleport-subtitle");

        int fadeIn = plugin.getConfig().getInt("effects.title-fade-in", 5);
        int stay = plugin.getConfig().getInt("effects.title-stay", 30);
        int fadeOut = plugin.getConfig().getInt("effects.title-fade-out", 10);

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    // ---------------------------------------------------------------
    // Safe-Teleport (ohne externe Abhaengigkeiten wie PaperLib)
    // ---------------------------------------------------------------

    private Location findSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        int x = location.getBlockX();
        int z = location.getBlockZ();

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 2;
        int clampedStart = Math.max(minY, Math.min(location.getBlockY(), maxY));

        // Zuerst ab der Home-Hoehe nach oben suchen - deckt den haeufigsten Fall ab
        // (z.B. das Gelaende wurde seit dem Setzen des Homes veraendert)
        for (int y = clampedStart; y <= maxY; y++) {
            if (isSafe(world, x, y, z)) {
                return centered(world, x, y, z, location.getYaw(), location.getPitch());
            }
        }

        // Danach nach unten suchen
        for (int y = clampedStart - 1; y >= minY; y--) {
            if (isSafe(world, x, y, z)) {
                return centered(world, x, y, z, location.getYaw(), location.getPitch());
            }
        }

        return null;
    }

    private boolean isSafe(World world, int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        return isPassable(feet.getType()) && isPassable(head.getType())
                && ground.getType().isSolid() && !isHarmful(ground.getType());
    }

    private boolean isPassable(Material material) {
        return !material.isSolid() && !isHarmful(material);
    }

    private boolean isHarmful(Material material) {
        return material == Material.LAVA
                || material == Material.FIRE
                || material == Material.SOUL_FIRE
                || material == Material.MAGMA_BLOCK
                || material == Material.CACTUS
                || material == Material.SWEET_BERRY_BUSH
                || material == Material.POWDER_SNOW;
    }

    private Location centered(World world, int x, int y, int z, float yaw, float pitch) {
        return new Location(world, x + 0.5, y, z + 0.5, yaw, pitch);
    }

    // ---------------------------------------------------------------
    // Tab-Complete
    // ---------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            Map<Integer, Home> homes = plugin.getHomeManager().getHomes(player.getUniqueId());
            List<String> numbers = new ArrayList<>();
            for (int nr : homes.keySet()) {
                numbers.add(String.valueOf(nr));
            }
            return filter(numbers, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
