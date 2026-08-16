package de.frostberg.homes.clan.manager;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.clan.model.Clan;
import de.frostberg.homes.clan.model.ClanInvite;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Verwaltet alle Clans: Laden/Speichern als YAML unter
 * plugins/FrostbergHomes/clans/&lt;name&gt;.yml (ein File pro Clan, analog zu
 * den Homes-Playerdaten), Mitgliederverwaltung und offene Einladungen.
 * Anders als bei Homes werden alle Clans direkt beim Start geladen (nicht
 * lazy pro Spieler), da /clan list jederzeit alle Clans kennen muss.
 */
public class ClanManager {

    private final FrostbergHomes plugin;
    private final File clansFolder;

    // Clan-Name (klein geschrieben) -> Clan
    private final Map<String, Clan> clans = new LinkedHashMap<>();

    // Spieler-UUID -> Clan-Name (klein), Cache fuer schnelle Zuordnung
    private final Map<UUID, String> memberIndex = new HashMap<>();

    // Eingeladener Spieler -> aktuell offene Einladung an ihn
    private final Map<UUID, ClanInvite> pendingInvites = new HashMap<>();

    public ClanManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.clansFolder = new File(plugin.getDataFolder(), "clans");
        if (!clansFolder.exists()) {
            clansFolder.mkdirs();
        }
        loadAll();
    }

    // ---------------------------------------------------------------
    // Laden / Speichern
    // ---------------------------------------------------------------

    private void loadAll() {
        File[] files = clansFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            Clan clan = loadClan(file);
            if (clan == null) {
                continue;
            }
            String key = clan.getName().toLowerCase(Locale.ROOT);
            clans.put(key, clan);
            for (UUID uuid : clan.getMembers().keySet()) {
                memberIndex.put(uuid, key);
            }
        }
    }

    private Clan loadClan(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String name = yaml.getString("name");
        if (name == null) {
            return null;
        }

        Clan clan = new Clan(name);
        clan.setTag(yaml.getString("tag"));
        clan.setDescription(yaml.getString("description"));
        clan.setCreatedAt(yaml.getLong("created-at", System.currentTimeMillis()));
        clan.setTokensBalance(yaml.getLong("tokens-balance", 0));
        clan.setGoldBalance(yaml.getDouble("gold-balance", 0));

        if (yaml.contains("base.world")) {
            clan.setBaseWorld(yaml.getString("base.world"));
            clan.setBaseX(yaml.getDouble("base.x"));
            clan.setBaseY(yaml.getDouble("base.y"));
            clan.setBaseZ(yaml.getDouble("base.z"));
            clan.setBaseYaw((float) yaml.getDouble("base.yaw"));
            clan.setBasePitch((float) yaml.getDouble("base.pitch"));
        }

        ConfigurationSection membersSection = yaml.getConfigurationSection("members");
        if (membersSection != null) {
            for (String key : membersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    Clan.Role role = Clan.Role.valueOf(membersSection.getString(key, "MEMBER"));
                    clan.getMembers().put(uuid, role);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Ungueltiger Mitglied-Eintrag '" + key + "' in " + file.getName() + " - wird uebersprungen.");
                }
            }
        }

        return clan;
    }

    public void saveClan(Clan clan) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", clan.getName());
        yaml.set("tag", clan.getTag());
        yaml.set("description", clan.getDescription());
        yaml.set("created-at", clan.getCreatedAt());
        yaml.set("tokens-balance", clan.getTokensBalance());
        yaml.set("gold-balance", clan.getGoldBalance());

        if (clan.hasBase()) {
            yaml.set("base.world", clan.getBaseWorld());
            yaml.set("base.x", clan.getBaseX());
            yaml.set("base.y", clan.getBaseY());
            yaml.set("base.z", clan.getBaseZ());
            yaml.set("base.yaw", clan.getBaseYaw());
            yaml.set("base.pitch", clan.getBasePitch());
        }

        for (Map.Entry<UUID, Clan.Role> entry : clan.getMembers().entrySet()) {
            yaml.set("members." + entry.getKey(), entry.getValue().name());
        }

        try {
            yaml.save(getClanFile(clan.getName()));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Clan '" + clan.getName() + "' nicht speichern.", ex);
        }
    }

    private File getClanFile(String name) {
        return new File(clansFolder, name.toLowerCase(Locale.ROOT) + ".yml");
    }

    /** Speichert alle aktuell geladenen Clans, z.B. beim onDisable() als Sicherheitsnetz. */
    public void saveAll() {
        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
    }

    // ---------------------------------------------------------------
    // Abfragen
    // ---------------------------------------------------------------

    public Optional<Clan> getClan(String name) {
        return Optional.ofNullable(clans.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<Clan> getClanOf(UUID uuid) {
        String name = memberIndex.get(uuid);
        return name == null ? Optional.empty() : getClan(name);
    }

    public Collection<Clan> getAllClans() {
        return Collections.unmodifiableCollection(clans.values());
    }

    public boolean existsByName(String name) {
        return clans.containsKey(name.toLowerCase(Locale.ROOT));
    }

    // ---------------------------------------------------------------
    // Erstellen / Loeschen / Mitglieder
    // ---------------------------------------------------------------

    public Clan createClan(String name, String tag, Player leader) {
        Clan clan = new Clan(name, tag, leader.getUniqueId());
        String key = name.toLowerCase(Locale.ROOT);
        clans.put(key, clan);
        memberIndex.put(leader.getUniqueId(), key);
        saveClan(clan);
        return clan;
    }

    public void deleteClan(Clan clan) {
        clans.remove(clan.getName().toLowerCase(Locale.ROOT));
        for (UUID uuid : clan.getMembers().keySet()) {
            memberIndex.remove(uuid);
        }
        File file = getClanFile(clan.getName());
        if (file.exists()) {
            file.delete();
        }
    }

    public void addMember(Clan clan, UUID uuid) {
        clan.getMembers().put(uuid, Clan.Role.MEMBER);
        memberIndex.put(uuid, clan.getName().toLowerCase(Locale.ROOT));
        saveClan(clan);
    }

    public void removeMember(Clan clan, UUID uuid) {
        clan.getMembers().remove(uuid);
        memberIndex.remove(uuid);
        saveClan(clan);
    }

    public void setRole(Clan clan, UUID uuid, Clan.Role role) {
        clan.getMembers().put(uuid, role);
        saveClan(clan);
    }

    /** Benennt einen Clan um. Gibt false zurueck, wenn der neue Name schon vergeben ist. */
    public boolean renameClan(Clan clan, String newName) {
        if (existsByName(newName)) {
            return false;
        }

        File oldFile = getClanFile(clan.getName());
        clans.remove(clan.getName().toLowerCase(Locale.ROOT));

        clan.setName(newName);
        String key = newName.toLowerCase(Locale.ROOT);
        clans.put(key, clan);
        for (UUID uuid : clan.getMembers().keySet()) {
            memberIndex.put(uuid, key);
        }

        if (oldFile.exists()) {
            oldFile.delete();
        }
        saveClan(clan);
        return true;
    }

    // ---------------------------------------------------------------
    // Einladungen (analog TpaManager)
    // ---------------------------------------------------------------

    public Optional<ClanInvite> getPendingInvite(UUID invitedUuid) {
        return Optional.ofNullable(pendingInvites.get(invitedUuid));
    }

    public ClanInvite createInvite(String clanName, Player invited, String inviterName, Runnable onExpire) {
        ClanInvite previous = pendingInvites.get(invited.getUniqueId());
        if (previous != null) {
            cancelTask(previous.getExpiryTask());
        }

        ClanInvite invite = new ClanInvite(clanName, invited.getUniqueId(), inviterName);

        int expirySeconds = plugin.getConfig().getInt("clan.invite-expiry-seconds", 60);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.get(invited.getUniqueId()) == invite) {
                pendingInvites.remove(invited.getUniqueId());
                onExpire.run();
            }
        }, expirySeconds * 20L);

        invite.setExpiryTask(task);
        pendingInvites.put(invited.getUniqueId(), invite);
        return invite;
    }

    public void removeInvite(ClanInvite invite) {
        cancelTask(invite.getExpiryTask());
        pendingInvites.remove(invite.getInvitedUuid());
    }

    public void clearInvitesOf(UUID uuid) {
        ClanInvite invite = pendingInvites.remove(uuid);
        if (invite != null) {
            cancelTask(invite.getExpiryTask());
        }
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    // ---------------------------------------------------------------
    // Limits
    // ---------------------------------------------------------------

    public int getMaxMembers() {
        return plugin.getConfig().getInt("clan.max-members", 20);
    }
}
