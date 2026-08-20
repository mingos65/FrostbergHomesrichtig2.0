package de.frostberg.homes.chat;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

/**
 * Zwei Aufgaben, beide muessen VOR FancyChat laufen (daher LOWEST) und
 * greifen direkt am rohen Nachrichtentext an, nicht ueber Platzhalter:
 *
 * 1) Verlauf-Chatfarbe: faerbt die Nachricht selbst Zeichen fuer Zeichen ein.
 *    Solide Farben/Hex laufen dagegen NICHT hier, sondern ueber die
 *    %frostbergchat_color%-Platzhalter (ChatColorPlaceholderExpansion), da
 *    dort kein Eingriff in den Nachrichtentext noetig ist - nur bei einem
 *    Verlauf braucht es Zugriff auf jedes einzelne Zeichen.
 * 2) Team-Leerzeile: Spieler mit frostbergchat.teamline (Mod+) bekommen vor
 *    ihrer Nachricht eine leere Zeile an den ganzen Server gesendet, als
 *    optische Abtrennung im Chat.
 */
public class ChatFormatListener implements Listener {

    private final FrostbergHomes plugin;

    public ChatFormatListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            // ChatModeListener (Team-/Adminchat) hat die Nachricht bereits
            // umgeleitet - nicht zusaetzlich als "normale" Nachricht formatieren.
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ChatColorManager colorManager = plugin.getChatColorManager();

        String code = colorManager.getColor(uuid);
        if (code != null && code.startsWith("gradient:") && player.hasPermission("frostbergchat.color.rgb")) {
            String[] parts = code.substring("gradient:".length()).split(":");
            if (parts.length == 2 && ColorUtil.isValidHex(parts[0]) && ColorUtil.isValidHex(parts[1])) {
                boolean bold = colorManager.isBold(uuid) && player.hasPermission("frostbergchat.color.bold");
                event.setMessage(ColorUtil.gradient(event.getMessage(), parts[0], parts[1], bold));
            }
        }

        if (player.hasPermission("frostbergchat.teamline")) {
            // AsyncPlayerChatEvent laeuft nicht im Hauptthread - Broadcast
            // deshalb auf den Hauptthread verschieben, statt direkt hier zu senden.
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(" ");
                }
            });
        }
    }
}
