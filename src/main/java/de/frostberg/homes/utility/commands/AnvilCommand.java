package de.frostberg.homes.utility.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /anvil - oeffnet einen virtuellen Amboss, ohne dass ein echter in der Naehe stehen muss. */
public class AnvilCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public AnvilCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }
        player.openAnvil(null, true);
        return true;
    }
}
