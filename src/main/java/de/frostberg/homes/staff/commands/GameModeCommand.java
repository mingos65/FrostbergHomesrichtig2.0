package de.frostberg.homes.staff.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /gm 0-3 - schneller Gamemode-Wechsel fuer die eigene Person. */
public class GameModeCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public GameModeCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gamemode-usage"));
            return true;
        }

        GameMode mode = switch (args[0]) {
            case "0" -> GameMode.SURVIVAL;
            case "1" -> GameMode.CREATIVE;
            case "2" -> GameMode.ADVENTURE;
            case "3" -> GameMode.SPECTATOR;
            default -> null;
        };

        if (mode == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gamemode-usage"));
            return true;
        }

        player.setGameMode(mode);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "gamemode-set")
                .replace("%mode%", MessageUtil.get(plugin.getMessages(), "gamemode-name-" + mode.name().toLowerCase())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (String option : new String[]{"0", "1", "2", "3"}) {
                if (option.startsWith(args[0])) {
                    result.add(option);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
