package de.frostberg.homes.clan.model;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Eine offene Einladung eines Clans an einen Spieler. Analog zu
 * de.frostberg.homes.model.TpaRequest, nur ohne "Richtung" - hier gibt es
 * immer nur einen eingeladenen Spieler und einen Clan.
 */
public class ClanInvite {

    private final String clanName;
    private final UUID invitedUuid;
    private final String inviterName;
    private BukkitTask expiryTask;

    public ClanInvite(String clanName, UUID invitedUuid, String inviterName) {
        this.clanName = clanName;
        this.invitedUuid = invitedUuid;
        this.inviterName = inviterName;
    }

    public String getClanName() {
        return clanName;
    }

    public UUID getInvitedUuid() {
        return invitedUuid;
    }

    public String getInviterName() {
        return inviterName;
    }

    public BukkitTask getExpiryTask() {
        return expiryTask;
    }

    public void setExpiryTask(BukkitTask expiryTask) {
        this.expiryTask = expiryTask;
    }
}
