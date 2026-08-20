package de.frostberg.homes.handel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Haelt laufende Handel-Sessions (pro beteiligtem Spieler auf dasselbe
 * TradeSession-Objekt gemappt) sowie offene Anfragen (wer hat wen
 * angefragt, noch nicht angenommen), rein im Speicher.
 */
public class TradeManager {

    private final Map<UUID, TradeSession> sessionsByPlayer = new ConcurrentHashMap<>();
    /** Ziel-UUID -> UUID des anfragenden Spielers. */
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();

    public boolean hasActiveSession(UUID uuid) {
        return sessionsByPlayer.containsKey(uuid);
    }

    public TradeSession getSession(UUID uuid) {
        return sessionsByPlayer.get(uuid);
    }

    public TradeSession startSession(UUID a, UUID b) {
        TradeSession session = new TradeSession(a, b);
        sessionsByPlayer.put(a, session);
        sessionsByPlayer.put(b, session);
        return session;
    }

    public void endSession(TradeSession session) {
        sessionsByPlayer.remove(session.getPlayerA());
        sessionsByPlayer.remove(session.getPlayerB());
    }

    public void createRequest(UUID target, UUID requester) {
        pendingRequests.put(target, requester);
    }

    public UUID takeRequest(UUID target) {
        return pendingRequests.remove(target);
    }

    public void cancelPending(UUID uuid) {
        pendingRequests.remove(uuid);
        pendingRequests.values().remove(uuid);
    }
}
