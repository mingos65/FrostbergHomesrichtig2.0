package de.frostberg.homes.quest.gui;

import de.frostberg.homes.quest.model.QuestCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert alle GUI-Inventare des Quest-Systems, damit QuestGuiListener sie
 * im InventoryClickEvent zuverlaessig ueber getHolder() erkennt - wie
 * HomeGuiHolder/ClanGuiHolder statt sich auf den Fenstertitel zu verlassen.
 */
public class QuestGuiHolder implements InventoryHolder {

    public enum Type {
        MAIN,
        CATEGORY
    }

    private final Type type;
    private final QuestCategory category; // nur relevant fuer CATEGORY
    private Inventory inventory;

    public QuestGuiHolder(Type type, QuestCategory category) {
        this.type = type;
        this.category = category;
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

    public QuestCategory getCategory() {
        return category;
    }
}
