package de.frostberg.homes.chat;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Faengt Nachrichten von Spielern im Team-/Admin-Chat-Modus (/tc, /ac) ab
 * und leitet sie nur an die passende Gruppe weiter statt in den
 * oeffentlichen Chat. Gleiche Prioritaet (LOWEST) wie ChatFormatListener,
 * aber MUSS bei der Registrierung in FrostbergHomes#registerListeners()
 * VOR ChatFormatListener stehen (Bukkit fuehrt bei gleicher Prioritaet in
 * Registrierreihenfolge aus) - ChatFormatListener bricht selbst zusaetzlich
 * bei event.isCancelled() ab, damit z.B. die Team-Leerzeile nicht trotzdem
 * an den ganzen Server geht.
 */
public class ChatModeListener implements Listener {

    private final FrostbergHomes plugin;

    public ChatModeListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatModeManager.Mode mode = plugin.getChatModeManager().getMode(player);
        if (mode == ChatModeManager.Mode.NONE) {
            return;
        }

        event.setCancelled(true);

        String permission = mode == ChatModeManager.Mode.ADMIN ? "chat.adminchat" : "chat.teamchat";
        String formatKey = mode == ChatModeManager.Mode.ADMIN ? "adminchat-format" : "teamchat-format";
        String formatted = MessageUtil.get(plugin.getMessages(), formatKey)
                .replace("%player%", player.getName())
                .replace("%message%", event.getMessage());

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player recipient : Bukkit.getOnlinePlayers()) {
                if (recipient.hasPermission(permission)) {
                    recipient.sendMessage(formatted);
                }
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getChatModeManager().remove(event.getPlayer().getUniqueId());
    }
}
