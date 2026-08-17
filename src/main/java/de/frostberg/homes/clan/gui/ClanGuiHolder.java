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
        MEMBERS,
        CONFIRM_DELETE,
        CONFIRM_LEAVE,
        COLOR
    }

    private final Type type;
    private final String clanName; // relevant fuer MEMBERS/CONFIRM_DELETE/CONFIRM_LEAVE
    private final int page; // relevant fuer LIST (aktuelle Seite) und MEMBERS (Ruecksprung-Seite)
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
