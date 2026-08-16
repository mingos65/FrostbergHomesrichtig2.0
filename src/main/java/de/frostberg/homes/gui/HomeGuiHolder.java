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
    private Inventory inventory;

    public HomeGuiHolder(Type type, int homeNumber) {
        this.type = type;
        this.homeNumber = homeNumber;
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
}
