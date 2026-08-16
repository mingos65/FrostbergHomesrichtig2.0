package de.frostberg.homes.clan.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /cc <nachricht> - kurzer Alias fuer /clan chat <nachricht>.
 */
public class ClanChatCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public ClanChatCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        plugin.getClanCommand().handleChat(player, args, 0);
        return true;
    }
}
