package de.frostberg.homes.quest.model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fortschritt, Abholungen, Streak und Statistik eines einzelnen Spielers.
 * Pro Kategorie wird zusaetzlich der Periode-Schluessel gespeichert, unter dem
 * der Fortschritt zuletzt aktualisiert wurde (z.B. "2026-08-17" fuer DAILY).
 * Der QuestManager vergleicht diesen Schluessel beim Zugriff faul (lazy) gegen
 * die aktuell aktive Periode - stimmt er nicht mehr ueberein, gilt der
 * gesamte Fortschritt dieser Kategorie automatisch als zurueckgesetzt, ohne
 * dass beim Reset selbst jede Spieler-Datei angefasst werden muss.
 */
public class PlayerQuestData {

    private final Map<QuestCategory, String> periodKeys = new EnumMap<>(QuestCategory.class);
    private final Map<QuestCategory, Map<String, Long>> progress = new EnumMap<>(QuestCategory.class);
    private final Map<QuestCategory, Set<String>> claimed = new EnumMap<>(QuestCategory.class);

    private int streak;
    private String lastDailyCompletionDate; // ISO-Datum, an dem zuletzt ALLE Daily-Quests fertig wurden
    private int totalCompleted;

    public PlayerQuestData() {
        for (QuestCategory category : QuestCategory.values()) {
            progress.put(category, new HashMap<>());
            claimed.put(category, new HashSet<>());
            periodKeys.put(category, "");
        }
    }

    /**
     * Stellt sicher, dass der gespeicherte Fortschritt zur aktuell aktiven
     * Periode gehoert. Ist der gespeicherte Periode-Schluessel veraltet
     * (Reset hat stattgefunden), wird Fortschritt+Abholungen dieser Kategorie
     * geleert und der neue Schluessel uebernommen.
     */
    public void ensureCurrentPeriod(QuestCategory category, String currentPeriodKey) {
        String stored = periodKeys.get(category);
        if (!currentPeriodKey.equals(stored)) {
            progress.get(category).clear();
            claimed.get(category).clear();
            periodKeys.put(category, currentPeriodKey);
        }
    }

    public long getProgress(QuestCategory category, String questId) {
        return progress.get(category).getOrDefault(questId, 0L);
    }

    public void setProgress(QuestCategory category, String questId, long value) {
        progress.get(category).put(questId, value);
    }

    public boolean isClaimed(QuestCategory category, String questId) {
        return claimed.get(category).contains(questId);
    }

    public void markClaimed(QuestCategory category, String questId) {
        claimed.get(category).add(questId);
    }

    public String getPeriodKey(QuestCategory category) {
        return periodKeys.get(category);
    }

    public void setPeriodKey(QuestCategory category, String key) {
        periodKeys.put(category, key);
    }

    public Map<String, Long> getProgressMap(QuestCategory category) {
        return progress.get(category);
    }

    public Set<String> getClaimedSet(QuestCategory category) {
        return claimed.get(category);
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public String getLastDailyCompletionDate() {
        return lastDailyCompletionDate;
    }

    public void setLastDailyCompletionDate(String lastDailyCompletionDate) {
        this.lastDailyCompletionDate = lastDailyCompletionDate;
    }

    public int getTotalCompleted() {
        return totalCompleted;
    }

    public void setTotalCompleted(int totalCompleted) {
        this.totalCompleted = totalCompleted;
    }

    public void incrementTotalCompleted() {
        this.totalCompleted++;
    }
}
