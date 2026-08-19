package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /setspawn - setzt den Spawnpunkt der Hauptwelt auf die aktuelle Position.
 * Verlangt, dass der Admin tatsaechlich in der Spawn-Welt steht.
 *
 * Das frueher hier per "farm"-Flag mitgefuehrte /setfarmwelt lebt jetzt im
 * eigenstaendigen de.frostberg.homes.farmwelt-Package (SetFarmweltCommand,
 * ein Mittelpunkt je Farmwelt statt nur einer).
 */
public class SetSpawnCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public SetSpawnCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        String worldName = plugin.getConfig().getString("settings.spawn-world");
        World expectedWorld = worldName == null ? null : Bukkit.getWorld(worldName);
        if (expectedWorld == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "spawn-world-not-loaded"));
            return true;
        }

        if (!player.getWorld().equals(expectedWorld)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "spawn-wrong-world")
                    .replace("%world%", expectedWorld.getName()));
            return true;
        }

        expectedWorld.setSpawnLocation(player.getLocation());
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "spawn-set"));
        return true;
    }
}
