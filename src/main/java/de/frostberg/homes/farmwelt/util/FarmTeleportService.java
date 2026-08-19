package de.frostberg.homes.farmwelt.util;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.farmwelt.FarmType;
import de.frostberg.homes.util.MessageUtil;
import de.frostberg.homes.util.SafeTeleport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Wuerfelt eine zufaellige, sichere Position innerhalb des konfigurierten
 * Radius um den per /setfarmwelt gesetzten Mittelpunkt einer Farmwelt und
 * teleportiert dorthin. Ausgelagert aus dem alten SpawnCommand, damit sowohl
 * der Direkt-Teleport-Shortcut ("/farmwelt <typ>") als auch das GUI dieselbe
 * Logik fuer alle drei Farmwelten (Overworld/Nether/End) nutzen.
 */
public final class FarmTeleportService {

    private static final int MAX_ATTEMPTS = 10;

    // Verhindert, dass die Sicher-Positions-Suche im Nether bis zur duennen
    // Luft-Tasche direkt unter der Bedrock-Decke hochscannt (siehe
    // SafeTeleport#findSafeLocation) - 120 liegt sicher unter der ueblichen
    // Nether-Decke bei Y=127.
    private static final int NETHER_MAX_SEARCH_Y = 120;

    private FarmTeleportService() {
    }

    /** Startet den Teleport-Versuch. Sendet bei fehlender/nicht geladener Welt selbst eine Fehlermeldung. */
    public static void teleport(FrostbergHomes plugin, Player player, FarmType type) {
        String prefix = "settings.farm-worlds." + type.getConfigKey() + ".";
        String worldName = plugin.getConfig().getString(prefix + "world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-not-loaded"));
            return;
        }

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-searching"));
        attempt(plugin, player, type, world, world.getSpawnLocation(), 0);
    }

    private static void attempt(FrostbergHomes plugin, Player player, FarmType type, World world, Location center, int attemptNumber) {
        if (!player.isOnline()) {
            return;
        }

        if (attemptNumber >= MAX_ATTEMPTS) {
            player.teleport(center);
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-success"));
            return;
        }

        String prefix = "settings.farm-worlds." + type.getConfigKey() + ".";
        int radius = Math.max(1, plugin.getConfig().getInt(prefix + "radius", 4000));
        int minRadius = Math.max(0, Math.min(plugin.getConfig().getInt(prefix + "min-radius", 2000), radius - 1));

        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = minRadius + ThreadLocalRandom.current().nextDouble() * (radius - minRadius);

        int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    // Der Mittelpunkt (world.getSpawnLocation()) liegt oft weit entfernt
                    // von x/z und seine Hoehe sagt nichts ueber das Gelaende an der
                    // gewuerfelten Position aus. Im Nether gibt es keine "hoechster
                    // Block"-Abfrage, die funktioniert (die Bedrock-Decke waere der
                    // hoechste Block) - dort wird stattdessen knapp unter der Decke
                    // gestartet und nach unten gesucht. In Overworld/End liefert
                    // getHighestBlockYAt() die tatsaechliche Gelaendeoberflaeche an
                    // dieser Spalte, unabhaengig von Bergen/Taelern.
                    int probeY = type == FarmType.NETHER ? NETHER_MAX_SEARCH_Y : world.getHighestBlockYAt(x, z);
                    Location probe = new Location(world, x, probeY, z);
                    Location safe = type == FarmType.NETHER
                            ? SafeTeleport.findSafeLocation(probe, NETHER_MAX_SEARCH_Y)
                            : SafeTeleport.findSafeLocation(probe);

                    if (safe != null) {
                        player.teleport(safe);
                        player.sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-success"));
                    } else {
                        attempt(plugin, player, type, world, center, attemptNumber + 1);
                    }
                }));
    }
}
