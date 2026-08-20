package de.frostberg.homes.staff.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /cw - schaltet den Command-Watcher-Live-Feed fuer den ausfuehrenden Spieler an/aus. */
public class CommandWatcherCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public CommandWatcherCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        boolean enabled = plugin.getCommandWatcherManager().toggle(player);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), enabled ? "cw-enabled" : "cw-disabled"));
        return true;
    }
}
