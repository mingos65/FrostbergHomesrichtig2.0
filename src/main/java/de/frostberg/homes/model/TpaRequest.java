package de.frostberg.homes.model;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Eine offene Teleport-Anfrage zwischen zwei Spielern (/tpa oder /tpahere).
 */
public class TpaRequest {

    public enum Type {
        /** Absender wird zum Ziel teleportiert (/tpa). */
        TPA,
        /** Ziel wird zum Absender teleportiert (/tpahere). */
        TPA_HERE
    }

    private final UUID senderUuid;
    private final String senderName;
    private final UUID targetUuid;
    private final String targetName;
    private final Type type;

    private BukkitTask expiryTask;

    public TpaRequest(UUID senderUuid, String senderName, UUID targetUuid, String targetName, Type type) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.type = type;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public Type getType() {
        return type;
    }

    public BukkitTask getExpiryTask() {
        return expiryTask;
    }

    public void setExpiryTask(BukkitTask expiryTask) {
        this.expiryTask = expiryTask;
    }

    /** Wer sich tatsaechlich bewegt: bei TPA der Absender, bei TPA_HERE das Ziel. */
    public UUID getTeleportingUuid() {
        return type == Type.TPA ? senderUuid : targetUuid;
    }

    /** Das Gegenstueck zu {@link #getTeleportingUuid()} - wer stehen bleibt. */
    public UUID getDestinationUuid() {
        return type == Type.TPA ? targetUuid : senderUuid;
    }
}
