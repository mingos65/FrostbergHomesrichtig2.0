package de.frostberg.homes.enderchest.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Markiert das Seiten-Auswahlmenue (nur bei mehr als 1 Enderchest-Seite noetig). */
public class EnderchestSelectorHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
