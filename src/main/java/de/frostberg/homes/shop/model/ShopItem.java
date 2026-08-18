package de.frostberg.homes.shop.model;

import org.bukkit.Material;

/**
 * Ein einzelnes kaufbares/verkaufbares Item im eigenen Shop. buy/sell sind
 * Preise in Tokens, -1 bedeutet "in dieser Richtung nicht handelbar" (z.B.
 * Loot-only-Items koennen nur verkauft, nicht gekauft werden).
 */
public class ShopItem {

    private final String id;
    private final Material material;
    private final String displayName;
    private final long buyPrice;
    private final long sellPrice;
    private final String potionType; // Name eines org.bukkit.potion.PotionType-Wertes, oder null fuer normale Items

    public ShopItem(String id, Material material, String displayName, long buyPrice, long sellPrice, String potionType) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.potionType = potionType;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getBuyPrice() {
        return buyPrice;
    }

    public long getSellPrice() {
        return sellPrice;
    }

    public boolean isBuyable() {
        return buyPrice > 0;
    }

    public boolean isSellable() {
        return sellPrice > 0;
    }

    public String getPotionType() {
        return potionType;
    }
}
