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
 * /ec-GUI: bei genau 1 erlaubter Seite direkt die Enderchest-Seite oeffnen,
 * bei mehreren zuerst ein Auswahlmenue. Die Seiten selbst sind ECHTE
 * Aufbewahrungs-Container (Klicks werden NICHT abgebrochen), Speichern
 * passiert beim Schliessen (InventoryCloseEvent).
 */
public class EnderchestGuiListener implements Listener {

    private static final int[] PAGE_SLOTS = {19, 20, 21, 22, 23, 24, 25, 29, 30};

    private final FrostbergHomes plugin;

    public EnderchestGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void openPage(Player player, int page) {
        String name = plugin.getEnderchestManager().getPageName(player.getUniqueId(), page);
        EnderchestGuiHolder holder = new EnderchestGuiHolder(player.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(holder, EnderchestManager.PAGE_SIZE, MessageUtil.color(name));
        holder.setInventory(inventory);
        inventory.setContents(plugin.getEnderchestManager().loadPage(player.getUniqueId(), page));
        player.openInventory(inventory);
    }

    public void openSelector(Player player) {
        int limit = plugin.getEnderchestManager().getPageLimit(player);
        EnderchestSelectorHolder holder = new EnderchestSelectorHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54,
                MessageUtil.color(MessageUtil.get(plugin.getMessages(), "ec-selector-title")));
        holder.setInventory(inventory);

        fillBorder(inventory);

        for (int i = 0; i < limit && i < PAGE_SLOTS.length; i++) {
            String name = plugin.getEnderchestManager().getPageName(player.getUniqueId(), i);
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), "ec-selector-lore"));
            inventory.setItem(PAGE_SLOTS[i], simpleItem(Material.ENDER_CHEST, MessageUtil.color("&d") + name, lore));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof EnderchestSelectorHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();
            for (int i = 0; i < PAGE_SLOTS.length; i++) {
                if (PAGE_SLOTS[i] == slot) {
                    openPage(player, i);
                    return;
                }
            }
        }
        // EnderchestGuiHolder: bewusst NICHT abbrechen, ist ein echter Container
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnderchestGuiHolder holder) {
            plugin.getEnderchestManager().savePage(holder.getPlayerId(), holder.getPage(), event.getInventory().getContents());
        }
    }

    private void fillBorder(Inventory inventory) {
        ItemStack filler = simpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        int size = inventory.getSize();
        int rows = size / 9;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                    inventory.setItem(row * 9 + col, filler.clone());
                }
            }
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
