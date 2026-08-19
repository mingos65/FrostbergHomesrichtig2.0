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
 * /spawn teleportiert immer exakt zum gesetzten Spawnpunkt der Spawn-Welt.
 * Ersetzt die vorher per commands.yml auf Multiverse gemappten Aliase, damit
 * die Nachrichten im Frostberg-Stil (Prefix, Farben) statt in Multiverse's
 * Standardtexten kommen.
 *
 * Der frueher hier per "farm"-Flag mitgefuehrte /farmwelt-Zweig lebt jetzt
 * im eigenstaendigen de.frostberg.homes.farmwelt-Package (3 Farmwelten +
 * Auswahl-GUI statt nur einer festen Welt).
 */
public class SpawnCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public SpawnCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        String worldName = plugin.getConfig().getString("settings.spawn-world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "spawn-world-not-loaded"));
            return true;
        }

        player.teleport(world.getSpawnLocation());
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "spawn-success"));
        return true;
    }
}
