package de.frostberg.homes.rucksack.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/** /rs-GUI: echter Aufbewahrungs-Container, Speichern passiert beim Schliessen. */
public class RucksackGuiListener implements Listener {

    private final FrostbergHomes plugin;

    public RucksackGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        RucksackGuiHolder holder = new RucksackGuiHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, plugin.getRucksackManager().getSize(),
                MessageUtil.color(MessageUtil.get(plugin.getMessages(), "rucksack-title")));
        holder.setInventory(inventory);
        inventory.setContents(plugin.getRucksackManager().load(player.getUniqueId()));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof RucksackGuiHolder holder) {
            plugin.getRucksackManager().save(holder.getPlayerId(), event.getInventory().getContents());
        }
    }
}
