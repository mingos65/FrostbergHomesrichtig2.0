package de.frostberg.homes.stats.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /spielzeit [spieler] - zeigt nur die Spielzeit an. */
public class SpielzeitCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public SpielzeitCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
                return true;
            }
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

        String name = target.getName() != null ? target.getName() : plugin.getPlaytimeManager().getStoredName(target.getUniqueId(), "?");
        String time = plugin.getPlaytimeManager().format(plugin.getPlaytimeManager().getTotalSeconds(target.getUniqueId()));
        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "spielzeit-line")
                .replace("%player%", name)
                .replace("%time%", time));
        return true;
    }
}
