package de.frostberg.homes.quest.manager;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.quest.model.Quest;
import de.frostberg.homes.quest.model.QuestCategory;
import de.frostberg.homes.quest.model.QuestType;
import de.frostberg.homes.quest.model.PlayerQuestData;
import de.frostberg.homes.util.CurrencyBridge;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Herzstueck des Quest-Systems: laedt den Quest-Pool aus quests.yml, waehlt
 * pro Periode zufaellig die aktiven Quests aus (fuer ALLE Spieler identisch),
 * verwaltet Reset/Nachhol-Logik, Spieler-Fortschritt, Belohnungen (inkl.
 * Rang-Multiplikator, Kategorie-Bonus, Streak) sowie Erinnerungen.
 *
 * Anders als HomeManager/ClanManager sendet dieser Manager fuer intern
 * ausgeloeste Ereignisse (Fortschritt, Abschluss, Reset, Erinnerungen) auch
 * selbst Chat-Nachrichten - analog zu TpaManager, der ebenfalls zeitgesteuerte
 * Ereignisse (Ablauf von Anfragen) direkt selbst meldet, statt das an einen
 * Command weiterzureichen, der zu diesem Zeitpunkt gar nicht aufgerufen wird.
 *
 * Persistenz:
 * - questdata/active.yml: aktuell aktive + vorgemerkte ("upcoming", nur fuer
 *   Admin-Vorschau) Quest-IDs und Periode-Schluessel pro Kategorie
 * - questdata/players/&lt;uuid&gt;.yml: Fortschritt/Abholungen/Streak/Statistik
 *
 * Reset-Erkennung laeuft bewusst NICHT ueber exakt getimte Einzel-Tasks,
 * sondern ueber einen einfachen Minuten-Takt, der den aktuell erwarteten
 * Periode-Schluessel mit dem gespeicherten vergleicht - das erledigt sowohl
 * den planmaessigen Reset als auch das Nachholen nach Offline-Zeiten in
 * derselben Logik, ohne separate Spezialfaelle.
 */
public class QuestManager implements Listener {

    private final FrostbergHomes plugin;
    private final File playersFolder;
    private File activeFile;
    private Logger actionLogger;

    private FileConfiguration questConfig;
    private final Map<QuestCategory, List<Quest>> pools = new EnumMap<>(QuestCategory.class);
    private final Map<QuestCategory, Integer> questCounts = new EnumMap<>(QuestCategory.class);
    private final Map<QuestCategory, List<Quest>> activeQuests = new EnumMap<>(QuestCategory.class);
    private final Map<QuestCategory, List<Quest>> upcomingQuests = new EnumMap<>(QuestCategory.class);
    private final Map<QuestCategory, String> periodKeys = new EnumMap<>(QuestCategory.class);

    private final Map<UUID, PlayerQuestData> cache = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();
    private final Map<UUID, Long> lastKnownTokenBalance = new HashMap<>();

    public enum ClaimResult {
        SUCCESS, NOT_FOUND, NOT_COMPLETE, ALREADY_CLAIMED
    }

    public record TopEntry(UUID uuid, int total) {
    }

