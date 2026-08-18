package de.frostberg.homes.shop.manager;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.shop.model.ShopCategory;
import de.frostberg.homes.shop.model.ShopItem;
import de.frostberg.homes.shop.model.ShopSubCategory;
import de.frostberg.homes.util.CurrencyBridge;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Laedt shop-items.yml (eigenes Format, unabhaengig von EconomyShopGUI) und
 * haelt alle Kategorien/Items im Speicher. Kauf/Verkauf laeuft ausschliesslich
 * in Tokens ueber CurrencyBridge/PlayerPoints - keine eigene Geld-Logik,
 * genau wie bei /pay und der Clan-Kasse.
 */
public class ShopManager {

    /** Ergebnis eines Kauf-/Verkaufsversuchs, damit der Listener nur noch die passende Nachricht auswaehlen muss. */
    public enum TransactionResult {
        SUCCESS,
        NOT_BUYABLE,
        NOT_SELLABLE,
        NOT_ENOUGH_TOKENS,
        NOT_ENOUGH_ITEMS,
        NOT_ENOUGH_SPACE,
        PLAYERPOINTS_MISSING
    }

    private final FrostbergHomes plugin;
    private final List<ShopCategory> categories = new ArrayList<>();
    private final Map<String, ShopItem> itemsById = new LinkedHashMap<>();

