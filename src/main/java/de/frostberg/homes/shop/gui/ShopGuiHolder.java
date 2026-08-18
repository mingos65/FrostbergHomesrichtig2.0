package de.frostberg.homes.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert alle Shop-Inventare, damit ShopGuiListener sie im
 * InventoryClickEvent zuverlaessig ueber getHolder() erkennt statt sich auf
 * den Fenstertitel zu verlassen (gleiches Muster wie HomeGuiHolder).
 */
public class ShopGuiHolder implements InventoryHolder {

    public enum Type {
        MAIN,
        CATEGORY
    }

    private final Type type;
    private final String categoryId;
    private final String subCategoryId;
    private final int page;
    private Inventory inventory;

    public ShopGuiHolder(Type type, String categoryId, String subCategoryId, int page) {
        this.type = type;
        this.categoryId = categoryId;
        this.subCategoryId = subCategoryId;
        this.page = page;
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
}