    public QuestManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "questdata/players");
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
        this.activeFile = new File(plugin.getDataFolder(), "questdata/active.yml");

        setupLogger();
        loadQuestsConfig();
        loadActiveState();
        checkResets();

        Bukkit.getScheduler().runTaskTimer(plugin, this::checkResets, 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirty, 2400L, 2400L);

        long tokenIntervalTicks = Math.max(1, plugin.getConfig().getLong("quest.token-balance-check-interval-seconds", 5)) * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkTokenProgress, tokenIntervalTicks, tokenIntervalTicks);

        long reminderIntervalTicks = Math.max(1, plugin.getConfig().getLong("quest.repeat-reminder-interval-minutes", 30)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::sendRepeatReminders, reminderIntervalTicks, reminderIntervalTicks);
    }

    // ---------------------------------------------------------------
    // quests.yml laden
    // ---------------------------------------------------------------

    private void loadQuestsConfig() {
        plugin.saveResource("quests.yml", false);
        File file = new File(plugin.getDataFolder(), "quests.yml");
        questConfig = YamlConfiguration.loadConfiguration(file);

        for (QuestCategory category : QuestCategory.values()) {
            String base = category.getConfigKey() + ".";
            questCounts.put(category, questConfig.getInt(base + "quest-count", 1));

            List<Quest> pool = new ArrayList<>();
            for (Map<?, ?> raw : questConfig.getMapList(base + "pool")) {
                Quest quest = parseQuest(raw);
                if (quest != null) {
                    pool.add(quest);
                }
            }
            pools.put(category, pool);
        }
    }

    private Quest parseQuest(Map<?, ?> raw) {
        try {
            String id = String.valueOf(raw.get("id"));
            QuestType type = QuestType.valueOf(String.valueOf(raw.get("type")).toUpperCase(Locale.ROOT));
            Object targetObj = raw.get("target");
            String target = targetObj != null ? String.valueOf(targetObj) : null;
            long amount = Long.parseLong(String.valueOf(raw.get("amount")));
            String name = String.valueOf(raw.get("name"));
            String description = raw.get("description") != null ? String.valueOf(raw.get("description")) : "";
            int difficulty = raw.get("difficulty") != null ? Integer.parseInt(String.valueOf(raw.get("difficulty"))) : 1;

            long tokens = 0;
            double gold = 0;
            if (raw.get("reward") instanceof Map<?, ?> rewardMap) {
                Object t = rewardMap.get("tokens");
                Object g = rewardMap.get("gold");
                tokens = t != null ? Long.parseLong(String.valueOf(t)) : 0;
                gold = g != null ? Double.parseDouble(String.valueOf(g)) : 0;
            }

            return new Quest(id, type, target, amount, name, description, difficulty, tokens, gold);
        } catch (Exception ex) {
            plugin.getLogger().warning("Ungueltige Quest-Definition in quests.yml uebersprungen: " + raw);
            return null;
        }
    }

    /** Laedt quests.yml neu und loest aktive/vorgemerkte IDs gegen den neuen Pool auf (siehe /quest reload). */
    public void reload() {
        Map<QuestCategory, List<String>> oldActive = new EnumMap<>(QuestCategory.class);
        Map<QuestCategory, List<String>> oldUpcoming = new EnumMap<>(QuestCategory.class);
        for (QuestCategory category : QuestCategory.values()) {
            oldActive.put(category, idsOf(activeQuests.getOrDefault(category, Collections.emptyList())));
            oldUpcoming.put(category, idsOf(upcomingQuests.getOrDefault(category, Collections.emptyList())));
        }

        loadQuestsConfig();

        for (QuestCategory category : QuestCategory.values()) {
            activeQuests.put(category, resolveQuests(category, oldActive.get(category)));
            upcomingQuests.put(category, resolveQuests(category, oldUpcoming.get(category)));
        }

        checkResets();
        logAction("RELOAD");
    }

    // ---------------------------------------------------------------
    // active.yml (Periode + aktive/vorgemerkte Quest-IDs je Kategorie)
    // ---------------------------------------------------------------

    private void loadActiveState() {
        if (!activeFile.exists()) {
            for (QuestCategory category : QuestCategory.values()) {
                periodKeys.put(category, "");
                activeQuests.put(category, new ArrayList<>());
                upcomingQuests.put(category, new ArrayList<>());
            }
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(activeFile);
        for (QuestCategory category : QuestCategory.values()) {
            String base = category.getConfigKey() + ".";
            periodKeys.put(category, yaml.getString(base + "period", ""));
            activeQuests.put(category, resolveQuests(category, yaml.getStringList(base + "active")));
            upcomingQuests.put(category, resolveQuests(category, yaml.getStringList(base + "upcoming")));
        }
    }

    private void saveActiveState() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (QuestCategory category : QuestCategory.values()) {
            String base = category.getConfigKey() + ".";
            yaml.set(base + "period", periodKeys.get(category));
            yaml.set(base + "active", idsOf(activeQuests.get(category)));
            yaml.set(base + "upcoming", idsOf(upcomingQuests.get(category)));
        }
        try {
            yaml.save(activeFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Konnte questdata/active.yml nicht speichern.", ex);
        }
    }

    private List<Quest> resolveQuests(QuestCategory category, List<String> ids) {
        List<Quest> result = new ArrayList<>();
        List<Quest> pool = pools.get(category);
        if (pool == null || ids == null) {
            return result;
        }
        for (String id : ids) {
            for (Quest quest : pool) {
                if (quest.getId().equals(id)) {
                    result.add(quest);
                    break;
                }
            }
        }
        return result;
    }

    private List<String> idsOf(List<Quest> quests) {
        List<String> ids = new ArrayList<>();
        for (Quest quest : quests) {
            ids.add(quest.getId());
        }
        return ids;
    }

    // ---------------------------------------------------------------
    // Reset-Engine (planmaessig + Nachholen nach Offline-Zeit)
    // ---------------------------------------------------------------

    private String expectedPeriodKey(QuestCategory category) {
        LocalDate now = LocalDate.now();
        return switch (category) {
            case DAILY -> now.toString();
            case WEEKLY -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
            case MONTHLY -> now.withDayOfMonth(1).toString();
        };
    }

    public long getNextResetMillis(QuestCategory category) {
        LocalDate now = LocalDate.now();
        LocalDate nextBoundary = switch (category) {
            case DAILY -> now.plusDays(1);
            case WEEKLY -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(7);
            case MONTHLY -> now.withDayOfMonth(1).plusMonths(1);
        };
        return nextBoundary.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void checkResets() {
        for (QuestCategory category : QuestCategory.values()) {
            String expected = expectedPeriodKey(category);
            if (!expected.equals(periodKeys.get(category))) {
                performReset(category, expected);
            }
        }
    }

    private void performReset(QuestCategory category, String newPeriodKey) {
        List<Quest> pool = pools.getOrDefault(category, Collections.emptyList());
        int count = Math.min(questCounts.getOrDefault(category, 1), pool.size());

        List<Quest> newActive = upcomingQuests.getOrDefault(category, Collections.emptyList());
        if (newActive.isEmpty() || newActive.size() != count) {
            newActive = rollQuests(pool, count, null);
        }
        List<Quest> newUpcoming = rollQuests(pool, count, newActive);

        activeQuests.put(category, newActive);
        upcomingQuests.put(category, newUpcoming);
        periodKeys.put(category, newPeriodKey);
        saveActiveState();

        logAction("RESET " + category + " -> " + idsOf(newActive));

        if (plugin.getConfig().getBoolean("quest.reset-broadcast-enabled", true) && !newActive.isEmpty()) {
            String message = MessageUtil.get(plugin.getMessages(), "quest-reset-broadcast")
                    .replace("%category%", categoryDisplayName(category));
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(message);
            }
        }
    }

    private List<Quest> rollQuests(List<Quest> pool, int count, List<Quest> exclude) {
        if (count <= 0 || pool.isEmpty()) {
            return new ArrayList<>();
        }
        List<Quest> candidates = new ArrayList<>(pool);
        if (exclude != null && !exclude.isEmpty()) {
            List<Quest> filtered = new ArrayList<>(candidates);
            filtered.removeIf(exclude::contains);
            if (filtered.size() >= count) {
                candidates = filtered;
            }
        }
        Collections.shuffle(candidates);
        return new ArrayList<>(candidates.subList(0, Math.min(count, candidates.size())));
    }

    // ---------------------------------------------------------------
    // Abfragen fuer GUI/Command
    // ---------------------------------------------------------------

    public List<Quest> getActiveQuests(QuestCategory category) {
        return Collections.unmodifiableList(activeQuests.getOrDefault(category, Collections.emptyList()));
    }

    /** Nur fuer die Admin-Vorschau gedacht (quest.admin) - normale Spieler sehen diese Liste nie. */
    public List<Quest> getUpcomingQuests(QuestCategory category) {
        return Collections.unmodifiableList(upcomingQuests.getOrDefault(category, Collections.emptyList()));
    }

    public int getQuestCount(QuestCategory category) {
        return questCounts.getOrDefault(category, 1);
    }

    public String categoryDisplayName(QuestCategory category) {
        return switch (category) {
            case DAILY -> MessageUtil.get(plugin.getMessages(), "quest-category-daily-name");
            case WEEKLY -> MessageUtil.get(plugin.getMessages(), "quest-category-weekly-name");
            case MONTHLY -> MessageUtil.get(plugin.getMessages(), "quest-category-monthly-name");
        };
    }

    // ---------------------------------------------------------------
    // Spielerdaten laden/speichern
    // ---------------------------------------------------------------

    public PlayerQuestData getData(UUID uuid) {
        PlayerQuestData data = cache.get(uuid);
        if (data == null) {
            data = loadData(uuid);
            cache.put(uuid, data);
        }
        for (QuestCategory category : QuestCategory.values()) {
            data.ensureCurrentPeriod(category, periodKeys.getOrDefault(category, ""));
        }
        return data;
    }

    private PlayerQuestData loadData(UUID uuid) {
        PlayerQuestData data = new PlayerQuestData();
        File file = new File(playersFolder, uuid + ".yml");
        if (!file.exists()) {
            return data;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (QuestCategory category : QuestCategory.values()) {
            String base = category.getConfigKey() + ".";
            data.setPeriodKey(category, yaml.getString(base + "period", ""));

            ConfigurationSection progressSection = yaml.getConfigurationSection(base + "progress");
            if (progressSection != null) {
                for (String key : progressSection.getKeys(false)) {
                    data.setProgress(category, key, progressSection.getLong(key));
                }
            }
            for (String id : yaml.getStringList(base + "claimed")) {
                data.markClaimed(category, id);
            }
        }

        data.setStreak(yaml.getInt("streak.current", 0));
        data.setLastDailyCompletionDate(yaml.getString("streak.last-completed-date"));
        data.setTotalCompleted(yaml.getInt("stats.total-completed", 0));
        return data;
    }

    public void saveData(UUID uuid) {
        PlayerQuestData data = cache.get(uuid);
        if (data == null) {
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (QuestCategory category : QuestCategory.values()) {
            String base = category.getConfigKey() + ".";
            yaml.set(base + "period", data.getPeriodKey(category));
            for (Map.Entry<String, Long> entry : data.getProgressMap(category).entrySet()) {
                yaml.set(base + "progress." + entry.getKey(), entry.getValue());
            }
            yaml.set(base + "claimed", new ArrayList<>(data.getClaimedSet(category)));
        }
        yaml.set("streak.current", data.getStreak());
        yaml.set("streak.last-completed-date", data.getLastDailyCompletionDate());
        yaml.set("stats.total-completed", data.getTotalCompleted());

        try {
            yaml.save(new File(playersFolder, uuid + ".yml"));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Konnte Quest-Daten fuer " + uuid + " nicht speichern.", ex);
        }
        dirty.remove(uuid);
    }

    public void saveAll() {
        for (UUID uuid : new HashSet<>(cache.keySet())) {
            saveData(uuid);
        }
    }

    private void flushDirty() {
        for (UUID uuid : new HashSet<>(dirty)) {
            saveData(uuid);
        }
    }

    private void unload(UUID uuid) {
        saveData(uuid);
        cache.remove(uuid);
    }

    // ---------------------------------------------------------------
    // Fortschritt (aufgerufen vom QuestProgressListener)
    // ---------------------------------------------------------------

    public boolean hasActiveQuestOfType(QuestType type) {
        for (List<Quest> quests : activeQuests.values()) {
            for (Quest quest : quests) {
                if (quest.getType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInQuestWorld(Player player) {
        if (player.hasPermission("quest.bypass.world")) {
            return true;
        }
        String worldName = plugin.getConfig().getString("settings.farm-world", "farm");
        return player.getWorld().getName().equals(worldName);
    }

    public void addProgress(Player player, QuestType type, Material material, EntityType entityType, long rawAmount) {
        if (rawAmount <= 0 || !hasActiveQuestOfType(type)) {
            return;
        }
        if (!isInQuestWorld(player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission("quest.bypass.creative")) {
            return;
        }

        PlayerQuestData data = getData(player.getUniqueId());
        boolean changed = false;

        for (QuestCategory category : QuestCategory.values()) {
            for (Quest quest : activeQuests.getOrDefault(category, Collections.emptyList())) {
                if (quest.getType() != type || !matchesTarget(quest, material, entityType)) {
                    continue;
                }
                if (applyProgress(player, data, category, quest, rawAmount)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            dirty.add(player.getUniqueId());
        }
    }

    private boolean matchesTarget(Quest quest, Material material, EntityType entityType) {
        String target = quest.getTarget();
        switch (quest.getType()) {
            case MINE_BLOCK, PLACE_BLOCK, CRAFT_ITEM, FISH -> {
                if (target == null || target.isEmpty()) {
                    return true;
                }
                return matchesMaterialTag(material, target);
            }
            case KILL_ENTITY -> {
                if (target == null || target.isEmpty()) {
                    return true;
                }
                try {
                    return entityType == EntityType.valueOf(target.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            }
            case WALK_DISTANCE, EARN_TOKENS -> {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean matchesMaterialTag(Material material, String targetSpec) {
        if (material == null) {
            return false;
        }
        Tag<Material> tag = switch (targetSpec.toUpperCase(Locale.ROOT)) {
            case "LOGS" -> Tag.LOGS;
            case "PLANKS" -> Tag.PLANKS;
            case "LEAVES" -> Tag.LEAVES;
            case "WOOL" -> Tag.WOOL;
            case "SAPLINGS" -> Tag.SAPLINGS;
            default -> null;
        };
        if (tag != null) {
            return tag.isTagged(material);
        }
        Material exact = Material.matchMaterial(targetSpec);
        return exact == material;
    }

    private boolean applyProgress(Player player, PlayerQuestData data, QuestCategory category, Quest quest, long rawAmount) {
        data.ensureCurrentPeriod(category, periodKeys.getOrDefault(category, ""));
        if (data.isClaimed(category, quest.getId())) {
            return false;
        }

        long current = data.getProgress(category, quest.getId());
        if (current >= quest.getAmount()) {
            return false;
        }

        long updated = Math.min(current + rawAmount, quest.getAmount());
        data.setProgress(category, quest.getId(), updated);

        boolean progressMessagesEnabled = plugin.getConfig().getBoolean("quest.progress-messages-enabled", true);
        long interval = plugin.getConfig().getLong("quest.progress-message-interval", 100);
        if (progressMessagesEnabled && interval > 0 && updated < quest.getAmount()) {
            long prevMilestone = current / interval;
            long newMilestone = updated / interval;
            if (newMilestone > prevMilestone) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-progress-update")
                        .replace("%quest%", MessageUtil.color(quest.getName()))
                        .replace("%progress%", String.valueOf(updated))
                        .replace("%target%", String.valueOf(quest.getAmount())));
            }
        }

        if (updated >= quest.getAmount() && current < quest.getAmount()) {
            onQuestCompleted(player, data, category, quest);
        }

        return true;
    }

    private void onQuestCompleted(Player player, PlayerQuestData data, QuestCategory category, Quest quest) {
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-completed")
                .replace("%quest%", MessageUtil.color(quest.getName())));

        if (plugin.getConfig().getBoolean("quest.effects.sound-on-complete", true)) {
            player.playSound(player.getLocation(),
                    plugin.getConfig().getString("quest.effects.sound-complete", "ENTITY_PLAYER_LEVELUP"),
                    1.0f, 1.0f);
        }
        if (plugin.getConfig().getBoolean("quest.effects.particles-on-complete", true)) {
            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(
                        plugin.getConfig().getString("quest.effects.particle-complete", "HAPPY_VILLAGER"));
                player.spawnParticle(particle, player.getLocation().add(0, 1, 0), 20);
            } catch (IllegalArgumentException ignored) {
                // ungueltiger Partikelname in der Config - Effekt einfach auslassen
            }
        }

        data.incrementTotalCompleted();
        logAction("QUEST_COMPLETED " + player.getName() + " " + category + " " + quest.getId());

        boolean allDone = true;
        for (Quest active : activeQuests.getOrDefault(category, Collections.emptyList())) {
            if (data.getProgress(category, active.getId()) < active.getAmount()) {
                allDone = false;
                break;
            }
        }

        if (allDone && category == QuestCategory.DAILY) {
            updateStreak(player, data);
        }
    }

    // ---------------------------------------------------------------
    // Kategorie-Bonus (eigenes Item im Kategorie-GUI, grau/gesperrt bis alle
    // Quests der Kategorie fertig sind, dann gruen und manuell abholbar -
    // genau wie eine einzelne Quest-Belohnung, siehe claimReward)
    // ---------------------------------------------------------------

    private static final String CATEGORY_BONUS_ID = "__category_bonus__";

    /** Ob ALLE aktiven Quests dieser Kategorie fuer diesen Spieler fertig sind. */
    public boolean areAllQuestsDone(UUID uuid, QuestCategory category) {
        List<Quest> active = activeQuests.getOrDefault(category, Collections.emptyList());
        if (active.isEmpty()) {
            return false;
        }
        PlayerQuestData data = getData(uuid);
        for (Quest quest : active) {
            if (data.getProgress(category, quest.getId()) < quest.getAmount()) {
                return false;
            }
        }
        return true;
    }

    public boolean isCategoryBonusClaimed(UUID uuid, QuestCategory category) {
        return getData(uuid).isClaimed(category, CATEGORY_BONUS_ID);
    }

    public long getCategoryBonusTokens(QuestCategory category) {
        ConfigurationSection section = questConfig.getConfigurationSection("category-bonus." + category.getConfigKey());
        return section != null ? section.getLong("tokens", 0) : 0;
    }

    public double getCategoryBonusGold(QuestCategory category) {
        ConfigurationSection section = questConfig.getConfigurationSection("category-bonus." + category.getConfigKey());
        return section != null ? section.getDouble("gold", 0) : 0;
    }

    public ClaimResult claimCategoryBonus(Player player, QuestCategory category) {
        UUID uuid = player.getUniqueId();
        PlayerQuestData data = getData(uuid);

        if (data.isClaimed(category, CATEGORY_BONUS_ID)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        if (!areAllQuestsDone(uuid, category)) {
            return ClaimResult.NOT_COMPLETE;
        }

        long tokens = getCategoryBonusTokens(category);
        double gold = getCategoryBonusGold(category);

        CurrencyBridge.giveTokens(player, tokens);
        CurrencyBridge.giveGold(player, gold);

        data.markClaimed(category, CATEGORY_BONUS_ID);
        dirty.add(uuid);

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-category-bonus")
                .replace("%category%", categoryDisplayName(category))
                .replace("%tokens%", String.valueOf(tokens))
                .replace("%gold%", formatGold(gold)));

        if (plugin.getConfig().getBoolean("quest.effects.sound-on-claim", true)) {
            player.playSound(player.getLocation(),
                    plugin.getConfig().getString("quest.effects.sound-claim", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                    1.0f, 1.0f);
        }

        logAction("CATEGORY_BONUS_CLAIMED " + player.getName() + " " + category);
        return ClaimResult.SUCCESS;
    }

    private void updateStreak(Player player, PlayerQuestData data) {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        String last = data.getLastDailyCompletionDate();

        if (todayStr.equals(last)) {
            return;
        }

        int newStreak;
        if (last != null && !last.isEmpty()) {
            try {
                LocalDate lastDate = LocalDate.parse(last);
                newStreak = lastDate.plusDays(1).equals(today) ? data.getStreak() + 1 : 1;
            } catch (Exception ex) {
                newStreak = 1;
            }
        } else {
            newStreak = 1;
        }

        data.setStreak(newStreak);
        data.setLastDailyCompletionDate(todayStr);

        ConfigurationSection bonuses = questConfig.getConfigurationSection("streak-bonuses");
        if (bonuses != null && bonuses.contains(String.valueOf(newStreak))) {
            long tokens = bonuses.getLong(newStreak + ".tokens", 0);
            double gold = bonuses.getDouble(newStreak + ".gold", 0);
            CurrencyBridge.giveTokens(player, tokens);
            CurrencyBridge.giveGold(player, gold);

            player.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-streak-bonus")
                    .replace("%streak%", String.valueOf(newStreak))
                    .replace("%tokens%", String.valueOf(tokens))
                    .replace("%gold%", formatGold(gold)));
            logAction("STREAK_BONUS " + player.getName() + " " + newStreak);
        }
    }

    // ---------------------------------------------------------------
    // Tokens verdienen (Kontostand-Schnappschuss statt PlayerPoints-API)
    // ---------------------------------------------------------------

    private void checkTokenProgress() {
        if (!hasActiveQuestOfType(QuestType.EARN_TOKENS)) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            long balance = CurrencyBridge.readTokenBalance(player);
            if (balance < 0) {
                continue;
            }
            Long last = lastKnownTokenBalance.get(player.getUniqueId());
            if (last != null && balance > last) {
                addProgress(player, QuestType.EARN_TOKENS, null, null, balance - last);
            }
            lastKnownTokenBalance.put(player.getUniqueId(), balance);
        }
    }

    // ---------------------------------------------------------------
    // Belohnung abholen (GUI)
    // ---------------------------------------------------------------

    public ClaimResult claimReward(Player player, QuestCategory category, String questId) {
        PlayerQuestData data = getData(player.getUniqueId());

        Optional<Quest> questOpt = activeQuests.getOrDefault(category, Collections.emptyList()).stream()
                .filter(q -> q.getId().equals(questId))
                .findFirst();
        if (questOpt.isEmpty()) {
            return ClaimResult.NOT_FOUND;
        }
        Quest quest = questOpt.get();

        if (data.isClaimed(category, questId)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        if (data.getProgress(category, questId) < quest.getAmount()) {
            return ClaimResult.NOT_COMPLETE;
        }

        double multiplier = getMultiplier(player);
        long tokens = Math.round(quest.getRewardTokens() * multiplier);
        double gold = quest.getRewardGold() * multiplier;

        CurrencyBridge.giveTokens(player, tokens);
        CurrencyBridge.giveGold(player, gold);

        data.markClaimed(category, questId);
        dirty.add(player.getUniqueId());

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-claimed")
                .replace("%quest%", MessageUtil.color(quest.getName()))
                .replace("%tokens%", String.valueOf(tokens))
                .replace("%gold%", formatGold(gold)));

        if (plugin.getConfig().getBoolean("quest.effects.sound-on-claim", true)) {
            player.playSound(player.getLocation(),
                    plugin.getConfig().getString("quest.effects.sound-claim", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                    1.0f, 1.0f);
        }

        logAction("CLAIMED " + player.getName() + " " + category + " " + questId + " tokens=" + tokens + " gold=" + gold);
        return ClaimResult.SUCCESS;
    }

    private double getMultiplier(Player player) {
        double best = 1.0;
        for (Map<?, ?> entry : plugin.getConfig().getMapList("quest.reward-multipliers")) {
            Object permissionObj = entry.get("permission");
            Object multiplierObj = entry.get("multiplier");
            if (permissionObj == null || multiplierObj == null) {
                continue;
            }
            try {
                if (player.hasPermission(permissionObj.toString())) {
                    best = Math.max(best, Double.parseDouble(multiplierObj.toString()));
                }
            } catch (NumberFormatException ignored) {
                // ungueltiger Multiplikator-Wert in der Config - ueberspringen
            }
        }
        return best;
    }

    private String formatGold(double amount) {
        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }
        return String.format(Locale.GERMANY, "%.2f", amount);
    }

    // ---------------------------------------------------------------
    // Erinnerungen (Join + wiederkehrend)
    // ---------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lastKnownTokenBalance.remove(player.getUniqueId());

        if (!plugin.getConfig().getBoolean("quest.join-reminder-enabled", true)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && hasAnyOpenOrReady(player)) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-join-reminder"));
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unload(event.getPlayer().getUniqueId());
        lastKnownTokenBalance.remove(event.getPlayer().getUniqueId());
    }

    private void sendRepeatReminders() {
        if (!plugin.getConfig().getBoolean("quest.repeat-reminder-enabled", true)) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasAnyReady(player)) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-repeat-reminder"));
            }
        }
    }

    private boolean hasAnyOpenOrReady(Player player) {
        PlayerQuestData data = getData(player.getUniqueId());
        for (QuestCategory category : QuestCategory.values()) {
            for (Quest quest : activeQuests.getOrDefault(category, Collections.emptyList())) {
                if (!data.isClaimed(category, quest.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAnyReady(Player player) {
        PlayerQuestData data = getData(player.getUniqueId());
        for (QuestCategory category : QuestCategory.values()) {
            for (Quest quest : activeQuests.getOrDefault(category, Collections.emptyList())) {
                if (!data.isClaimed(category, quest.getId()) && data.getProgress(category, quest.getId()) >= quest.getAmount()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Admin-Funktionen
    // ---------------------------------------------------------------

    public void resetPlayer(UUID uuid, QuestCategory category) {
        PlayerQuestData data = getData(uuid);
        data.getProgressMap(category).clear();
        data.getClaimedSet(category).clear();
        dirty.add(uuid);
        saveData(uuid);
        logAction("ADMIN_RESET " + uuid + " " + category);
    }

    public List<TopEntry> getTopPlayers(int limit) {
        List<TopEntry> entries = new ArrayList<>();
        File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                    int total;
                    PlayerQuestData cached = cache.get(uuid);
                    if (cached != null) {
                        total = cached.getTotalCompleted();
                    } else {
                        total = YamlConfiguration.loadConfiguration(file).getInt("stats.total-completed", 0);
                    }
                    if (total > 0) {
                        entries.add(new TopEntry(uuid, total));
                    }
                } catch (IllegalArgumentException ignored) {
                    // Datei mit ungueltigem UUID-Namen - ueberspringen
                }
            }
        }
        entries.sort((a, b) -> Integer.compare(b.total(), a.total()));
        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }

    public void broadcastAdminMessage(String rawText) {
        String prefix = plugin.getMessages().getString("quest-prefix", "");
        String formatted = MessageUtil.color(prefix + rawText);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(formatted);
        }
        logAction("BROADCAST " + rawText);
    }

    // ---------------------------------------------------------------
    // Admin-Log-Datei
    // ---------------------------------------------------------------

    private void setupLogger() {
        try {
            File logsFolder = new File(plugin.getDataFolder(), "logs");
            if (!logsFolder.exists()) {
                logsFolder.mkdirs();
            }
            actionLogger = Logger.getLogger("FrostbergHomesQuestLog");
            actionLogger.setUseParentHandlers(false);
            for (var handler : actionLogger.getHandlers()) {
                actionLogger.removeHandler(handler);
            }
            FileHandler handler = new FileHandler(new File(logsFolder, "quest.log").getPath(), true);
            handler.setFormatter(new SimpleFormatter());
            actionLogger.addHandler(handler);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Konnte quest.log nicht einrichten.", ex);
        }
    }

    private void logAction(String message) {
        if (actionLogger != null) {
            actionLogger.info(message);
        }
    }
}
