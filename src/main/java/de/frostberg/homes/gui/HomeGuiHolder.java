package de.frostberg.homes.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert alle GUI-Inventare des /homes-Systems, damit HomesGuiListener sie
 * im InventoryClickEvent zuverlaessig ueber getHolder() erkennen kann - statt
 * sich brueichig auf den Titel-String zu verlassen.
 */
public class HomeGuiHolder implements InventoryHolder {

    public enum Type {
        MENU,
        DETAIL,
        CONFIRM_DELETE,
        RENAME
    }

    private final Type type;
    private final int homeNumber; // nur relevant fuer DETAIL/CONFIRM_DELETE/RENAME
    private final int page; // nur relevant fuer MENU (0-indexiert)
    private Inventory inventory;

    public HomeGuiHolder(Type type, int homeNumber) {
        this(type, homeNumber, 0);
    }

    public HomeGuiHolder(Type type, int homeNumber, int page) {
        this.type = type;
        this.homeNumber = homeNumber;
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

    public int getHomeNumber() {
        return homeNumber;
    }

    public int getPage() {
        return page;
    }
}
