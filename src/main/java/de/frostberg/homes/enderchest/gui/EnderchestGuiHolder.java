package de.frostberg.homes.enderchest.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Markiert eine geoeffnete Enderchest-Seite - im Gegensatz zu den Menue-GUIs ein echter Aufbewahrungs-Container, Klicks werden NICHT abgebrochen. */
public class EnderchestGuiHolder implements InventoryHolder {

    private final UUID playerId;
    private final int page;
    private Inventory inventory;

    public EnderchestGuiHolder(UUID playerId, int page) {
        this.playerId = playerId;
        this.page = page;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
