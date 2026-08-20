package de.frostberg.homes.enderchest.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.enderchest.EnderchestManager;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * /ec-GUI: 36 Slots, die ersten 27 (Reihe 1-3) sind echter Speicher (Klicks
 * dort NICHT abgebrochen), die letzte Reihe ist eine reine Navigationsleiste
 * (Zurueck-Pfeil / Seiten-Anzeige / Weiter-Pfeil, Klicks dort abgebrochen).
 * Kein separates Auswahlmenue mehr - bei mehreren erlaubten Seiten blaettert
 * man direkt in der geoeffneten Seite durch.
 */
public class EnderchestGuiListener implements Listener {

    private static final int WINDOW_SIZE = 36;
    private static final int PREV_SLOT = 27;
    private static final int INFO_SLOT = 31;
    private static final int NEXT_SLOT = 35;

    private final FrostbergHomes plugin;

    public EnderchestGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void openPage(Player player, int page) {
        int limit = plugin.getEnderchestManager().getPageLimit(player);
        int clampedPage = Math.max(0, Math.min(page, limit - 1));

        EnderchestGuiHolder holder = new EnderchestGuiHolder(player.getUniqueId(), clampedPage);
        String title = MessageUtil.get(plugin.getMessages(), "ec-title");
        Inventory inventory = Bukkit.createInventory(holder, WINDOW_SIZE, MessageUtil.color(title));
        holder.setInventory(inventory);

        ItemStack[] stored = plugin.getEnderchestManager().loadPage(player.getUniqueId(), clampedPage);
        for (int i = 0; i < stored.length; i++) {
            inventory.setItem(i, stored[i]);
        }

        renderNav(inventory, clampedPage, limit);

        player.openInventory(inventory);
    }

    private void renderNav(Inventory inventory, int page, int limit) {
        ItemStack filler = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = EnderchestManager.PAGE_SIZE; i < WINDOW_SIZE; i++) {
            inventory.setItem(i, filler.clone());
        }

        if (page > 0) {
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), "ec-prev-lore"));
            inventory.setItem(PREV_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "ec-prev"), lore));
        }
        if (page < limit - 1) {
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), "ec-next-lore"));
            inventory.setItem(NEXT_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "ec-next"), lore));
        }

        String info = MessageUtil.get(plugin.getMessages(), "ec-page-indicator")
                .replace("%page%", String.valueOf(page + 1))
                .replace("%pages%", String.valueOf(limit));
        inventory.setItem(INFO_SLOT, simpleItem(Material.ENDER_CHEST, info, null));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EnderchestGuiHolder holder)) {
            return;
        }

        boolean inTopInventory = event.getRawSlot() < event.getView().getTopInventory().getSize();
        if (!inTopInventory) {
            return;
        }

        int slot = event.getSlot();
        if (slot < EnderchestManager.PAGE_SIZE) {
            // Echter Speicher-Slot - normal erlaubt
            return;
        }

        // Navigationsleiste - nie Items reinlegen/rausnehmen
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int limit = plugin.getEnderchestManager().getPageLimit(player);

        if (slot == PREV_SLOT && holder.getPage() > 0) {
            plugin.getEnderchestManager().savePage(player.getUniqueId(), holder.getPage(), event.getInventory().getContents());
            openPage(player, holder.getPage() - 1);
        } else if (slot == NEXT_SLOT && holder.getPage() < limit - 1) {
            plugin.getEnderchestManager().savePage(player.getUniqueId(), holder.getPage(), event.getInventory().getContents());
            openPage(player, holder.getPage() + 1);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnderchestGuiHolder holder) {
            plugin.getEnderchestManager().savePage(holder.getPlayerId(), holder.getPage(), event.getInventory().getContents());
        }
    }

    private ItemStack simpleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.color(name));
            if (lore != null) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(MessageUtil.color(line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
