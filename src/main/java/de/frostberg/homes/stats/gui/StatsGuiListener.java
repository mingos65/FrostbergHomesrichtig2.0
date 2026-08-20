package de.frostberg.homes.stats.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.CurrencyBridge;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /stats-GUI: zeigt Spielzeit, Wallet- und Bankguthaben alles auf einen Blick, rein informativ. */
public class StatsGuiListener implements Listener {

    private static final int CLOSE_SLOT = 16;

    private final FrostbergHomes plugin;
    private final DecimalFormat tokenFormat = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DecimalFormat goldFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));

    public StatsGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, OfflinePlayer target) {
        StatsGuiHolder holder = new StatsGuiHolder();
        String title = MessageUtil.get(plugin.getMessages(), "stats-gui-title")
                .replace("%player%", target.getName() != null ? target.getName() : "?");
        Inventory inventory = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inventory);

        fillBorder(inventory);

        boolean self = viewer.getUniqueId().equals(target.getUniqueId());
        boolean hidden = !self && plugin.getBankManager().isHidden(target.getUniqueId()) && !viewer.hasPermission("bank.viewhidden");

        String time = plugin.getPlaytimeManager().format(plugin.getPlaytimeManager().getTotalSeconds(target.getUniqueId()));
        inventory.setItem(10, simpleItem(Material.CLOCK, MessageUtil.get(plugin.getMessages(), "stats-gui-playtime-name"),
                List.of(time)));

        if (target.isOnline() && target.getPlayer() != null) {
            Player online = target.getPlayer();
            long walletTokens = CurrencyBridge.readTokenBalance(online);
            double walletGold = CurrencyBridge.readGoldBalance(online);
            inventory.setItem(11, simpleItem(Material.SUNFLOWER, MessageUtil.get(plugin.getMessages(), "stats-gui-wallet-tokens-name"),
                    List.of(walletTokens < 0 ? "?" : tokenFormat.format(walletTokens))));
            inventory.setItem(12, simpleItem(Material.GOLD_INGOT, MessageUtil.get(plugin.getMessages(), "stats-gui-wallet-gold-name"),
                    List.of(walletGold < 0 ? "?" : goldFormat.format(walletGold))));
        } else {
            inventory.setItem(11, simpleItem(Material.SUNFLOWER, MessageUtil.get(plugin.getMessages(), "stats-gui-wallet-tokens-name"),
                    List.of(MessageUtil.get(plugin.getMessages(), "stats-gui-offline"))));
            inventory.setItem(12, simpleItem(Material.GOLD_INGOT, MessageUtil.get(plugin.getMessages(), "stats-gui-wallet-gold-name"),
                    List.of(MessageUtil.get(plugin.getMessages(), "stats-gui-offline"))));
        }

        if (hidden) {
            inventory.setItem(14, simpleItem(Material.BARRIER, MessageUtil.get(plugin.getMessages(), "stats-gui-bank-tokens-name"),
                    List.of(MessageUtil.get(plugin.getMessages(), "stats-gui-hidden"))));
            inventory.setItem(15, simpleItem(Material.BARRIER, MessageUtil.get(plugin.getMessages(), "stats-gui-bank-gold-name"),
                    List.of(MessageUtil.get(plugin.getMessages(), "stats-gui-hidden"))));
        } else {
            inventory.setItem(14, simpleItem(Material.CHEST, MessageUtil.get(plugin.getMessages(), "stats-gui-bank-tokens-name"),
                    List.of(tokenFormat.format(plugin.getBankManager().getBankTokens(target.getUniqueId())))));
            inventory.setItem(15, simpleItem(Material.CHEST, MessageUtil.get(plugin.getMessages(), "stats-gui-bank-gold-name"),
                    List.of(goldFormat.format(plugin.getBankManager().getBankGold(target.getUniqueId())))));
        }

        List<String> closeLore = List.of(MessageUtil.get(plugin.getMessages(), "stats-gui-close-lore"));
        inventory.setItem(CLOSE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "stats-gui-close"), closeLore));

        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StatsGuiHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlot() == CLOSE_SLOT && event.getWhoClicked() instanceof Player player) {
            player.closeInventory();
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
                    colored.add(MessageUtil.color("&f" + line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
