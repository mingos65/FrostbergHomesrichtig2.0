package de.frostberg.homes.utility.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /werkbank - oeffnet eine virtuelle Werkbank, ohne dass eine echte in der Naehe stehen muss. */
public class WerkbankCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public WerkbankCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }
        player.openWorkbench(null, true);
        return true;
    }
}
