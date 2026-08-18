package de.frostberg.homes.shop.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.shop.manager.ShopManager;
import de.frostberg.homes.shop.model.ShopCategory;
import de.frostberg.homes.shop.model.ShopItem;
import de.frostberg.homes.shop.model.ShopSubCategory;
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
 *  Reihe 0 (0-8):     Zurueck-Button (Slot 0), Tab-Buttons fuer Unterkategorien
 *  Reihen 1-4 (9-44): Item-Raster, 9 Items pro Reihe = 36 pro Seite
 *  Reihe 5 (45-53):   Vorherige-Seite (45), Kaufmenge 1/16/32/64 (46-49),
 *                      Verkaufmenge 1/32/64 (50-52), Naechste-Seite (53)
 * Alle nicht belegten Slots werden mit Glas-Scheiben gefuellt (Rahmen-Look,
 * gleiches Prinzip wie bei EconomyShopGUI), statt leer/schwarz zu bleiben.
 */
public class ShopGuiListener implements Listener {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int ITEM_GRID_START = 9;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_SLOT = 0;

    private static final int[] BUY_AMOUNT_SLOTS = {46, 47, 48, 49};
    private static final int[] BUY_AMOUNTS = {1, 16, 32, 64};
    private static final int[] SELL_AMOUNT_SLOTS = {50, 51, 52};
    private static final int[] SELL_AMOUNTS = {1, 32, 64};

    private final FrostbergHomes plugin;

    public ShopGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Oeffnen
    // ---------------------------------------------------------------

