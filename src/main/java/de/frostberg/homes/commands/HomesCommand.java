package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /homes         -> oeffnet dasselbe GUI wie /home (siehe HomesGuiListener)
 * /homes reload  -> laedt config.yml UND messages.yml neu (benoetigt homes.reload)
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
                sender.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
                return true;
            }
            plugin.reloadConfig();
            plugin.reloadMessages();
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "reload-success"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        plugin.getHomesGuiListener().openMenu(player);
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
