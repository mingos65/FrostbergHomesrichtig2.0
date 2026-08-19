package de.frostberg.homes.farmwelt.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.farmwelt.FarmType;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /setfarmwelt <overworld|nether|end> - setzt den Mittelpunkt der
 * zufaelligen Farm-Teleports fuer die angegebene Farmwelt auf die aktuelle
 * Position (Spieler muss dort auch tatsaechlich stehen).
 */
public class SetFarmweltCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public SetFarmweltCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-usage-setfarmwelt"));
            return true;
        }

        FarmType type = FarmType.fromConfigKey(args[0]);
        if (type == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-invalid-type"));
            return true;
        }

        String worldName = plugin.getConfig().getString("settings.farm-worlds." + type.getConfigKey() + ".world");
        World expectedWorld = worldName == null ? null : Bukkit.getWorld(worldName);
        if (expectedWorld == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-not-loaded"));
            return true;
        }

        if (!player.getWorld().equals(expectedWorld)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-wrong-world")
                    .replace("%world%", expectedWorld.getName()));
            return true;
        }

        expectedWorld.setSpawnLocation(player.getLocation());
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-set"));
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
