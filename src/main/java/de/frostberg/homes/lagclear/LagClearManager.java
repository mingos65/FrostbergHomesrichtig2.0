package de.frostberg.homes.lagclear;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;

/**
 * Entfernt regelmaessig Item-Entities, die auf dem Boden liegen (nicht
 * platzierte Bloecke), um Server-Lag zu vermeiden. Zwei unabhaengige
 * Zeitplaene:
 * - Regulaer: alle settings.lagclear.auto-interval-minutes eine kurze
 *   Warnung, dann Clear.
 * - Notfall-Ueberwachung: prueft alle paar Sekunden, ob zu viele Items
 *   gleichzeitig liegen (emergency-threshold) - falls ja, laengere Warnung
 *   und Clear erst nach emergency-warning-seconds, damit Spieler ihre
 *   Sachen noch aufheben koennen.
 */
public class LagClearManager {

    private final FrostbergHomes plugin;
    private BukkitTask autoTask;
    private BukkitTask emergencyMonitorTask;
    private boolean emergencyActive = false;

    public LagClearManager(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long intervalTicks = Math.max(1, plugin.getConfig().getLong("lagclear.auto-interval-minutes", 30)) * 60L * 20L;
        autoTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runScheduledClear, intervalTicks, intervalTicks);

        long monitorIntervalTicks = Math.max(1, plugin.getConfig().getLong("lagclear.emergency-check-interval-seconds", 30)) * 20L;
        emergencyMonitorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkEmergency, monitorIntervalTicks, monitorIntervalTicks);
    }

    public void stop() {
        if (autoTask != null) {
            autoTask.cancel();
        }
        if (emergencyMonitorTask != null) {
            emergencyMonitorTask.cancel();
        }
    }

    /**
     * /laggclear bzw. regulaerer/manueller Clear - Ansage bei Vorwarnzeit,
     * danach zusaetzlich ein Countdown 5-4-3-2-1 in den letzten 5 Sekunden
     * davor, dann wird geleert.
     */
    public void manualClear() {
        long leadSeconds = Math.max(5, plugin.getConfig().getLong("lagclear.warning-lead-seconds", 60));
        Bukkit.broadcastMessage(MessageUtil.get(plugin.getMessages(), "lagclear-warning")
                .replace("%seconds%", String.valueOf(leadSeconds)));

        long countdownStartTicks = (leadSeconds - 5) * 20L;
        for (int secondsLeft = 5; secondsLeft >= 1; secondsLeft--) {
            long delayTicks = countdownStartTicks + (5 - secondsLeft) * 20L;
            final int displaySeconds = secondsLeft;
            Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(
                    MessageUtil.get(plugin.getMessages(), "lagclear-countdown").replace("%seconds%", String.valueOf(displaySeconds))), delayTicks);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::clearItems, leadSeconds * 20L);
    }

    private void runScheduledClear() {
        if (emergencyActive) {
            // Notfall-Bereinigung laeuft bereits, kein zusaetzlicher regulaerer Clear noetig
            return;
        }
        manualClear();
    }

    private void checkEmergency() {
        if (emergencyActive) {
            return;
        }
        int threshold = plugin.getConfig().getInt("lagclear.emergency-threshold", 2000);
        if (countGroundItems() < threshold) {
            return;
        }

        emergencyActive = true;
        long warningSeconds = Math.max(1, plugin.getConfig().getLong("lagclear.emergency-warning-seconds", 180));
        Bukkit.broadcastMessage(MessageUtil.get(plugin.getMessages(), "lagclear-emergency-warning")
                .replace("%minutes%", String.valueOf(Math.max(1, warningSeconds / 60))));
        long warningTicks = warningSeconds * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            clearItems();
            emergencyActive = false;
        }, warningTicks);
    }

    private int countGroundItems() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Item.class)) {
                count++;
            }
        }
        return count;
    }

    private void clearItems() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                item.remove();
                removed++;
            }
        }
        Bukkit.broadcastMessage(MessageUtil.get(plugin.getMessages(), "lagclear-done").replace("%amount%", String.valueOf(removed)));
    }
}
