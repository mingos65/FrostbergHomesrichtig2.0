package de.frostberg.homes.clan.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reines Datenmodell eines Clans. Kennt keine Speicher-/Validierungslogik -
 * das passiert im ClanManager (analog zu Home/HomeManager).
 */
public class Clan {

    public enum Role {
        LEADER, MOD, MEMBER
    }

    private String name;
    private String tag;
    private String description;
    private final Map<UUID, Role> members = new LinkedHashMap<>();

    private String baseWorld;
    private double baseX;
    private double baseY;
    private double baseZ;
    private float baseYaw;
    private float basePitch;

    private long tokensBalance;
    private double goldBalance;
    private long createdAt;

    /** Fuer den ClanManager beim Laden aus der YAML - Mitglieder werden danach ueber getMembers() befuellt. */
    public Clan(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    /** Fuer die Neu-Erstellung eines Clans - Gruender wird direkt als Leader eingetragen. */
    public Clan(String name, String tag, UUID leaderUuid) {
        this(name);
        this.tag = tag;
        this.members.put(leaderUuid, Role.LEADER);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<UUID, Role> getMembers() {
        return members;
    }

    public Role getRole(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public UUID getLeaderUuid() {
        for (Map.Entry<UUID, Role> entry : members.entrySet()) {
            if (entry.getValue() == Role.LEADER) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean hasBase() {
        return baseWorld != null;
    }

    /** Gibt null zurueck, wenn keine Base gesetzt ist oder deren Welt aktuell nicht geladen ist. */
    public Location getBaseLocation() {
        if (baseWorld == null) {
            return null;
        }
        World world = Bukkit.getWorld(baseWorld);
        if (world == null) {
            return null;
        }
        return new Location(world, baseX, baseY, baseZ, baseYaw, basePitch);
    }

    public void setBase(Location location) {
        World world = location.getWorld();
        this.baseWorld = world != null ? world.getName() : null;
        this.baseX = location.getX();
        this.baseY = location.getY();
        this.baseZ = location.getZ();
        this.baseYaw = location.getYaw();
        this.basePitch = location.getPitch();
    }

    public String getBaseWorld() {
        return baseWorld;
    }

    public void setBaseWorld(String baseWorld) {
        this.baseWorld = baseWorld;
    }

    public double getBaseX() {
        return baseX;
    }

    public void setBaseX(double baseX) {
        this.baseX = baseX;
    }

    public double getBaseY() {
        return baseY;
    }

    public void setBaseY(double baseY) {
        this.baseY = baseY;
    }

    public double getBaseZ() {
        return baseZ;
    }

    public void setBaseZ(double baseZ) {
        this.baseZ = baseZ;
    }

    public float getBaseYaw() {
        return baseYaw;
    }

    public void setBaseYaw(float baseYaw) {
        this.baseYaw = baseYaw;
    }

    public float getBasePitch() {
        return basePitch;
    }

    public void setBasePitch(float basePitch) {
        this.basePitch = basePitch;
    }

    public long getTokensBalance() {
        return tokensBalance;
    }

    public void setTokensBalance(long tokensBalance) {
        this.tokensBalance = tokensBalance;
    }

    public double getGoldBalance() {
        return goldBalance;
    }

    public void setGoldBalance(double goldBalance) {
        this.goldBalance = goldBalance;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
