package de.frostberg.homes.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert alle Shop-Inventare, damit ShopGuiListener sie im
 * InventoryClickEvent zuverlaessig ueber getHolder() erkennt statt sich auf
 * den Fenstertitel zu verlassen (gleiches Muster wie HomeGuiHolder).
 *
 * Vier Ebenen: MAIN (Hauptmenue) -> CATEGORY_HUB (Icons der
 * Unterkategorien) -> ITEM_LIST (die eigentlichen Items einer
 * Unterkategorie) -> ITEM_DETAIL (Kauf/Verkauf-Fenster fuer ein einzelnes
 * Item).
 */
public class ShopGuiHolder implements InventoryHolder {

    public enum Type {
        MAIN,
        CATEGORY_HUB,
        ITEM_LIST,
        ITEM_DETAIL
    }

    private final Type type;
    private final String categoryId;
    private final String subCategoryId;
    private final int page;
    private final String itemId;
    private Inventory inventory;

    public ShopGuiHolder(Type type, String categoryId, String subCategoryId, int page, String itemId) {
        this.type = type;
        this.categoryId = categoryId;
        this.subCategoryId = subCategoryId;
        this.page = page;
        this.itemId = itemId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Type getType() {
        return type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getSubCategoryId() {
        return subCategoryId;
    }

    public int getPage() {
        return page;
    }

    public String getItemId() {
        return itemId;
    }
}
