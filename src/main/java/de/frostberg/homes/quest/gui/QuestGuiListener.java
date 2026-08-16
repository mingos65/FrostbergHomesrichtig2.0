package de.frostberg.homes.quest.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.quest.model.PlayerQuestData;
import de.frostberg.homes.quest.model.Quest;
import de.frostberg.homes.quest.model.QuestCategory;
import de.frostberg.homes.quest.model.QuestType;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Klick-basiertes GUI fuer /quest: Hauptmenue mit 3 Kategorie-Buttons,
 * Reset-Countdown, Statistik und Hilfe-Buch (fuer Admins zusaetzlich eine
 * Vorschau auf die naechste, noch nicht aktive Periode), sowie ein
 * Kategorie-Untermenue mit den einzelnen Quests inkl. Abholen-Interaktion.
 * Alle Inventare tragen einen QuestGuiHolder, analog zu HomeGuiHolder.
 */
public class QuestGuiListener implements Listener {

    private static final int DAILY_SLOT = 11;
    private static final int WEEKLY_SLOT = 13;
    private static final int MONTHLY_SLOT = 15;
    private static final int COUNTDOWN_SLOT = 20;
    private static final int STATS_SLOT = 24;
    private static final int ADMIN_PREVIEW_SLOT = 29;
    private static final int HELP_SLOT_MAIN = 35;

    private static final int BACK_SLOT = 18;
    private static final int HELP_SLOT_CATEGORY = 26;

    private final FrostbergHomes plugin;

    public QuestGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Oeffnen: Hauptmenue
    // ---------------------------------------------------------------

    public void openMain(Player player) {
        QuestGuiHolder holder = new QuestGuiHolder(QuestGuiHolder.Type.MAIN, null);
        String title = MessageUtil.get(plugin.getMessages(), "quest-gui-main-title");
        Inventory inventory = Bukkit.createInventory(holder, 36, MessageUtil.color(title));
        holder.setInventory(inventory);

        inventory.setItem(DAILY_SLOT, buildCategoryItem(player, QuestCategory.DAILY, Material.CLOCK));
        inventory.setItem(WEEKLY_SLOT, buildCategoryItem(player, QuestCategory.WEEKLY, Material.BOOK));
        inventory.setItem(MONTHLY_SLOT, buildCategoryItem(player, QuestCategory.MONTHLY, Material.ENDER_CHEST));
        inventory.setItem(COUNTDOWN_SLOT, buildCountdownItem());
        inventory.setItem(STATS_SLOT, buildStatsItem(player));

        if (player.hasPermission("quest.admin")) {
            inventory.setItem(ADMIN_PREVIEW_SLOT, buildAdminPreviewItem());
        }
        inventory.setItem(HELP_SLOT_MAIN, buildHelpItem());
        fillBorder(inventory);

        player.openInventory(inventory);
    }

    private ItemStack buildCategoryItem(Player player, QuestCategory category, Material icon) {
        List<Quest> active = plugin.getQuestManager().getActiveQuests(category);
        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());

        int done = 0;
        for (Quest quest : active) {
            if (data.getProgress(category, quest.getId()) >= quest.getAmount()) {
                done++;
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.get(plugin.getMessages(), "quest-gui-category-lore")
                .replace("%done%", String.valueOf(done))
                .replace("%total%", String.valueOf(active.size())));

        return simpleItem(icon, plugin.getQuestManager().categoryDisplayName(category), lore);
    }

    private ItemStack buildCountdownItem() {
        List<String> lore = new ArrayList<>();
        for (QuestCategory category : QuestCategory.values()) {
            long remaining = plugin.getQuestManager().getNextResetMillis(category) - System.currentTimeMillis();
            lore.add(MessageUtil.get(plugin.getMessages(), "quest-gui-countdown-line")
                    .replace("%category%", plugin.getQuestManager().categoryDisplayName(category))
                    .replace("%time%", formatDuration(remaining)));
        }
        return simpleItem(Material.CLOCK, MessageUtil.get(plugin.getMessages(), "quest-gui-countdown-name"), lore);
    }