    public void openMain(Player player) {
        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.MAIN, null, null, 0, 1, 1);
        String title = bracketTitle(MessageUtil.get(plugin.getMessages(), "shop-gui-main-title"));
        Inventory inventory = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inventory);

        fillBorder(inventory, Material.BLACK_STAINED_GLASS_PANE);

        List<ShopCategory> categories = plugin.getShopManager().getCategories();
        int[] slots = centeredSlots(categories.size(), 9, 27);
        for (int i = 0; i < categories.size(); i++) {
            ShopCategory category = categories.get(i);
            List<String> lore = new ArrayList<>();
            for (ShopSubCategory sub : category.getSubCategories()) {
                lore.add(MessageUtil.color("&7» " + sub.getDisplayName()));
            }
            lore.add("");
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-gui-main-hint"));
            inventory.setItem(slots[i], simpleItem(category.getIcon(), category.getDisplayName(), lore));
        }

        player.openInventory(inventory);
    }

    /** Umrahmt einen Titel im Stil "&8«——— Titel ———»", wie im Vorbild-Shop. */
    private String bracketTitle(String rawTitle) {
        return "&8«——— " + rawTitle + "&8 ———»";
    }

    public void openCategory(Player player, String categoryId, String subCategoryId, int page) {
        openCategory(player, categoryId, subCategoryId, page, 1, 1);
    }

    public void openCategory(Player player, String categoryId, String subCategoryId, int page, int buyAmount, int sellAmount) {
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

        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.CATEGORY, category.getId(), subCategory.getId(), page, buyAmount, sellAmount);
        String title = bracketTitle(MessageUtil.get(plugin.getMessages(), "shop-gui-category-title")
                .replace("%category%", category.getDisplayName())
                .replace("%sub%", subCategory.getDisplayName()));
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inventory);

        fillBorder(inventory, category.getBorderMaterial());

        List<String> backLore = List.of(MessageUtil.get(plugin.getMessages(), "shop-gui-back-lore"));
        inventory.setItem(BACK_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-back"), backLore));

        List<ShopSubCategory> subCategories = category.getSubCategories();
        for (int i = 0; i < subCategories.size() && i < 7; i++) {
            ShopSubCategory tab = subCategories.get(i);
            boolean active = tab.getId().equals(subCategory.getId());
            String name = (active ? "&n&l" : "&l") + tab.getDisplayName();
            List<String> tabLore = active
                    ? List.of(MessageUtil.get(plugin.getMessages(), "shop-gui-tab-active"))
                    : List.of(MessageUtil.get(plugin.getMessages(), "shop-gui-tab-hint"));
            inventory.setItem(1 + i, simpleItem(tab.getIcon(), MessageUtil.color(name), tabLore));
        }

        List<ShopItem> items = subCategory.getItems();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        int offset = clampedPage * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE && offset + i < items.size(); i++) {
            inventory.setItem(ITEM_GRID_START + i, buildItemStack(items.get(offset + i), buyAmount, sellAmount));
        }

        buildAmountSelector(inventory, buyAmount, sellAmount);

        if (totalPages > 1) {
            List<String> pageLore = List.of(MessageUtil.get(plugin.getMessages(), "shop-gui-page-indicator")
                    .replace("%page%", String.valueOf(clampedPage + 1))
                    .replace("%pages%", String.valueOf(totalPages)));
            if (clampedPage > 0) {
                inventory.setItem(PREV_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-prev-page"), pageLore));
            }
            if (clampedPage < totalPages - 1) {
                inventory.setItem(NEXT_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "shop-gui-next-page"), pageLore));
            }
        }

        player.openInventory(inventory);
    }

    /** Baut die 4 Kauf- und 3 Verkaufmengen-Knoepfe, die aktuell ausgewaehlte Menge ist unterstrichen+fett markiert und leuchtet. */
    private void buildAmountSelector(Inventory inventory, int buyAmount, int sellAmount) {
        for (int i = 0; i < BUY_AMOUNT_SLOTS.length; i++) {
            int amount = BUY_AMOUNTS[i];
            boolean active = amount == buyAmount;
            String name = (active ? "&n&l" : "&l") + MessageUtil.get(plugin.getMessages(), "shop-amount-buy").replace("%amount%", String.valueOf(amount));
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), active ? "shop-amount-active" : "shop-amount-hint"));
            Material mat = active ? Material.LIME_DYE : Material.GREEN_DYE;
            ItemStack stack = simpleItem(mat, MessageUtil.color(name), lore);
            if (active) {
                glow(stack);
            }
            inventory.setItem(BUY_AMOUNT_SLOTS[i], stack);
        }
        for (int i = 0; i < SELL_AMOUNT_SLOTS.length; i++) {
            int amount = SELL_AMOUNTS[i];
            boolean active = amount == sellAmount;
            String name = (active ? "&n&l" : "&l") + MessageUtil.get(plugin.getMessages(), "shop-amount-sell").replace("%amount%", String.valueOf(amount));
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), active ? "shop-amount-active" : "shop-amount-hint"));
            Material mat = active ? Material.RED_DYE : Material.ORANGE_DYE;
            ItemStack stack = simpleItem(mat, MessageUtil.color(name), lore);
            if (active) {
                glow(stack);
            }
            inventory.setItem(SELL_AMOUNT_SLOTS[i], stack);
        }
    }

    /** Fuellt jeden noch leeren Slot mit einer zur Kategorie passenden Glasscheibe, damit das Fenster wie bei EconomyShopGUI einen Rahmen statt leerer Flaechen hat. */
    private void fillBorder(Inventory inventory, Material borderMaterial) {
        ItemStack filler = simpleItem(borderMaterial, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }
    }

    /** Laesst ein Item leicht leuchten (Verzauberungs-Glanz ohne sichtbare Verzauberung in der Lore), fuer die aktive Mengen-Auswahl. */
    private void glow(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
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
        int buyAmount = holder.getBuyAmount();
        int sellAmount = holder.getSellAmount();

        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == PREV_PAGE_SLOT) {
            openCategory(player, category.getId(), holder.getSubCategoryId(), holder.getPage() - 1, buyAmount, sellAmount);
            return;
        }
        if (slot == NEXT_PAGE_SLOT) {
            openCategory(player, category.getId(), holder.getSubCategoryId(), holder.getPage() + 1, buyAmount, sellAmount);
            return;
        }

        for (int i = 0; i < BUY_AMOUNT_SLOTS.length; i++) {
            if (BUY_AMOUNT_SLOTS[i] == slot) {
                openCategory(player, category.getId(), holder.getSubCategoryId(), holder.getPage(), BUY_AMOUNTS[i], sellAmount);
                return;
            }
        }
        for (int i = 0; i < SELL_AMOUNT_SLOTS.length; i++) {
            if (SELL_AMOUNT_SLOTS[i] == slot) {
                openCategory(player, category.getId(), holder.getSubCategoryId(), holder.getPage(), buyAmount, SELL_AMOUNTS[i]);
                return;
            }
        }

        List<ShopSubCategory> subCategories = category.getSubCategories();
        if (slot >= 1 && slot <= 7 && slot - 1 < subCategories.size()) {
            openCategory(player, category.getId(), subCategories.get(slot - 1).getId(), 0, buyAmount, sellAmount);
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
        if (sellClick) {
            handleSell(player, item, sellAmount);
        } else {
            handleBuy(player, item, buyAmount);
        }

        // Ansicht neu aufbauen, damit ein evtl. veraendertes Inventar (Verkauf) sich nicht mit dem GUI ueberschneidet
        openCategory(player, category.getId(), subCategory.getId(), holder.getPage(), buyAmount, sellAmount);
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

    private ItemStack buildItemStack(ShopItem item, int buyAmount, int sellAmount) {
        List<String> lore = new ArrayList<>();
        if (item.isBuyable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-buy").replace("%price%", String.valueOf(item.getBuyPrice())));
        }
        if (item.isSellable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-sell").replace("%price%", String.valueOf(item.getSellPrice())));
        }
        lore.add("");
        if (item.isBuyable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-hint-buy")
                    .replace("%amount%", String.valueOf(buyAmount))
                    .replace("%total%", String.valueOf(item.getBuyPrice() * buyAmount)));
        }
        if (item.isSellable()) {
            lore.add(MessageUtil.get(plugin.getMessages(), "shop-item-lore-hint-sell")
                    .replace("%amount%", String.valueOf(sellAmount))
                    .replace("%total%", String.valueOf(item.getSellPrice() * sellAmount)));
        }
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
