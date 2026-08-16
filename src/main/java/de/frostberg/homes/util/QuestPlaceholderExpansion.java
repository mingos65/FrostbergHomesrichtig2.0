package de.frostberg.homes.util;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.quest.model.PlayerQuestData;
import de.frostberg.homes.quest.model.Quest;
import de.frostberg.homes.quest.model.QuestCategory;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;

/**
 * Stellt %frostbergquests_...%-Platzhalter bereit. Wird nur registriert, wenn
 * PlaceholderAPI installiert ist (siehe FrostbergHomes#onEnable).
 *
 * Verfuegbare Platzhalter:
 * %frostbergquests_daily_done%    - erledigte Daily-Quests (Zahl)
 * %frostbergquests_daily_total%   - Anzahl aktiver Daily-Quests
 * %frostbergquests_weekly_done%   / _weekly_total%
 * %frostbergquests_monthly_done%  / _monthly_total%
 * %frostbergquests_streak%        - aktueller Daily-Streak
 * %frostbergquests_completed%     - insgesamt abgeschlossene Quests
 */
public class QuestPlaceholderExpansion extends PlaceholderExpansion {

    private final FrostbergHomes plugin;

    public QuestPlaceholderExpansion(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "frostbergquests";
    }

    @Override
    public String getAuthor() {
        return "Frostberg";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }

        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
        String key = params.toLowerCase(Locale.ROOT);

        for (QuestCategory category : QuestCategory.values()) {
            String prefix = category.getConfigKey() + "_";
            List<Quest> active = plugin.getQuestManager().getActiveQuests(category);

            if (key.equals(prefix + "total")) {
                return String.valueOf(active.size());
            }
            if (key.equals(prefix + "done")) {
                int done = 0;
                for (Quest quest : active) {
                    if (data.getProgress(category, quest.getId()) >= quest.getAmount()) {
                        done++;
                    }
                }
                return String.valueOf(done);
            }
        }

        return switch (key) {
            case "streak" -> String.valueOf(data.getStreak());
            case "completed" -> String.valueOf(data.getTotalCompleted());
            default -> "";
        };
    }
}
