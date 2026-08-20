package de.frostberg.homes.bank.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Markiert das Bank-Inventar, damit BankGuiListener es im InventoryClickEvent zuverlaessig erkennt. */
public class BankGuiHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
