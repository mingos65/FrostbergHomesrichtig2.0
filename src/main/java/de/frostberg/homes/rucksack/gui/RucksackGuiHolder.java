package de.frostberg.homes.rucksack.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Markiert den geoeffneten Rucksack - echter Aufbewahrungs-Container, Klicks werden NICHT abgebrochen. */
public class RucksackGuiHolder implements InventoryHolder {

    private final UUID playerId;
    private Inventory inventory;

    public RucksackGuiHolder(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
