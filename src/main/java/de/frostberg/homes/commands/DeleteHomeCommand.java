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
 * /delete home [nr]
 * Die Basis-Permission (homes.delete) wird bereits automatisch ueber plugin.yml
 * geprueft, bevor onCommand ueberhaupt aufgerufen wird.
 */
public class DeleteHomeCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public DeleteHomeCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("home")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "usage-delete"));
            return true;
        }

        int number = 1;
        if (args.length >= 2) {
            try {
                number = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "invalid-number"));
                return true;
            }
        }

        boolean deleted = plugin.getHomeManager().deleteHome(player.getUniqueId(), number);

        if (!deleted) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "home-not-found")
                    .replace("%nr%", String.valueOf(number)));
            return true;
        }

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "home-deleted")
                .replace("%nr%", String.valueOf(number)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Collections.singletonList("home"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("home") && sender instanceof Player player) {
            Map<Integer, Home> homes = plugin.getHomeManager().getHomes(player.getUniqueId());
            List<String> numbers = new ArrayList<>();
            for (int nr : homes.keySet()) {
                numbers.add(String.valueOf(nr));
            }
            return filter(numbers, args[1]);
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
