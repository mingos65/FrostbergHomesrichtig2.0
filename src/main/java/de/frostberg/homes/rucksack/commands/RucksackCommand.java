package de.frostberg.homes.rucksack.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rs, /rucksack - oeffnet den eigenen Extra-Rucksack. */
public class RucksackCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public RucksackCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        plugin.getRucksackGuiListener().open(player);
        return true;
    }
}
