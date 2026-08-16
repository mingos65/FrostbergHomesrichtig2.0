package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.Home;
import de.frostberg.homes.util.MessageUtil;
import de.frostberg.homes.util.SafeTeleport;
import de.frostberg.homes.util.TeleportWarmup;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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

        if (args.length == 0) {
            plugin.getHomesGuiListener().openMenu(player);
            return true;
        }

        int number;
        try {
            number = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "invalid-number"));
            return true;
        }

        teleportToHome(player, number);
        return true;
    }

    /**
     * Fuehrt den kompletten Teleport-Ablauf (Home suchen -> Cooldown pruefen
     * -> Safe-Teleport -> Warmup) fuer eine Home-Nummer aus. Oeffentlich, damit
     * HomesGuiListener beim Linksklick auf ein Home denselben Ablauf nutzen
     * kann statt ihn zu duplizieren.
     */
    public void teleportToHome(Player player, int number) {
        Optional<Home> homeOptional = plugin.getHomeManager().getHome(player.getUniqueId(), number);
        if (homeOptional.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "home-not-found")
                    .replace("%nr%", String.valueOf(number)));
            return;
        }

        long remainingCooldown = plugin.getHomeManager().getRemainingCooldown(player);
        if (remainingCooldown > 0) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "cooldown-active")
                    .replace("%seconds%", String.valueOf(remainingCooldown)));
            return;
        }

        Location target = resolveTargetLocation(player, homeOptional.get());
        if (target == null) {
            return; // passende Fehlermeldung wurde bereits gesendet
        }

        startTeleport(player, number, target);
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

        Location safe = SafeTeleport.findSafeLocation(raw);
        if (safe == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "safe-teleport-not-found"));
            return null;
        }

        return safe;
    }

    // ---------------------------------------------------------------
    // Warmup-Countdown (im Chat) - eigentliche Logik in TeleportWarmup,
    // damit sie sich die TPA-Commands (siehe manager/TpaManager) teilen
    // ---------------------------------------------------------------

    private void startTeleport(Player player, int number, Location target) {
        Runnable onWarmupStart = () -> {
            if (plugin.getConfig().getBoolean("effects.sound-on-warmup-start", true)) {
                playConfiguredSound(player, player.getLocation(), "effects.sound-warmup",
                        "effects.sound-warmup-volume", "effects.sound-warmup-pitch");
            }
        };

        TeleportWarmup.start(plugin, player, pendingTeleports, "homes.bypass.warmup", onWarmupStart,
                () -> teleportNow(player, number, target));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TeleportWarmup.cancel(pendingTeleports, event.getPlayer().getUniqueId());
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
