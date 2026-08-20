package de.frostberg.homes.chat.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.chat.ChatModeManager;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /adminchat, /ac - schaltet den Admin-Chat-Modus fuer den Spieler um. */
public class AdminChatCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public AdminChatCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        ChatModeManager.Mode mode = plugin.getChatModeManager().toggleAdmin(player);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), mode == ChatModeManager.Mode.ADMIN ? "adminchat-on" : "adminchat-off"));
        return true;
    }
}
