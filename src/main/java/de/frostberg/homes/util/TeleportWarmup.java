package de.frostberg.homes.util;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

/**
 * Gemeinsame Warmup-Countdown-Logik (im Chat, abbrechbar bei Bewegung).
 * Urspruenglich Teil von HomeCommand, jetzt hierher ausgelagert, damit die
 * TPA-Commands (siehe manager/TpaManager) denselben Ablauf wiederverwenden
 * koennen statt ihn zu duplizieren.
 */
public final class TeleportWarmup {

    private TeleportWarmup() {
    }

    /**
     * Startet den Countdown fuer {@code player} (bricht einen bereits laufenden
     * zuerst ab). Ohne Warmup (settings.warmup-seconds <= 0) oder mit
     * {@code bypassPermission} wird sofort {@code onComplete} ausgefuehrt.
     * {@code onWarmupStart} laeuft nur, wenn tatsaechlich ein Countdown beginnt
     * (z.B. fuer den Warmup-Start-Sound bei Homes) und darf null sein.
     */
    public static void start(FrostbergHomes plugin, Player player, Map<UUID, BukkitTask> pendingTeleports,
                              String bypassPermission, Runnable onWarmupStart, Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        cancel(pendingTeleports, uuid);

        int warmupSeconds = plugin.getConfig().getInt("settings.warmup-seconds", 0);
        boolean bypassWarmup = bypassPermission != null && player.hasPermission(bypassPermission);

        if (warmupSeconds <= 0 || bypassWarmup) {
            onComplete.run();
            return;
        }

        if (onWarmupStart != null) {
            onWarmupStart.run();
        }

        boolean cancelOnMove = plugin.getConfig().getBoolean("settings.cancel-warmup-on-move", true);
        Location startLocation = player.getLocation();

        // Zaehler als Array, damit die Lambda-Task ihn zwischen den Ticks veraendern kann
        int[] secondsLeft = {warmupSeconds};

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancel(pendingTeleports, uuid);
                return;
            }

            if (cancelOnMove && hasMoved(startLocation, player.getLocation())) {
                player.sendMessage(MessageUtil.get(plugin.getConfig(), "teleport-cancelled-move"));
                cancel(pendingTeleports, uuid);
                return;
            }

            if (secondsLeft[0] <= 0) {
                cancel(pendingTeleports, uuid);
                onComplete.run();
                return;
            }

            player.sendMessage(MessageUtil.get(plugin.getConfig(), "teleport-warmup")
                    .replace("%seconds%", String.valueOf(secondsLeft[0])));
            secondsLeft[0]--;
        }, 0L, 20L);

        pendingTeleports.put(uuid, task);
    }

    public static void cancel(Map<UUID, BukkitTask> pendingTeleports, UUID uuid) {
        BukkitTask task = pendingTeleports.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private static boolean hasMoved(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return true;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