    private ItemStack buildStatsItem(Player player) {
        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.get(plugin.getMessages(), "quest-gui-stats-completed")
                .replace("%amount%", String.valueOf(data.getTotalCompleted())));
        lore.add(MessageUtil.get(plugin.getMessages(), "quest-gui-stats-streak")
                .replace("%streak%", String.valueOf(data.getStreak())));
        return simpleItem(Material.NETHER_STAR, MessageUtil.get(plugin.getMessages(), "quest-gui-stats-name"), lore);
    }

    /** Nur fuer Spieler mit quest.admin sichtbar - zeigt die noch nicht aktive naechste Periode. */
    private ItemStack buildAdminPreviewItem() {
        List<String> lore = new ArrayList<>();
        for (QuestCategory category : QuestCategory.values()) {
            lore.add(MessageUtil.color("&8» &7" + plugin.getQuestManager().categoryDisplayName(category)));
            for (Quest quest : plugin.getQuestManager().getUpcomingQuests(category)) {
                lore.add(MessageUtil.color("   &7- &f" + quest.getName()));
            }
        }
        return simpleItem(Material.SPYGLASS, MessageUtil.get(plugin.getMessages(), "quest-gui-admin-preview-name"), lore);
    }

    private ItemStack buildHelpItem() {
        return simpleItem(Material.WRITTEN_BOOK, MessageUtil.get(plugin.getMessages(), "quest-gui-help-name"),
                List.of(MessageUtil.get(plugin.getMessages(), "quest-gui-help-lore")));
    }

    // ---------------------------------------------------------------
    // Oeffnen: Kategorie-Untermenue
    // ---------------------------------------------------------------

    public void openCategory(Player player, QuestCategory category) {
        QuestGuiHolder holder = new QuestGuiHolder(QuestGuiHolder.Type.CATEGORY, category);
        String title = MessageUtil.get(plugin.getMessages(), "quest-gui-category-title")
                .replace("%category%", plugin.getQuestManager().categoryDisplayName(category));
        Inventory inventory = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inventory);

        List<Quest> active = plugin.getQuestManager().getActiveQuests(category);
        int[] slots = centeredSlots(active.size());
        PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());

        for (int i = 0; i < active.size() && i < slots.length; i++) {
            inventory.setItem(slots[i], buildQuestItem(category, active.get(i), data));
        }

        inventory.setItem(BACK_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "quest-gui-back-name"), null));
        inventory.setItem(HELP_SLOT_CATEGORY, buildHelpItem());
        fillBorder(inventory);

        player.openInventory(inventory);
    }

    private int[] centeredSlots(int count) {
        count = Math.max(0, Math.min(count, 9));
        int start = 9 + (9 - count) / 2;
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = start + i;
        }
        return slots;
    }

    private ItemStack buildQuestItem(QuestCategory category, Quest quest, PlayerQuestData data) {
        long progress = data.getProgress(category, quest.getId());
        boolean claimed = data.isClaimed(category, quest.getId());
        boolean ready = !claimed && progress >= quest.getAmount();

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.color("&7" + quest.getDescription()));
        lore.add("");
        lore.add(MessageUtil.get(plugin.getMessages(), "quest-gui-quest-lore-progress")
                .replace("%progress%", String.valueOf(Math.min(progress, quest.getAmount())))
                .replace("%target%", String.valueOf(quest.getAmount())));
        lore.add(MessageUtil.get(plugin.getMessages(), "quest-gui-quest-lore-reward")
                .replace("%tokens%", String.valueOf(quest.getRewardTokens()))
                .replace("%gold%", String.valueOf(quest.getRewardGold())));
        lore.add(MessageUtil.get(plugin.getMessages(), "quest-gui-quest-difficulty")
                .replace("%stars%", starString(quest.getDifficulty())));
        lore.add("");

        String statusKey = claimed ? "quest-gui-quest-status-claimed"
                : ready ? "quest-gui-quest-status-ready"
                : "quest-gui-quest-status-open";
        lore.add(MessageUtil.get(plugin.getMessages(), statusKey));

        Material icon = claimed ? Material.GRAY_DYE : ready ? Material.LIME_DYE : iconFor(quest.getType());
        return simpleItem(icon, "&d" + quest.getName(), lore);
    }

    private Material iconFor(QuestType type) {
        return switch (type) {
            case MINE_BLOCK -> Material.IRON_PICKAXE;
            case PLACE_BLOCK -> Material.TURTLE_EGG;
            case KILL_ENTITY -> Material.IRON_SWORD;
            case CRAFT_ITEM -> Material.CRAFTING_TABLE;
            case FISH -> Material.FISHING_ROD;
            case WALK_DISTANCE -> Material.LEATHER_BOOTS;
            case EARN_TOKENS -> Material.EMERALD;
        };
    }

    private String starString(int difficulty) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= difficulty ? "&e★" : "&7★");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Hilfe-Buch
    // ---------------------------------------------------------------

    private void openHelpBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(MessageUtil.get(plugin.getMessages(), "quest-help-book-title"));
        meta.setAuthor("Frostberg");
        for (String page : plugin.getMessages().getStringList("quest-help-book-pages")) {
            meta.addPage(MessageUtil.color(page));
        }
        book.setItemMeta(meta);
        player.openBook(book);
    }

    // ---------------------------------------------------------------
    // Klicks
    // ---------------------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof QuestGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory topInventory = event.getView().getTopInventory();
        if (!topInventory.equals(event.getClickedInventory())) {
            return;
        }

        int slot = event.getSlot();
        switch (holder.getType()) {
            case MAIN -> handleMainClick(player, slot);
            case CATEGORY -> handleCategoryClick(player, holder.getCategory(), slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case DAILY_SLOT -> openCategory(player, QuestCategory.DAILY);
            case WEEKLY_SLOT -> openCategory(player, QuestCategory.WEEKLY);
            case MONTHLY_SLOT -> openCategory(player, QuestCategory.MONTHLY);
            case HELP_SLOT_MAIN -> openHelpBook(player);
            default -> {
                // kein interaktiver Slot (Deko/Countdown/Statistik/Admin-Vorschau)
            }
        }
    }

    private void handleCategoryClick(Player player, QuestCategory category, int slot) {
        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == HELP_SLOT_CATEGORY) {
            openHelpBook(player);
            return;
        }

        List<Quest> active = plugin.getQuestManager().getActiveQuests(category);
        int[] slots = centeredSlots(active.size());
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != slot) {
                continue;
            }
            Quest quest = active.get(i);
            PlayerQuestData data = plugin.getQuestManager().getData(player.getUniqueId());
            if (!data.isClaimed(category, quest.getId()) && data.getProgress(category, quest.getId()) >= quest.getAmount()) {
                plugin.getQuestManager().claimReward(player, category, quest.getId());
            }
            openCategory(player, category);
            return;
        }
    }

    // ---------------------------------------------------------------
    // Hilfsmittel
    // ---------------------------------------------------------------

    private String formatDuration(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long totalMinutes = millis / 60000L;
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(days == 1 ? " Tag " : " Tage ");
        }
        sb.append(hours).append(" Std. ").append(minutes).append(" Min.");
        return sb.toString();
    }

    private void fillBorder(Inventory inventory) {
        ItemStack filler = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack simpleItem(Material material, String coloredName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(coloredName));
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
