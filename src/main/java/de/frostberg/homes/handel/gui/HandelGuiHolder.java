package de.frostberg.homes.handel.gui;

import de.frostberg.homes.handel.TradeSession;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Markiert das gemeinsame Handel-Inventar, haelt die zugehoerige TradeSession. */
public class HandelGuiHolder implements InventoryHolder {

    private final TradeSession session;
    private Inventory inventory;

    public HandelGuiHolder(TradeSession session) {
        this.session = session;
    }

    public TradeSession getSession() {
        return session;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
