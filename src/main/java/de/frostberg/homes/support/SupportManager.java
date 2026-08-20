package de.frostberg.homes.support;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Haelt offene Support-Anfragen im Speicher (kein Neustart-Ueberdauern
 * noetig - eine Anfrage soll sowieso zeitnah waehrend derselben Session
 * bearbeitet werden). Eine Anfrage ist ueber die UUID des anfragenden
 * Spielers indiziert, "claimedBy" ist gesetzt, sobald ein Supporter sie per
 * /support accept <spieler> uebernommen hat.
 */
public class SupportManager {

    public static class Ticket {
        private final UUID requester;
        private final String message;
        private UUID claimedBy;

        Ticket(UUID requester, String message) {
            this.requester = requester;
            this.message = message;
        }

        public UUID getRequester() {
            return requester;
        }

        public String getMessage() {
            return message;
        }

        public UUID getClaimedBy() {
            return claimedBy;
        }

        public void setClaimedBy(UUID claimedBy) {
            this.claimedBy = claimedBy;
        }
    }

    private final Map<UUID, Ticket> ticketsByRequester = new ConcurrentHashMap<>();

    public boolean hasActiveTicket(UUID requester) {
        return ticketsByRequester.containsKey(requester);
    }

    public Ticket createTicket(UUID requester, String message) {
        Ticket ticket = new Ticket(requester, message);
        ticketsByRequester.put(requester, ticket);
        return ticket;
    }

    public Ticket getTicketByRequester(UUID requester) {
        return ticketsByRequester.get(requester);
    }

    /** Findet das Ticket, an dem "uuid" gerade beteiligt ist - entweder als Anfragender oder als der Supporter, der es angenommen hat. */
    public Ticket findTicketFor(UUID uuid) {
        Ticket own = ticketsByRequester.get(uuid);
        if (own != null) {
            return own;
        }
        for (Ticket ticket : ticketsByRequester.values()) {
            if (uuid.equals(ticket.getClaimedBy())) {
                return ticket;
            }
        }
        return null;
    }

    public void close(UUID requester) {
        ticketsByRequester.remove(requester);
    }

    /** Beim Quit: eigenes Ticket als Anfragender entfernen, als Supporter beteiligtes Ticket wieder freigeben statt zu loeschen. */
    public void removePlayer(UUID uuid) {
        ticketsByRequester.remove(uuid);
        for (Ticket ticket : ticketsByRequester.values()) {
            if (uuid.equals(ticket.getClaimedBy())) {
                ticket.setClaimedBy(null);
            }
        }
    }
}
