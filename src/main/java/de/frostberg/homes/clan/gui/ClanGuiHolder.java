package de.frostberg.homes.clan.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert alle GUI-Inventare des Clan-Systems, analog zu
 * de.frostberg.homes.gui.HomeGuiHolder.
 */
public class ClanGuiHolder implements InventoryHolder {

    public enum Type {
        LIST,
        CONFIRM_DELETE
    }

    private final Type type;
    private final String clanName; // nur relevant fuer CONFIRM_DELETE
    private final int page; // nur relevant fuer LIST
    private Inventory inventory;

    public ClanGuiHolder(Type type, String clanName, int page) {
        this.type = type;
        this.clanName = clanName;
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

    public String getClanName() {
        return clanName;
    }

    public int getPage() {
        return page;
    }
}
