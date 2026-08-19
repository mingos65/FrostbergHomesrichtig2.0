package de.frostberg.homes.farmwelt.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.farmwelt.FarmType;
import de.frostberg.homes.farmwelt.util.FarmTeleportService;
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
 * /farmwelt oeffnet ohne Argument das Auswahl-GUI (Overworld/Nether/End).
 * /farmwelt <overworld|nether|end> teleportiert direkt, als Shortcut fuer
 * Spieler, die den alten Ein-Welt-Workflow gewohnt sind.
 */
public class FarmweltCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public FarmweltCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length == 0) {
            plugin.getFarmweltGuiListener().open(player);
            return true;
        }

        FarmType type = FarmType.fromConfigKey(args[0]);
        if (type == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-invalid-type"));
            return true;
        }

        FarmTeleportService.teleport(plugin, player, type);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> options = new ArrayList<>();
        for (FarmType type : FarmType.values()) {
            if (type.getConfigKey().startsWith(args[0].toLowerCase())) {
                options.add(type.getConfigKey());
            }
        }
        return options;
    }
}
