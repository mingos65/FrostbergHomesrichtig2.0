package de.frostberg.homes.quest.model;

/**
 * Die drei Quest-Kategorien. configKey ist der Schluessel in quests.yml
 * (z.B. "daily"), messageKey der Nachrichten-Baustein fuer den Anzeigenamen
 * (z.B. "quest-category-daily-name" in messages.yml).
 */
public enum QuestCategory {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String configKey;

    QuestCategory(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
