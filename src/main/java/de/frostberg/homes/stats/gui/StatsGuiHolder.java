package de.frostberg.homes.stats.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Markiert das /stats-Anzeige-Inventar - rein informativ, keine Interaktion ausser Schliessen. */
public class StatsGuiHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
