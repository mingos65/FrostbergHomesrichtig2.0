package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /sethome <name> - Text-Alternative zu /set home [nr]: legt ein Home im
 * naechsten freien Slot an und speichert den angegebenen Text direkt als
 * Anzeigename (siehe Home#getName, genutzt vom /homes-GUI).
 */
public class SetHomeByNameCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public SetHomeByNameCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "usage-sethome"));
            return true;
        }

        String name = String.join(" ", args);
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }

        int limit = Math.min(plugin.getHomeManager().getHomeLimit(player), 14);
        int nextNumber = -1;
        for (int i = 1; i <= limit; i++) {
            if (!plugin.getHomeManager().hasHome(player.getUniqueId(), i)) {
                nextNumber = i;
                break;
            }
        }

        if (nextNumber == -1) {
            String limitDisplay = plugin.getHomeManager().getHomeLimit(player) == Integer.MAX_VALUE
                    ? "unbegrenzt" : String.valueOf(limit);
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "home-limit-reached")
                    .replace("%limit%", limitDisplay));
            return true;
        }

        plugin.getHomeManager().setHome(player, nextNumber, player.getLocation());
        plugin.getHomeManager().renameHome(player.getUniqueId(), nextNumber, name);

        player.sendMessage(MessageUtil.get(plugin.getConfig(), "home-set-named")
                .replace("%name%", MessageUtil.color(name))
                .replace("%nr%", String.valueOf(nextNumber)));
        return true;
    }
}
