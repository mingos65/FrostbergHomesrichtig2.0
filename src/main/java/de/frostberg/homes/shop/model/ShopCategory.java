package de.frostberg.homes.shop.model;

import org.bukkit.Material;

import java.util.List;

/**
 * Eine Hauptkategorie im /shop-Hauptmenue (z.B. "Kampf"). Enthaelt eine oder
 * mehrere Unterkategorien (Tabs), die auf derselben Kategorie-Seite
 * umgeschaltet werden.
 */
public class ShopCategory {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final Material borderMaterial;
    private final List<ShopSubCategory> subCategories;

    public ShopCategory(String id, String displayName, Material icon, Material borderMaterial, List<ShopSubCategory> subCategories) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.borderMaterial = borderMaterial;
        this.subCategories = subCategories;
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

    public Material getBorderMaterial() {
        return borderMaterial;
    }

    public List<ShopSubCategory> getSubCategories() {
        return subCategories;
    }
}
