package de.frostberg.homes.util;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Faengt die naechste Chat-Nachricht eines Spielers ab, wenn eine
 * ChatInputManager-Abfrage fuer ihn offen ist (z.B. Bank-/Handel-
 * Betragseingabe) - MUSS als erster Chat-Listener registriert werden
 * (LOWEST-Prioritaet, vor ChatModeListener/ChatFormatListener in
 * FrostbergHomes#registerListeners()), damit eine Betragseingabe nicht
 * versehentlich als Team-/Adminchat-Nachricht oder eingefaerbter
 * Chat-Text behandelt wird.
 */
public class ChatInputListener implements Listener {

    private final FrostbergHomes plugin;

    public ChatInputListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInputManager().hasPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        ChatInputManager.InputHandler handler = plugin.getChatInputManager().take(player.getUniqueId());
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handler.handle(player, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getChatInputManager().cancel(event.getPlayer().getUniqueId());
    }
}
