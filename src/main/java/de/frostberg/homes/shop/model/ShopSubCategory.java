package de.frostberg.homes.shop.model;

import org.bukkit.Material;

import java.util.List;

/**
 * Ein Tab innerhalb einer Hauptkategorie (z.B. "Ruestung" innerhalb von
 * "Kampf"). Anders als bei EconomyShopGUI oeffnet ein Tab KEIN eigenes
 * Unterfenster, sondern schaltet nur die Item-Liste auf derselben Seite um -
 * damit man nicht durch mehrere GUI-Ebenen klicken muss.
 */
public class ShopSubCategory {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final int columns;
    private final List<ShopItem> items;

    public ShopSubCategory(String id, String displayName, Material icon, int columns, List<ShopItem> items) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.columns = columns;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getColumns() {
        return columns;
    }

    public List<ShopItem> getItems() {
        return items;
    }
}
