package de.frostberg.homes.shop.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.shop.manager.ShopManager;
import de.frostberg.homes.shop.model.ShopCategory;
import de.frostberg.homes.shop.model.ShopItem;
import de.frostberg.homes.shop.model.ShopSubCategory;
import de.frostberg.homes.util.CurrencyBridge;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Eigenes Shop-GUI, Ersatz fuer EconomyShopGUI. Vier Ebenen (wie beim
 * Vorbild-Shop): Hauptmenue -> Kategorie-Uebersicht (Icons der
 * Unterkategorien) -> Item-Liste -> Kauf/Verkauf-Detailfenster fuer ein
 * einzelnes Item. Auf jeder Ebene unten immer an derselben Stelle
 * "Zurueck" (Slot 45) und "Hauptmenue" (Slot 49).
 *
 * Alle Inventare sind 54 Slots (6 Reihen) gross, fuer ein einheitliches
 * Erscheinungsbild. Nicht belegte Slots werden mit Glas-Scheiben gefuellt.
 */
public class ShopGuiListener implements Listener {

    private static final int CENTER_ROW_START = 18; // Reihe 3 (0-indexiert Reihe 2)
    private static final int ITEM_GRID_START = 9; // Reihe 2
    private static final int ITEM_GRID_ROWS = 4;
    private static final int BACK_SLOT = 45;
    private static final int MAIN_MENU_SLOT = 49;
    private static final int PREV_PAGE_SLOT = 46;
    private static final int NEXT_PAGE_SLOT = 52;

    private static final int DETAIL_ITEM_SLOT = 22;
    private static final int[] SELL_SLOTS = {21, 20, 19}; // -1, -32, -64 (von innen nach aussen)
    private static final int[] SELL_AMOUNTS = {1, 32, 64};
    private static final int[] BUY_SLOTS = {23, 24, 25}; // +1, +32, +64
    private static final int[] BUY_AMOUNTS = {1, 32, 64};

    private final FrostbergHomes plugin;

    public ShopGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Ebene 1: Hauptmenue
    // ---------------------------------------------------------------

