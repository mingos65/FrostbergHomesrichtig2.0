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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Eigenes Shop-GUI, Ersatz fuer EconomyShopGUI. Anders als dort fuehrt ein
 * Klick auf einen Unterkategorie-Tab NICHT in ein neues Fenster, sondern
 * schaltet nur die Item-Liste auf derselben Seite um - so bleibt alles in
 * maximal 2 Klicks erreichbar (Hauptmenue -> Kategorie -> Item).
 *
 * Slot-Layout der Kategorie-Ansicht (54 Slots):
 *  Reihe 0 (0-8):   Zurueck-Button (Slot 0), Tab-Buttons fuer Unterkategorien
 *  Reihen 1-4 (9-44): Item-Raster, 9 Items pro Reihe = 36 pro Seite
 *  Reihe 5 (45-53): Vorherige-Seite (45), Fuell-Glas, Naechste-Seite (53)
 */
public class ShopGuiListener implements Listener {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int ITEM_GRID_START = 9;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_SLOT = 0;
    private static final int BULK_AMOUNT = 64;

    private final FrostbergHomes plugin;

    public ShopGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Oeffnen
    // ---------------------------------------------------------------

    public void openMain(Player player) {
        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.MAIN, null, null, 0);
        String title = MessageUtil.get(plugin.getMessages(), "shop-gui-main-title");
        Inventory inventory = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inventory);

        List<ShopCategory> categories = plugin.getShopManager().getCategories();
        int[] slots = centeredSlots(categories.size(), 9, 27);
        for (int i = 0; i < categories.size(); i++) {
            ShopCategory category = categories.get(i);
            inventory.setItem(slots[i], simpleItem(category.getIcon(), category.getDisplayName(), null));
        }

        player.openInventory(inventory);
    }

    public void openCategory(Player player, String categoryId, String subCategoryId, int page) {
        Optional<ShopCategory> categoryOpt = findCategory(categoryId);
        if (categoryOpt.isEmpty()) {
            openMain(player);
            return;
        }
        ShopCategory category = categoryOpt.get();
        if (category.getSubCategories().isEmpty()) {
            openMain(player);
            return;
        }

        ShopSubCategory subCategory = findSubCategory(category, subCategoryId)
                .orElse(category.getSubCategories().get(0));

        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.CATEGORY, category.getId(), subCategory.getId(), page);
        String title = MessageUtil.get(plugin.getMessages(), "shop-gui-category-title")
                .replace("%category%", category.getDisplayName())
                .replace("%sub%", subCategory.getDisplayName());
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inventory);

        inventory.setItem(BACK_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-back"), null));

        List<ShopSubCategory> subCategories = category.getSubCategories();
        for (int i = 0; i < subCategories.size() && i < 7; i++) {
            ShopSubCategory tab = subCategories.get(i);
            boolean active = tab.getId().equals(subCategory.getId());
            String name = (active ? "&n" : "") + tab.getDisplayName();
            inventory.setItem(1 + i, simpleItem(tab.getIcon(), MessageUtil.color(name), null));
        }

        List<ShopItem> items = subCategory.getItems();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        int offset = clampedPage * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE && offset + i < items.size(); i++) {
            inventory.setItem(ITEM_GRID_START + i, buildItemStack(items.get(offset + i)));
        }

        if (clampedPage > 0) {
            inventory.setItem(PREV_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-prev-page"), null));
        }
        if (clampedPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-next-page"), null));
        }

        player.openInventory(inventory);
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

        if (holder.getType() == ShopGuiHolder.Type.MAIN) {
            handleMainClick(player, slot);
            return;
        }

        handleCategoryClick(player, holder, slot, event.getClick());
    }

    private void handleMainClick(Player player, int slot) {
        List<ShopCategory> categories = plugin.getShopManager().getCategories();
        int[] slots = centeredSlots(categories.size(), 9, 27);
        for (int i = 0; i < categories.size(); i++) {
            if (slots[i] == slot) {
                ShopCategory category = categories.get(i);
                if (!category.getSubCategories().isEmpty()) {
                    openCategory(player, category.getId(), category.getSubCategories().get(0).getId(), 0);
                }
                return;
            }
        }
    }

    private void handleCategoryClick(Player player, ShopGuiHolder holder, int slot, ClickType click) {
        Optional<ShopCategory> categoryOpt = findCategory(holder.getCategoryId());
        if (categoryOpt.isEmpty()) {
            openMain(player);
            return;
        }
        ShopCategory category = categoryOpt.get();

        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == PREV_PAGE_SLOT) {
            openCategory(player, category.getId(), holder.getSubCategoryId(), holder.getPage() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT) {
            openCategory(player, category.getId(), holder.getSubCategoryId(), holder.getPage() + 1);
            return;
        }

        List<ShopSubCategory> subCategories = category.getSubCategories();
        if (slot >= 1 && slot <= 7 && slot - 1 < subCategories.size()) {
            openCategory(player, category.getId(), subCategories.get(slot - 1).getId(), 0);
            return;
        }

        if (slot < ITEM_GRID_START || slot >= ITEM_GRID_START + ITEMS_PER_PAGE) {
            return;
        }

        ShopSubCategory subCategory = findSubCategory(category, holder.getSubCategoryId())
                .orElse(subCategories.get(0));
        int index = holder.getPage() * ITEMS_PER_PAGE + (slot - ITEM_GRID_START);
        if (index < 0 || index >= subCategory.getItems().size()) {
            return;
        }
        ShopItem item = subCategory.getItems().get(index);

        boolean sellClick = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;
        int amount = (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) ? BULK_AMOUNT : 1;

        if (sellClick) {
            handleSell(player, item, amount);
        } else {
            handleBuy(player, item, amount);
        }

        // Ansicht neu aufbauen, damit ein evtl. veraendertes Inventar (Verkauf) sich nicht mit dem GUI ueberschneidet
        openCategory(player, category.getId(), subCategory.getId(), holder.getPage());
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

    private ItemStack buildItemStack(ShopItem item) {
        List<String> lore = new ArrayList<>();
        if (item.isBuyable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-buy").replace("%price%", String.valueOf(item.getBuyPrice())));
        }
        if (item.isSellable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-sell").replace("%price%", String.valueOf(item.getSellPrice())));
        }
        lore.add("");
        if (item.isBuyable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-hint-buy"));
        }
        if (item.isSellable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-hint-sell"));
        }
        return simpleItem(item.getMaterial(), item.getDisplayName(), lore);
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

    /** Berechnet zentrierte Slot-Positionen fuer "count" Icons in der mittleren Reihe eines "size"-grossen Inventars. */
    private int[] centeredSlots(int count, int rowWidth, int size) {
        int middleRowStart = (size / rowWidth / 2) * rowWidth;
        int start = middleRowStart + Math.max(0, (rowWidth - count) / 2);
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = start + i;
        }
        return slots;
    }
}
