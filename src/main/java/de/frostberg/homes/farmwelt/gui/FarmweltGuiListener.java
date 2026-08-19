package de.frostberg.homes.farmwelt.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.farmwelt.FarmType;
import de.frostberg.homes.farmwelt.util.FarmTeleportService;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * /farmwelt-Auswahlmenue: 3 Icons (Overworld/Nether/End) in der mittleren
 * Reihe, je 2 leere Slots dazwischen/daneben, gleicher Rahmen-Stil wie
 * Shop-/Home-GUI. Ein Klick teleportiert direkt ueber FarmTeleportService.
 */
public class FarmweltGuiListener implements Listener {

    private static final int[] TYPE_SLOTS = {19, 22, 25}; // Overworld, Nether, End - je 2 Slots Abstand
    private static final int CLOSE_SLOT = 49;

    private final FrostbergHomes plugin;

    public FarmweltGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FarmweltGuiHolder holder = new FarmweltGuiHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54,
                MessageUtil.color(MessageUtil.get(plugin.getMessages(), "farmwelt-gui-title")));
        holder.setInventory(inventory);

        fillBorder(inventory);

        FarmType[] types = FarmType.values();
        for (int i = 0; i < types.length && i < TYPE_SLOTS.length; i++) {
            inventory.setItem(TYPE_SLOTS[i], buildTypeItem(types[i]));
        }

        List<String> closeLore = List.of(MessageUtil.get(plugin.getMessages(), "farmwelt-gui-close-lore"));
        inventory.setItem(CLOSE_SLOT, simpleItem(Material.ARROW,
                MessageUtil.get(plugin.getMessages(), "farmwelt-gui-close"), closeLore));

        player.openInventory(inventory);
    }

    private ItemStack buildTypeItem(FarmType type) {
        String key = type.getConfigKey();
        return simpleItem(type.getIcon(),
                MessageUtil.get(plugin.getMessages(), "farmwelt-gui-" + key + "-name"), null);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FarmweltGuiHolder)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        FarmType[] types = FarmType.values();
        for (int i = 0; i < types.length && i < TYPE_SLOTS.length; i++) {
            if (TYPE_SLOTS[i] == slot) {
                player.closeInventory();
                FarmTeleportService.teleport(plugin, player, types[i]);
                return;
            }
        }
    }

    /** Fuellt nur den aeusseren Rand mit grauen Glasscheiben, gleiche Optik wie Shop-/Home-GUI. */
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
