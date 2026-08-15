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
 * /set home [nr]
 * Die Basis-Permission (homes.set) wird bereits automatisch ueber plugin.yml
 * geprueft, bevor onCommand ueberhaupt aufgerufen wird.
 */
public class SetHomeCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public SetHomeCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("home")) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "usage-set"));
            return true;
        }

        int number = 1;
        if (args.length >= 2) {
            try {
                number = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtil.get(plugin.getConfig(), "invalid-number"));
                return true;
            }
        }

        if (number < 1) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "invalid-number"));
            return true;
        }

        boolean alreadyExists = plugin.getHomeManager().hasHome(player.getUniqueId(), number);

        // Limit nur pruefen, wenn dadurch tatsaechlich ein NEUES Home entsteht -
        // Ueberschreiben einer vorhandenen Nummer ist immer erlaubt (siehe Konzept).
        if (!alreadyExists) {
            int limit = plugin.getHomeManager().getHomeLimit(player);
            int current = plugin.getHomeManager().getHomeCount(player.getUniqueId());

            if (current >= limit) {
                player.sendMessage(MessageUtil.get(plugin.getConfig(), "home-limit-reached")
                        .replace("%limit%", limitDisplay(limit)));
                return true;
            }
        }

        boolean overwritten = plugin.getHomeManager().setHome(player, number, player.getLocation());

        String messageKey = overwritten ? "home-overwritten" : "home-set";
        player.sendMessage(MessageUtil.get(plugin.getConfig(), messageKey)
                .replace("%nr%", String.valueOf(number)));

        return true;
    }

    private String limitDisplay(int limit) {
        return limit == Integer.MAX_VALUE ? "unbegrenzt" : String.valueOf(limit);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Collections.singletonList("home"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("home") && sender instanceof Player player) {
            int limit = plugin.getHomeManager().getHomeLimit(player);
            int currentCount = plugin.getHomeManager().getHomeCount(player.getUniqueId());
            int upper = (limit == Integer.MAX_VALUE) ? currentCount + 1 : limit;

            List<String> numbers = new ArrayList<>();
            for (int i = 1; i <= Math.max(upper, 1); i++) {
                numbers.add(String.valueOf(i));
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
