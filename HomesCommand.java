package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.Home;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /homes         -> farbenfrohe Liste aller eigenen Homes
 * /homes reload  -> laedt die config.yml neu (benoetigt homes.reload)
 *
 * Die Basis-Permission (homes.list) fuer /homes wird bereits automatisch ueber
 * plugin.yml geprueft. homes.reload wird zusaetzlich manuell geprueft, da es
 * eine Unterberechtigung fuer das Subcommand "reload" ist.
 */
public class HomesCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public HomesCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("homes.reload")) {
                sender.sendMessage(MessageUtil.get(plugin.getConfig(), "no-permission"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "reload-success"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        Map<Integer, Home> homes = plugin.getHomeManager().getHomes(player.getUniqueId());

        if (homes.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "no-homes"));
            return true;
        }

        player.sendMessage(MessageUtil.get(plugin.getConfig(), "homes-list-header"));

        for (Home home : homes.values()) {
            String line = MessageUtil.get(plugin.getConfig(), "homes-list-entry")
                    .replace("%nr%", String.valueOf(home.getNumber()))
                    .replace("%world%", home.getWorldName())
                    .replace("%x%", String.valueOf(Math.round(home.getX())))
                    .replace("%y%", String.valueOf(Math.round(home.getY())))
                    .replace("%z%", String.valueOf(Math.round(home.getZ())));
            player.sendMessage(line);
        }

        int limit = plugin.getHomeManager().getHomeLimit(player);
        String limitDisplay = (limit == Integer.MAX_VALUE) ? "unbegrenzt" : String.valueOf(limit);

        player.sendMessage(MessageUtil.get(plugin.getConfig(), "homes-list-footer")
                .replace("%count%", String.valueOf(homes.size()))
                .replace("%limit%", limitDisplay));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("homes.reload")) {
            return filter(Collections.singletonList("reload"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
