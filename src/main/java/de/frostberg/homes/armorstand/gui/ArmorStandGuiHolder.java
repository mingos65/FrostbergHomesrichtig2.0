package de.frostberg.homes.armorstand.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Markiert das Armor-Stand-Editor-Inventar, haelt die UUID des bearbeiteten Armor Stands. */
public class ArmorStandGuiHolder implements InventoryHolder {

    private final UUID armorStandId;
    private Inventory inventory;

    public ArmorStandGuiHolder(UUID armorStandId) {
        this.armorStandId = armorStandId;
    }

    public UUID getArmorStandId() {
        return armorStandId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