    public void openMain(Player player) {
        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.MAIN, null, null, 0, null);
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(bracketTitle(MessageUtil.get(plugin.getMessages(), "shop-gui-main-title"))));
        holder.setInventory(inventory);

        fillBorder(inventory);

        List<ShopCategory> categories = plugin.getShopManager().getCategories();
        int[] slots = centeredSlots(categories.size(), CENTER_ROW_START);
        for (int i = 0; i < categories.size() && i < slots.length; i++) {
            ShopCategory category = categories.get(i);
            List<String> lore = new ArrayList<>();
            for (ShopSubCategory sub : category.getSubCategories()) {
                lore.add(MessageUtil.color("&7▸ " + sub.getDisplayName()));
            }
            lore.add("");
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-gui-main-hint"));
            inventory.setItem(slots[i], simpleItem(category.getIcon(), MessageUtil.color("&l") + category.getDisplayName(), lore));
        }

        player.openInventory(inventory);
    }

    // ---------------------------------------------------------------
    // Ebene 2: Kategorie-Uebersicht (Unterkategorie-Icons)
    // ---------------------------------------------------------------

    public void openCategoryHub(Player player, String categoryId) {
        Optional<ShopCategory> categoryOpt = findCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            openMain(player);
            return;
        }
        ShopCategory category = categoryOpt.get();

        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.CATEGORY_HUB, category.getId(), null, 0, null);
        String title = bracketTitle(category.getDisplayName());
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inventory);

        fillBorder(inventory);
        addNavBar(inventory, true, false, 0, 0);

        List<ShopSubCategory> subCategories = category.getSubCategories();
        int[] slots = centeredSlots(subCategories.size(), CENTER_ROW_START);
        for (int i = 0; i < subCategories.size() && i < slots.length; i++) {
            ShopSubCategory sub = subCategories.get(i);
            List<String> lore = List.of(
                    MessageUtil.get(plugin.getMessages(), "shop-gui-hub-hint").replace("%count%", String.valueOf(sub.getItems().size()))
            );
            inventory.setItem(slots[i], simpleItem(sub.getIcon(), MessageUtil.color("&l") + sub.getDisplayName(), lore));
        }

        player.openInventory(inventory);
    }

    // ---------------------------------------------------------------
    // Ebene 3: Item-Liste
    // ---------------------------------------------------------------

    public void openItemList(Player player, String categoryId, String subCategoryId, int page) {
        Optional<ShopCategory> categoryOpt = findCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            openMain(player);
            return;
        }
        ShopCategory category = categoryOpt.get();
        Optional<ShopSubCategory> subOpt = findSubCategory(category, subCategoryId);
        if (subOpt.isEmpty()) {
            openCategoryHub(player, categoryId);
            return;
        }
        ShopSubCategory subCategory = subOpt.get();

        int columns = Math.max(1, Math.min(9, subCategory.getColumns()));
        int itemsPerPage = columns * ITEM_GRID_ROWS;
        List<ShopItem> items = subCategory.getItems();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) itemsPerPage));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        int offset = clampedPage * itemsPerPage;

        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.ITEM_LIST, category.getId(), subCategory.getId(), clampedPage, null);
        String title = bracketTitle(MessageUtil.get(plugin.getMessages(), "shop-gui-list-title")
                .replace("%category%", category.getDisplayName())
                .replace("%sub%", subCategory.getDisplayName()));
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inventory);

        fillBorder(inventory);
        addNavBar(inventory, true, totalPages > 1, clampedPage, totalPages);

        int leftPad = (9 - columns) / 2;
        for (int i = 0; i < itemsPerPage && offset + i < items.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            if (row >= ITEM_GRID_ROWS) {
                break;
            }
            int slot = ITEM_GRID_START + row * 9 + leftPad + col;
            inventory.setItem(slot, buildListItemStack(items.get(offset + i)));
        }

        player.openInventory(inventory);
    }

    // ---------------------------------------------------------------
    // Ebene 4: Kauf/Verkauf-Detail
    // ---------------------------------------------------------------

    public void openItemDetail(Player player, String categoryId, String subCategoryId, int page, String itemId) {
        ShopItem item = plugin.getShopManager().getItem(itemId);
        Optional<ShopCategory> categoryOpt = findCategory(categoryId);
        if (item == null || categoryOpt.isEmpty()) {
            openMain(player);
            return;
        }
        ShopCategory category = categoryOpt.get();

        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.ITEM_DETAIL, categoryId, subCategoryId, page, itemId);
        String title = bracketTitle(item.getDisplayName());
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inventory);

        fillBorder(inventory);
        addNavBar(inventory, true, false, 0, 0);

        inventory.setItem(DETAIL_ITEM_SLOT, buildDetailItemStack(player, item));

        for (int i = 0; i < SELL_SLOTS.length; i++) {
            if (!item.isSellable()) {
                continue;
            }
            int amount = SELL_AMOUNTS[i];
            String name = MessageUtil.get(plugin.getMessages(), "shop-detail-sell-button").replace("%amount%", String.valueOf(amount));
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), "shop-detail-sell-lore")
                    .replace("%total%", String.valueOf(item.getSellPrice() * amount)));
            inventory.setItem(SELL_SLOTS[i], simpleItem(Material.RED_STAINED_GLASS_PANE, MessageUtil.color("&c&l") + name, lore));
        }
        for (int i = 0; i < BUY_SLOTS.length; i++) {
            if (!item.isBuyable()) {
                continue;
            }
            int amount = BUY_AMOUNTS[i];
            String name = MessageUtil.get(plugin.getMessages(), "shop-detail-buy-button").replace("%amount%", String.valueOf(amount));
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), "shop-detail-buy-lore")
                    .replace("%total%", String.valueOf(item.getBuyPrice() * amount)));
            inventory.setItem(BUY_SLOTS[i], simpleItem(Material.LIME_STAINED_GLASS_PANE, MessageUtil.color("&a&l") + name, lore));
        }

        player.openInventory(inventory);
    }

    private ItemStack buildDetailItemStack(Player player, ShopItem item) {
        List<String> lore = new ArrayList<>();
        if (item.isBuyable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-buy").replace("%price%", String.valueOf(item.getBuyPrice())));
        }
        if (item.isSellable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-sell").replace("%price%", String.valueOf(item.getSellPrice())));
        }
        lore.add("");
        long balance = CurrencyBridge.readTokenBalance(player);
        if (balance >= 0) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-detail-balance").replace("%balance%", String.valueOf(balance)));
        }
        return simpleItem(item.getMaterial(), MessageUtil.color("&l") + item.getDisplayName(), lore);
    }

    // ---------------------------------------------------------------
    // Hilfsaufbau: Navigationsleiste unten, Titel-Klammern, Rahmen
    // ---------------------------------------------------------------

    private void addNavBar(Inventory inventory, boolean showBack, boolean showPages, int page, int totalPages) {
        List<String> backLore = List.of(MessageUtil.get(plugin.getMessages(), "shop-gui-back-lore"));
        inventory.setItem(BACK_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-back"), backLore));

        List<String> mainLore = List.of(MessageUtil.get(plugin.getMessages(), "shop-gui-mainmenu-lore"));
        inventory.setItem(MAIN_MENU_SLOT, simpleItem(Material.COMPASS, MessageUtil.get(plugin.getMessages(), "shop-gui-mainmenu"), mainLore));

        if (showPages) {
            List<String> pageLore = List.of(MessageUtil.get(plugin.getMessages(), "shop-gui-page-indicator")
                    .replace("%page%", String.valueOf(page + 1))
                    .replace("%pages%", String.valueOf(totalPages)));
            if (page > 0) {
                inventory.setItem(PREV_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-prev-page"), pageLore));
            }
            if (page < totalPages - 1) {
                inventory.setItem(NEXT_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-next-page"), pageLore));
            }
        }
    }

    /** Umrahmt einen Titel im Stil "&8«——— Titel ———»", wie im Vorbild-Shop. */
    private String bracketTitle(String rawTitle) {
        return "&8«——— " + rawTitle + "&8 ———»";
    }

    /**
     * Fuellt nur den AEUSSEREN Rand (oberste/unterste Reihe, linke/rechte
     * Spalte) mit schlichten grauen Glasscheiben - bewusst nicht die ganze
     * Flaeche, damit es nicht "vollgemuellt" wirkt. Der Innenbereich bleibt
     * leer (normale Inventar-Optik), bis Items ihn befuellen.
     */
    private void fillBorder(Inventory inventory) {
        ItemStack filler = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        int size = inventory.getSize();
        int rows = size / 9;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                    inventory.setItem(row * 9 + col, filler.clone());
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Klicks
    // ---------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == MAIN_MENU_SLOT && holder.getType() != ShopGuiHolder.Type.MAIN) {
            openMain(player);
            return;
        }

        switch (holder.getType()) {
            case MAIN -> handleMainClick(player, slot);
            case CATEGORY_HUB -> handleHubClick(player, holder, slot);
            case ITEM_LIST -> handleListClick(player, holder, slot);
            case ITEM_DETAIL -> handleDetailClick(player, holder, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        List<ShopCategory> categories = plugin.getShopManager().getCategories();
        int[] slots = centeredSlots(categories.size(), CENTER_ROW_START);
        for (int i = 0; i < categories.size() && i < slots.length; i++) {
            if (slots[i] == slot) {
                openCategory(player, categories.get(i));
                return;
            }
        }
    }

    /** Hat eine Kategorie nur eine Unterkategorie (z.B. Ruestung, Essen), waere die Uebersicht mit nur einem Icon ein sinnloser Extra-Klick - dann direkt zur Item-Liste. */
    private void openCategory(Player player, ShopCategory category) {
        if (category.getSubCategories().size() == 1) {
            openItemList(player, category.getId(), category.getSubCategories().get(0).getId(), 0);
        } else {
            openCategoryHub(player, category.getId());
        }
    }

    private void handleHubClick(Player player, ShopGuiHolder holder, int slot) {
        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        Optional<ShopCategory> categoryOpt = findCategory(holder.getCategoryId());
        if (categoryOpt.isEmpty()) {
            openMain(player);
            return;
        }
        ShopCategory category = categoryOpt.get();
        List<ShopSubCategory> subCategories = category.getSubCategories();
        int[] slots = centeredSlots(subCategories.size(), CENTER_ROW_START);
        for (int i = 0; i < subCategories.size() && i < slots.length; i++) {
            if (slots[i] == slot) {
                openItemList(player, category.getId(), subCategories.get(i).getId(), 0);
                return;
            }
        }
    }

    private void handleListClick(Player player, ShopGuiHolder holder, int slot) {
        Optional<ShopCategory> categoryOpt = findCategory(holder.getCategoryId());
        if (categoryOpt.isEmpty()) {
            openMain(player);
            return;
        }
        ShopCategory category = categoryOpt.get();

        if (slot == BACK_SLOT) {
            // Wurde die Uebersicht beim Reingehen uebersprungen (nur 1 Unterkategorie), auch beim Zurueckgehen ueberspringen
            if (category.getSubCategories().size() == 1) {
                openMain(player);
            } else {
                openCategoryHub(player, category.getId());
            }
            return;
        }
        Optional<ShopSubCategory> subOpt = findSubCategory(category, holder.getSubCategoryId());
        if (subOpt.isEmpty()) {
            openCategoryHub(player, category.getId());
            return;
        }
        ShopSubCategory subCategory = subOpt.get();

        if (slot == PREV_PAGE_SLOT) {
            openItemList(player, category.getId(), subCategory.getId(), holder.getPage() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT) {
            openItemList(player, category.getId(), subCategory.getId(), holder.getPage() + 1);
            return;
        }

        int columns = Math.max(1, Math.min(9, subCategory.getColumns()));
        int itemsPerPage = columns * ITEM_GRID_ROWS;
        int leftPad = (9 - columns) / 2;

        int rowIndex = (slot - ITEM_GRID_START) / 9;
        int colInRow = (slot - ITEM_GRID_START) % 9;
        if (slot < ITEM_GRID_START || rowIndex >= ITEM_GRID_ROWS || colInRow < leftPad || colInRow >= leftPad + columns) {
            return;
        }
        int index = holder.getPage() * itemsPerPage + rowIndex * columns + (colInRow - leftPad);
        if (index < 0 || index >= subCategory.getItems().size()) {
            return;
        }
        ShopItem item = subCategory.getItems().get(index);
        openItemDetail(player, category.getId(), subCategory.getId(), holder.getPage(), item.getId());
    }

    private void handleDetailClick(Player player, ShopGuiHolder holder, int slot) {
        if (slot == BACK_SLOT) {
            openItemList(player, holder.getCategoryId(), holder.getSubCategoryId(), holder.getPage());
            return;
        }
        ShopItem item = plugin.getShopManager().getItem(holder.getItemId());
        if (item == null) {
            openMain(player);
            return;
        }

        for (int i = 0; i < BUY_SLOTS.length; i++) {
            if (BUY_SLOTS[i] == slot) {
                handleBuy(player, item, BUY_AMOUNTS[i]);
                openItemDetail(player, holder.getCategoryId(), holder.getSubCategoryId(), holder.getPage(), holder.getItemId());
                return;
            }
        }
        for (int i = 0; i < SELL_SLOTS.length; i++) {
            if (SELL_SLOTS[i] == slot) {
                handleSell(player, item, SELL_AMOUNTS[i]);
                openItemDetail(player, holder.getCategoryId(), holder.getSubCategoryId(), holder.getPage(), holder.getItemId());
                return;
            }
        }
    }

    private void handleBuy(Player player, ShopItem item, int amount) {
        ShopManager.TransactionResult result = plugin.getShopManager().buy(player, item, amount);
        switch (result) {
            case SUCCESS -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-buy-success")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", item.getDisplayName())
                    .replace("%price%", String.valueOf(item.getBuyPrice() * amount)));
            case NOT_BUYABLE -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-not-buyable"));
            case NOT_ENOUGH_TOKENS -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-not-enough-tokens"));
            case PLAYERPOINTS_MISSING -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-playerpoints-missing"));
            default -> {
            }
        }
    }

    private void handleSell(Player player, ShopItem item, int amount) {
        ShopManager.TransactionResult result = plugin.getShopManager().sell(player, item, amount);
        switch (result) {
            case SUCCESS -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-sell-success")
                    .replace("%item%", item.getDisplayName()));
            case NOT_SELLABLE -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-not-sellable"));
            case NOT_ENOUGH_ITEMS -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-not-enough-items"));
            case PLAYERPOINTS_MISSING -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-playerpoints-missing"));
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------
    // Hilfsfunktionen
    // ---------------------------------------------------------------

    private Optional<ShopCategory> findCategory(String categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return plugin.getShopManager().getCategories().stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst();
    }

    private Optional<ShopSubCategory> findSubCategory(ShopCategory category, String subCategoryId) {
        if (subCategoryId == null) {
            return Optional.empty();
        }
        return category.getSubCategories().stream()
                .filter(s -> s.getId().equals(subCategoryId))
                .findFirst();
    }

    private ItemStack buildListItemStack(ShopItem item) {
        List<String> lore = new ArrayList<>();
        if (item.isBuyable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-buy").replace("%price%", String.valueOf(item.getBuyPrice())));
        }
        if (item.isSellable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-sell").replace("%price%", String.valueOf(item.getSellPrice())));
        }
        lore.add("");
        lore.add(MessageUtil.get(plugin.getMessages(), "shop-gui-list-item-hint"));
        return simpleItem(item.getMaterial(), MessageUtil.color("&l") + item.getDisplayName(), lore);
    }

    private ItemStack simpleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Berechnet zentrierte Slot-Positionen fuer bis zu 9 Icons in einer Reihe, beginnend bei rowStart. */
    private int[] centeredSlots(int count, int rowStart) {
        int clamped = Math.min(count, 9);
        int start = rowStart + Math.max(0, (9 - clamped) / 2);
        int[] slots = new int[clamped];
        for (int i = 0; i < clamped; i++) {
            slots[i] = start + i;
        }
        return slots;
    }
}
