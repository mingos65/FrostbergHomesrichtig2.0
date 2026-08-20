package de.frostberg.homes.handel;

import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * Zustand eines laufenden Handels zwischen genau zwei Spielern: das
 * gemeinsam geoeffnete Inventar (beide Spieler sehen dasselbe Inventory-
 * Objekt live, wie bei einer echten Truhe mit zwei Betrachtern), die
 * angebotenen Tokens/Gold-Betraege und der Bestaetigungs-Status jeder
 * Seite. Aendert sich irgendwas (Items, Betraege), muessen BEIDE Seiten
 * erneut bestaetigen - das ist der Betrugsschutz.
 */
public class TradeSession {

    private final UUID playerA;
    private final UUID playerB;
    private Inventory inventory;

    private long tokensOfferedA;
    private long tokensOfferedB;
    private double goldOfferedA;
    private double goldOfferedB;

    private boolean confirmedA;
    private boolean confirmedB;
    private boolean completed;
    private boolean cancelled;

    public TradeSession(UUID playerA, UUID playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
    }

    public UUID getPlayerA() {
        return playerA;
    }

    public UUID getPlayerB() {
        return playerB;
    }

    public boolean isPlayerA(UUID uuid) {
        return playerA.equals(uuid);
    }

    public boolean isPlayerB(UUID uuid) {
        return playerB.equals(uuid);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public long getTokensOffered(UUID uuid) {
        return isPlayerA(uuid) ? tokensOfferedA : tokensOfferedB;
    }

    public void setTokensOffered(UUID uuid, long amount) {
        long clamped = Math.max(0, amount);
        if (isPlayerA(uuid)) {
            tokensOfferedA = clamped;
        } else {
            tokensOfferedB = clamped;
        }
    }

    public double getGoldOffered(UUID uuid) {
        return isPlayerA(uuid) ? goldOfferedA : goldOfferedB;
    }

    public void setGoldOffered(UUID uuid, double amount) {
        double clamped = Math.max(0, amount);
        if (isPlayerA(uuid)) {
            goldOfferedA = clamped;
        } else {
            goldOfferedB = clamped;
        }
    }

    public boolean isConfirmed(UUID uuid) {
        return isPlayerA(uuid) ? confirmedA : confirmedB;
    }

    public void setConfirmed(UUID uuid, boolean confirmed) {
        if (isPlayerA(uuid)) {
            confirmedA = confirmed;
        } else {
            confirmedB = confirmed;
        }
    }

    public void resetConfirmations() {
        confirmedA = false;
        confirmedB = false;
    }

    public boolean bothConfirmed() {
        return confirmedA && confirmedB;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isFinished() {
        return completed || cancelled;
    }
}
