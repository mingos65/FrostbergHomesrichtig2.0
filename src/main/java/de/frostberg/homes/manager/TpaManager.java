package de.frostberg.homes.manager;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.TpaRequest;
import de.frostberg.homes.util.MessageUtil;
import de.frostberg.homes.util.TeleportWarmup;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Verwaltet offene TPA-Anfragen (/tpa, /tpahere) inkl. Ablauf-Timer, den
 * TPA-eigenen Cooldown (unabhaengig vom Homes-Cooldown) sowie den
 * Warmup-Countdown nach einer Annahme. Buendelt das hier in einer Klasse
 * (statt wie bei Homes auf Manager+Command aufgeteilt), weil /tpaccept -
 * egal ob getippt oder per Klick - und der Ablauf-Timer denselben Zustand
 * teilen muessen.
 */
public class TpaManager implements Listener {

    private final FrostbergHomes plugin;

    // Ziel-UUID -> aktuell offene Anfrage an diesen Spieler (nur eine gleichzeitig)
    private final Map<UUID, TpaRequest> pendingByTarget = new HashMap<>();

    // UUID -> Zeitpunkt (System.currentTimeMillis) des letzten erfolgreichen TPA-Teleports
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Spieler mit laufendem Warmup-Countdown -> die geplante Bukkit-Task (zum Abbrechen)
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    public TpaManager(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Anfragen
    // ---------------------------------------------------------------

    public Optional<TpaRequest> getPendingRequestFrom(UUID senderUuid) {
        return pendingByTarget.values().stream()
                .filter(request -> request.getSenderUuid().equals(senderUuid))
                .findFirst();
    }

    public TpaRequest getPendingRequestTo(UUID targetUuid) {
        return pendingByTarget.get(targetUuid);
    }

    /**
     * Legt eine neue Anfrage an und plant ihren automatischen Ablauf nach
     * settings.tpa-expiry-seconds. Eine bereits vorhandene Anfrage an denselben
     * Zielspieler wird ersetzt (deren Ablauf-Timer wird abgebrochen).
     */
    public TpaRequest createRequest(Player sender, Player target, TpaRequest.Type type, Runnable onExpire) {
        TpaRequest previous = pendingByTarget.get(target.getUniqueId());
        if (previous != null) {
            cancelTask(previous.getExpiryTask());
        }

        TpaRequest request = new TpaRequest(sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), target.getName(), type);

        int expirySeconds = plugin.getConfig().getInt("settings.tpa-expiry-seconds", 60);
        BukkitTask expiryTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Nur abbrechen, wenn fuer dieses Ziel noch genau diese Anfrage aktiv ist -
            // sonst wuerde eine spaetere Anfrage vom selben/anderem Spieler versehentlich
            // mit entfernt.
            if (pendingByTarget.get(target.getUniqueId()) == request) {
                pendingByTarget.remove(target.getUniqueId());
                onExpire.run();
            }
        }, expirySeconds * 20L);

        request.setExpiryTask(expiryTask);
        pendingByTarget.put(target.getUniqueId(), request);
        return request;
    }

    /** Entfernt eine Anfrage (z.B. nach Annahme/Ablehnung) und bricht ihren Ablauf-Timer ab. */
    public void removeRequest(TpaRequest request) {
        cancelTask(request.getExpiryTask());
        pendingByTarget.remove(request.getTargetUuid());
    }

    // ---------------------------------------------------------------
    // Warmup-Countdown nach Annahme (wiederverwendet TeleportWarmup, s. HomeCommand)
    // ---------------------------------------------------------------

    /**
     * Prueft den TPA-Cooldown, startet danach den Warmup-Countdown fuer den
     * sich tatsaechlich bewegenden Spieler und teleportiert ihn anschliessend
     * zum jeweils anderen (bei /tpa der Absender, bei /tpahere das Ziel -
     * siehe TpaRequest#getTeleportingUuid).
     */
    public void startAcceptedTeleport(Player teleportingPlayer, Player destinationPlayer) {
        long remainingCooldown = getRemainingCooldown(teleportingPlayer);
        if (remainingCooldown > 0) {
            teleportingPlayer.sendMessage(MessageUtil.get(plugin.getConfig(), "cooldown-active")
                    .replace("%seconds%", String.valueOf(remainingCooldown)));
            return;
        }

        TeleportWarmup.start(plugin, teleportingPlayer, pendingTeleports, "tpa.bypass.warmup", null, () -> {
            teleportingPlayer.teleport(destinationPlayer.getLocation());
            setCooldown(teleportingPlayer);

            teleportingPlayer.sendMessage(MessageUtil.get(plugin.getConfig(), "tp-success")
                    .replace("%player%", destinationPlayer.getName()));
        });
    }

    // ---------------------------------------------------------------
    // Cooldown (eigener Topf, unabhaengig vom Homes-Cooldown)
    // ---------------------------------------------------------------

    public long getRemainingCooldown(Player player) {
        if (player.hasPermission("tpa.bypass.cooldown")) {
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
        return Math.max(cooldownSeconds - elapsedSeconds, 0);
    }

    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // ---------------------------------------------------------------
    // Aufraeumen beim Verlassen des Servers
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        pendingByTarget.values().removeIf(request -> {
            if (request.getSenderUuid().equals(uuid) || request.getTargetUuid().equals(uuid)) {
                cancelTask(request.getExpiryTask());
                return true;
            }
            return false;
        });

        TeleportWarmup.cancel(pendingTeleports, uuid);
        cooldowns.remove(uuid);
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
