package de.frostberg.homes.stats.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /stats [spieler] - oeffnet die Stats-GUI mit Spielzeit, Wallet- und Bankguthaben auf einen Blick. */
public class StatsCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public StatsCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0) {
            target = player;
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer resolved = Bukkit.getOfflinePlayer(args[0]);
            if (!resolved.hasPlayedBefore() && !resolved.isOnline()) {
                sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[0]));
                return true;
            }
            target = resolved;
        }

        plugin.getStatsGuiListener().open(player, target);
        return true;
    }
}
