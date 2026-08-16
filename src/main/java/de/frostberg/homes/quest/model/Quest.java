package de.frostberg.homes.quest.model;

/**
 * Eine einzelne Quest-Definition aus dem Pool in quests.yml. Unveraenderlich -
 * beim /quest reload werden komplett neue Quest-Objekte aus der Config gebaut.
 */
public class Quest {

    private final String id;
    private final QuestType type;
    private final String target; // Material-/EntityType-Name oder Tag-Keyword, null bei EARN_TOKENS
    private final long amount;
    private final String name;
    private final String description;
    private final int difficulty; // 1-5
    private final long rewardTokens;
    private final double rewardGold;

    public Quest(String id, QuestType type, String target, long amount, String name,
                 String description, int difficulty, long rewardTokens, double rewardGold) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.name = name;
        this.description = description;
        this.difficulty = Math.max(1, Math.min(5, difficulty));
        this.rewardTokens = rewardTokens;
        this.rewardGold = rewardGold;
    }

    public String getId() {
        return id;
    }

    public QuestType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public long getAmount() {
        return amount;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public long getRewardTokens() {
        return rewardTokens;
    }

    public double getRewardGold() {
        return rewardGold;
    }
}
