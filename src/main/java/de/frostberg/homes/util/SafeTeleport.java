package de.frostberg.homes.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Sucht ausgehend von einer Ziel-Location eine sichere Landeposition (zwei
 * freie, ungefaehrliche Bloecke ueber festem, ungefaehrlichem Untergrund).
 * Urspruenglich Teil von HomeCommand, hierher ausgelagert, damit /farmwelt
 * (zufaellige Position in der Farmwelt, siehe SpawnCommand) dieselbe Suche
 * wiederverwenden kann statt sie zu duplizieren.
 */
public final class SafeTeleport {

    private SafeTeleport() {
    }

    /** Wie {@link #findSafeLocation(Location, int)}, ohne zusaetzliche Hoehenbegrenzung (nutzt die volle Welthoehe). */
    public static Location findSafeLocation(Location location) {
        return findSafeLocation(location, Integer.MAX_VALUE);
    }

    /**
     * Sucht zuerst ab der Hoehe von {@code location} nach oben, dann nach
     * unten, nach der naechsten sicheren Position auf derselben X/Z-Spalte.
     * Gibt null zurueck, wenn in der ganzen Spalte keine gefunden wurde.
     *
     * {@code maxSearchY} begrenzt die Aufwaerts-Suche zusaetzlich zur
     * natuerlichen Welthoehe - wichtig im Nether, wo die Aufwaerts-Suche
     * sonst haeufig zuerst die duenne Luft-Tasche direkt unter der
     * Bedrock-Decke findet (der Untergrund ist dort meist durchgehend
     * massiv), statt einer sinnvollen Position naeher am Nether-Boden.
     */
    public static Location findSafeLocation(Location location, int maxSearchY) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        int x = location.getBlockX();
        int z = location.getBlockZ();

        int minY = world.getMinHeight();
        int maxY = Math.min(world.getMaxHeight() - 2, maxSearchY);
        int clampedStart = Math.max(minY, Math.min(location.getBlockY(), maxY));

        for (int y = clampedStart; y <= maxY; y++) {
            if (isSafe(world, x, y, z)) {
                return centered(world, x, y, z, location.getYaw(), location.getPitch());
            }
        }

        for (int y = clampedStart - 1; y >= minY; y--) {
            if (isSafe(world, x, y, z)) {
                return centered(world, x, y, z, location.getYaw(), location.getPitch());
            }
        }

        return null;
    }

    private static boolean isSafe(World world, int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        return isPassable(feet.getType()) && isPassable(head.getType())
                && ground.getType().isSolid() && !isHarmful(ground.getType());
    }

    private static boolean isPassable(Material material) {
        return !material.isSolid() && !isHarmful(material);
    }

    private static boolean isHarmful(Material material) {
        return material == Material.LAVA
                || material == Material.WATER
                || material == Material.FIRE
                || material == Material.SOUL_FIRE
                || material == Material.MAGMA_BLOCK
                || material == Material.CACTUS
                || material == Material.SWEET_BERRY_BUSH
                || material == Material.POWDER_SNOW;
    }

    private static Location centered(World world, int x, int y, int z, float yaw, float pitch) {
        return new Location(world, x + 0.5, y, z + 0.5, yaw, pitch);
    }
}
