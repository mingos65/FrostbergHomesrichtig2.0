package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /spawn und /farmwelt - sofortiger Teleport zum Spawnpunkt der Hauptwelt
 * bzw. der Farmwelt. Beide Befehle teilen sich diese Klasse (per "farm"-Flag
 * im Konstruktor), analog zu TpaCommand/AdminTpCommand. Ersetzt die vorher
 * per commands.yml auf Multiverse gemappten Aliase, damit die Nachrichten im
 * Frostberg-Stil (Prefix, Farben) statt in Multiverse's Standardtexten kommen.
 */
public class SpawnCommand implements CommandExecutor {

    private final FrostbergHomes plugin;
    private final boolean farm;

    public SpawnCommand(FrostbergHomes plugin, boolean farm) {
        this.plugin = plugin;
        this.farm = farm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        World world = farm ? Bukkit.getWorld("farm") : Bukkit.getWorlds().get(0);
        if (world == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "farmwelt-not-loaded"));
            return true;
        }

        Location target = world.getSpawnLocation();
        player.teleport(target);

        player.sendMessage(MessageUtil.get(plugin.getConfig(), farm ? "farmwelt-success" : "spawn-success"));
        return true;
    }
}
