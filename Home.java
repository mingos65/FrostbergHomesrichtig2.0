package de.frostberg.homes.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Reines Datenmodell fuer ein einzelnes Home eines Spielers.
 * Kennt keine Spieler-UUID - die Zuordnung zum Spieler passiert im HomeManager
 * (dort als Map<Integer, Home> pro UUID).
 */
public class Home {

    private final int number;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public Home(int number, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.number = number;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Erstellt ein Home aus einer Bukkit-Location. Nummer wird separat uebergeben,
     * da sie vom HomeManager (naechste freie/gewuenschte Nummer) vorgegeben wird.
     */
    public static Home fromLocation(int number, Location location) {
        World world = location.getWorld();
        return new Home(
                number,
                world != null ? world.getName() : "world",
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public int getNumber() {
        return number;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * Wandelt dieses Home in eine Bukkit-Location um.
     * Gibt null zurueck, wenn die zugehoerige Welt aktuell nicht geladen ist
     * (z.B. weil eine Multiverse-Welt entladen wurde).
     */
    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Prueft, ob die zu diesem Home gehoerende Welt aktuell geladen ist,
     * ohne dabei eine Location zu erzeugen.
     */
    public boolean isWorldLoaded() {
        return Bukkit.getWorld(worldName) != null;
    }
}
