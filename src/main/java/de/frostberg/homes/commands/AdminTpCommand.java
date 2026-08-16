package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /tp <spieler> und /tphere <spieler> - sofortiger Admin-Teleport ohne
 * Anfrage, Countdown oder Cooldown (nur mit tpa.admin-Permission). Beide
 * Befehle teilen sich diese Klasse (per "here"-Flag im Konstruktor), analog
 * zu TpaCommand.
 */
public class AdminTpCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;
    private final boolean here;

    public AdminTpCommand(FrostbergHomes plugin, boolean here) {
        this.plugin = plugin;
        this.here = here;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), here ? "tphere-usage" : "tp-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found")
                    .replace("%player%", args[0]));
            return true;
        }

        Player teleportingPlayer = here ? target : player;
        Player destinationPlayer = here ? player : target;

        teleportingPlayer.teleport(destinationPlayer.getLocation());

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "tp-success")
                .replace("%player%", target.getName()));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[0]);
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
