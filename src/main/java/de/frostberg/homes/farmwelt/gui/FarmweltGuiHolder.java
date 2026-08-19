package de.frostberg.homes.farmwelt.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert das Farmwelt-Auswahl-Inventar, damit FarmweltGuiListener es im
 * InventoryClickEvent zuverlaessig ueber getHolder() erkennt (gleiches
 * Muster wie ShopGuiHolder/HomeGuiHolder). Nur ein einziger Screen mit 3
 * Optionen - kein Zustand wie Seiten/Kategorien noetig.
 */
public class FarmweltGuiHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