    public ShopManager(FrostbergHomes plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        categories.clear();
        itemsById.clear();

        plugin.saveResource("shop-items.yml", true);
        File file = new File(plugin.getDataFolder(), "shop-items.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) {
            return;
        }

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
            if (categorySection == null) {
                continue;
            }

            String categoryDisplayName = MessageUtil.color(categorySection.getString("display-name", categoryId));
            Material categoryIcon = parseMaterial(categorySection.getString("icon"), Material.CHEST);
            Material borderMaterial = parseMaterial(categorySection.getString("border"), Material.GRAY_STAINED_GLASS_PANE);

            List<ShopSubCategory> subCategories = new ArrayList<>();
            ConfigurationSection subSection = categorySection.getConfigurationSection("sub-categories");
            if (subSection != null) {
                for (String subId : subSection.getKeys(false)) {
                    ConfigurationSection sub = subSection.getConfigurationSection(subId);
                    if (sub == null) {
                        continue;
                    }
                    subCategories.add(loadSubCategory(subId, sub));
                }
            }

            categories.add(new ShopCategory(categoryId, categoryDisplayName, categoryIcon, borderMaterial, subCategories));
        }
    }

    private ShopSubCategory loadSubCategory(String subId, ConfigurationSection sub) {
        String subDisplayName = MessageUtil.color(sub.getString("display-name", subId));
        Material subIcon = parseMaterial(sub.getString("icon"), Material.CHEST);
        int columns = sub.getInt("columns", 9);

        List<ShopItem> items = new ArrayList<>();
        ConfigurationSection itemsSection = sub.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                if (itemSection == null) {
                    continue;
                }
                Material material = parseMaterial(itemSection.getString("material"), null);
                if (material == null) {
                    plugin.getLogger().warning("shop-items.yml: unbekanntes Material bei Item '" + itemId + "', wird uebersprungen.");
                    continue;
                }
                String name = MessageUtil.color(itemSection.getString("name", itemId));
                long buy = itemSection.getLong("buy", -1);
                long sell = itemSection.getLong("sell", -1);
                String potionType = itemSection.getString("potion");

                ShopItem item = new ShopItem(itemId, material, name, buy, sell, potionType);
                items.add(item);
                itemsById.put(itemId, item);
            }
        }

        return new ShopSubCategory(subId, subDisplayName, subIcon, columns, items);
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw);
        return material != null ? material : fallback;
    }

    public List<ShopCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public ShopItem getItem(String id) {
        return itemsById.get(id);
    }

    /** Kauft "amount" Stueck eines Items und uebergibt sie direkt ins Inventar des Spielers. */
    public TransactionResult buy(Player player, ShopItem item, int amount) {
        if (!item.isBuyable()) {
            return TransactionResult.NOT_BUYABLE;
        }
        if (!hasInventorySpace(player, item.getMaterial(), amount)) {
            return TransactionResult.NOT_ENOUGH_SPACE;
        }
        long totalPrice = item.getBuyPrice() * amount;
        long balance = CurrencyBridge.readTokenBalance(player);
        if (balance < 0) {
            return TransactionResult.PLAYERPOINTS_MISSING;
        }
        if (balance < totalPrice) {
            return TransactionResult.NOT_ENOUGH_TOKENS;
        }
        if (!CurrencyBridge.takeTokens(player, totalPrice)) {
            return TransactionResult.PLAYERPOINTS_MISSING;
        }
        player.getInventory().addItem(buildPurchasedStack(item, amount));
        return TransactionResult.SUCCESS;
    }

    /**
     * Prueft, ob "amount" Stueck von "material" komplett im Spielerinventar
     * Platz finden wuerden (leere Slots + Platz in bereits vorhandenen,
     * nicht vollen Stacks desselben Materials) - simuliert dieselbe Logik,
     * die Inventory#addItem spaeter tatsaechlich anwendet, damit vorher
     * geprueft werden kann, ohne schon Tokens abzuziehen.
     */
    private boolean hasInventorySpace(Player player, Material material, int amount) {
        int maxStackSize = material.getMaxStackSize();
        int remaining = amount;
        for (org.bukkit.inventory.ItemStack stack : player.getInventory().getStorageContents()) {
            if (remaining <= 0) {
                return true;
            }
            if (stack == null || stack.getType() == Material.AIR) {
                remaining -= maxStackSize;
            } else if (stack.getType() == material && stack.getAmount() < maxStackSize) {
                remaining -= (maxStackSize - stack.getAmount());
            }
        }
        return remaining <= 0;
    }

    /**
     * Baut den tatsaechlich ausgegebenen ItemStack. Braeu/Splash-Traenke
     * brauchen zusaetzlich zum Material einen PotionType, sonst waeren es nur
     * wirkungslose leere Fläschchen (siehe "potion:"-Feld in shop-items.yml).
     */
    private org.bukkit.inventory.ItemStack buildPurchasedStack(ShopItem item, int amount) {
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(item.getMaterial(), amount);
        if (item.getPotionType() == null) {
            return stack;
        }
        if (!(stack.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta potionMeta)) {
            return stack;
        }
        try {
            org.bukkit.potion.PotionType potionType = org.bukkit.potion.PotionType.valueOf(item.getPotionType());
            potionMeta.setBasePotionType(potionType);
            stack.setItemMeta(potionMeta);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("shop-items.yml: unbekannter PotionType '" + item.getPotionType() + "' bei Item '" + item.getId() + "'.");
        }
        return stack;
    }

    /** Verkauft bis zu "amount" Stueck aus dem Inventar des Spielers, entfernt hoechstens das, was er tatsaechlich besitzt. */
    public TransactionResult sell(Player player, ShopItem item, int amount) {
        if (!item.isSellable()) {
            return TransactionResult.NOT_SELLABLE;
        }
        int owned = countInInventory(player, item.getMaterial());
        if (owned <= 0) {
            return TransactionResult.NOT_ENOUGH_ITEMS;
        }
        int toSell = Math.min(owned, amount);
        long payout = item.getSellPrice() * toSell;

        player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(item.getMaterial(), toSell));
        if (!CurrencyBridge.giveTokens(player, payout)) {
            // Rueckgaengig machen, falls die Gutschrift fehlschlaegt (z.B. PlayerPoints nicht verfuegbar)
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(item.getMaterial(), toSell));
            return TransactionResult.PLAYERPOINTS_MISSING;
        }
        return TransactionResult.SUCCESS;
    }

    private int countInInventory(Player player, Material material) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }
}
