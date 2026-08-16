package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import de.frostberg.homes.util.SafeTeleport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * /spawn und /farmwelt. Beide Befehle teilen sich diese Klasse (per
 * "farm"-Flag im Konstruktor), analog zu TpaCommand/AdminTpCommand. Ersetzt
 * die vorher per commands.yml auf Multiverse gemappten Aliase, damit die
 * Nachrichten im Frostberg-Stil (Prefix, Farben) statt in Multiverse's
 * Standardtexten kommen.
 *
 * /spawn teleportiert immer exakt zum gesetzten Spawnpunkt der Spawn-Welt.
 * /farmwelt teleportiert dagegen zu einer zufaelligen, sicheren Position
 * innerhalb von settings.farm-teleport-radius um den per /setfarmwelt
 * gesetzten Mittelpunkt - damit sich Spieler in der Farmwelt verteilen statt
 * sich immer an derselben Stelle zu stapeln.
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

        String worldName = plugin.getConfig().getString(farm ? "settings.farm-world" : "settings.spawn-world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), farm ? "farmwelt-not-loaded" : "spawn-world-not-loaded"));
            return true;
        }

        Location target = farm ? findRandomFarmLocation(world) : world.getSpawnLocation();
        player.teleport(target);

        player.sendMessage(MessageUtil.get(plugin.getConfig(), farm ? "farmwelt-success" : "spawn-success"));
        return true;
    }

    /**
     * Wuerfelt bis zu 10 Positionen innerhalb des konfigurierten Radius um
     * den Farmwelt-Mittelpunkt und sucht dort jeweils per SafeTeleport eine
     * sichere Landestelle. Findet keiner der Versuche eine, wird als
     * Rueckfallebene der exakte Mittelpunkt zurueckgegeben.
     */
    private Location findRandomFarmLocation(World world) {
        Location center = world.getSpawnLocation();
        int radius = Math.max(1, plugin.getConfig().getInt("settings.farm-teleport-radius", 200));
        int minRadius = Math.min(plugin.getConfig().getInt("settings.farm-teleport-min-radius", 0), radius - 1);
        minRadius = Math.max(minRadius, 0);

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double distance = minRadius + ThreadLocalRandom.current().nextDouble() * (radius - minRadius);

            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);

            Location probe = new Location(world, x, center.getY(), z);
            Location safe = SafeTeleport.findSafeLocation(probe);
            if (safe != null) {
                return safe;
            }
        }

        return center;
    }
}
