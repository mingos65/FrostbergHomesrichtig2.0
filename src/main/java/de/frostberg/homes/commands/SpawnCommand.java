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
 * zwischen settings.farm-teleport-min-radius und -radius um den per
 * /setfarmwelt gesetzten Mittelpunkt - damit sich Spieler in der Farmwelt
 * verteilen statt sich immer an derselben Stelle zu stapeln. Da der Radius im
 * Bereich mehrerer tausend Bloecke liegt, ist die Ziel-Chunk oft noch nicht
 * generiert - das Laden passiert deshalb asynchron (World#getChunkAtAsync),
 * damit der Server dabei nicht kurz einfriert.
 */
public class SpawnCommand implements CommandExecutor {

    private static final int MAX_ATTEMPTS = 10;

    private final FrostbergHomes plugin;
    private final boolean farm;

    public SpawnCommand(FrostbergHomes plugin, boolean farm) {
        this.plugin = plugin;
        this.farm = farm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        String worldName = plugin.getConfig().getString(farm ? "settings.farm-world" : "settings.spawn-world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), farm ? "farmwelt-not-loaded" : "spawn-world-not-loaded"));
            return true;
        }

        if (!farm) {
            player.teleport(world.getSpawnLocation());
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "spawn-success"));
            return true;
        }

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-searching"));
        attemptRandomFarmTeleport(player, world, world.getSpawnLocation(), 0);
        return true;
    }

    /**
     * Wuerfelt eine Position zwischen min- und maximalem Radius um {@code center},
     * laedt deren Chunk asynchron und sucht dort per SafeTeleport eine sichere
     * Landestelle. Klappt das nicht, wird erneut gewuerfelt (bis MAX_ATTEMPTS);
     * danach als Rueckfallebene der exakte Mittelpunkt genutzt. Der Spieler
     * kann in der Zwischenzeit den Server verlassen - das wird vor jedem
     * Teleport-Versuch geprueft.
     */
    private void attemptRandomFarmTeleport(Player player, World world, Location center, int attempt) {
        if (!player.isOnline()) {
            return;
        }

        if (attempt >= MAX_ATTEMPTS) {
            player.teleport(center);
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-success"));
            return;
        }

        int radius = Math.max(1, plugin.getConfig().getInt("settings.farm-teleport-radius", 4000));
        int minRadius = Math.max(0, Math.min(plugin.getConfig().getInt("settings.farm-teleport-min-radius", 2000), radius - 1));

        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = minRadius + ThreadLocalRandom.current().nextDouble() * (radius - minRadius);

        int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    Location probe = new Location(world, x, center.getY(), z);
                    Location safe = SafeTeleport.findSafeLocation(probe);

                    if (safe != null) {
                        player.teleport(safe);
                        player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-success"));
                    } else {
                        attemptRandomFarmTeleport(player, world, center, attempt + 1);
                    }
                }));
    }
}
