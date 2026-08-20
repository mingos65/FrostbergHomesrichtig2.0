package de.frostberg.homes.util;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kleine, wiederverwendbare Chat-Eingabe-Abfrage: eine GUI kann sich
 * schliessen, den Spieler um eine Texteingabe im normalen Chat bitten
 * (man kann nicht gleichzeitig ein Inventar offen haben UND in den Chat
 * tippen) und einen Callback registrieren, der die naechste Chat-
 * Nachricht dieses Spielers abfaengt (wird NICHT oeffentlich gesendet)
 * und verarbeitet. Genutzt von Bank und Handel fuer Betrags-Eingaben.
 */
public class ChatInputManager {

    /** Wird mit der eingegebenen Zeile aufgerufen, sobald der Spieler im Chat antwortet. */
    public interface InputHandler {
        void handle(Player player, String input);
    }

    private final Map<UUID, InputHandler> pending = new ConcurrentHashMap<>();

    public void awaitInput(Player player, InputHandler handler) {
        pending.put(player.getUniqueId(), handler);
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public InputHandler take(UUID uuid) {
        return pending.remove(uuid);
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }
}
