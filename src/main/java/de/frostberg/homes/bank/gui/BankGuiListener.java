package de.frostberg.homes.bank.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.CurrencyBridge;
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
import java.util.Locale;

/**
 * /bank-GUI: je eine Spalte fuer Tokens und Gold, Einzahlen (gruen)/Info
 * (Muenzen-Icon)/Abheben (rot) nebeneinander, mit Luecke dazwischen -
 * gleicher Rahmen-/Nav-Stil wie Shop-GUI. Ein-/Auszahlen bucht jeweils das
 * KOMPLETTE verfuegbare Guthaben (kein Text-Eingabe-Screen noetig).
 */
public class BankGuiListener implements Listener {

    private static final int TOKENS_DEPOSIT_SLOT = 19;
    private static final int TOKENS_INFO_SLOT = 20;
    private static final int TOKENS_WITHDRAW_SLOT = 21;
    private static final int GOLD_DEPOSIT_SLOT = 23;
    private static final int GOLD_INFO_SLOT = 24;
    private static final int GOLD_WITHDRAW_SLOT = 25;
    private static final int CLOSE_SLOT = 49;

    private final FrostbergHomes plugin;
    private final DecimalFormatHolder format = new DecimalFormatHolder();

    public BankGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        BankGuiHolder holder = new BankGuiHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54,
                MessageUtil.color(MessageUtil.get(plugin.getMessages(), "bank-gui-title")));
        holder.setInventory(inventory);

        fillBorder(inventory);
        render(inventory, player);

        List<String> closeLore = List.of(MessageUtil.get(plugin.getMessages(), "bank-close-lore"));
        inventory.setItem(CLOSE_SLOT, simpleItem(Material.ARROW,
                MessageUtil.get(plugin.getMessages(), "bank-close"), closeLore));

        player.openInventory(inventory);
    }

    private void render(Inventory inventory, Player player) {
        long walletTokens = Math.max(0, CurrencyBridge.readTokenBalance(player));
        long bankTokens = plugin.getBankManager().getBankTokens(player.getUniqueId());
        double walletGold = Math.max(0, CurrencyBridge.readGoldBalance(player));
        double bankGold = plugin.getBankManager().getBankGold(player.getUniqueId());

        inventory.setItem(TOKENS_DEPOSIT_SLOT, simpleItem(Material.LIME_STAINED_GLASS_PANE,
                MessageUtil.get(plugin.getMessages(), "bank-deposit-button"),
                List.of(MessageUtil.get(plugin.getMessages(), "bank-deposit-tokens-lore").replace("%amount%", format.tokens(walletTokens)))));
        inventory.setItem(TOKENS_INFO_SLOT, simpleItem(Material.SUNFLOWER,
                MessageUtil.get(plugin.getMessages(), "bank-tokens-name"),
                List.of(
                        MessageUtil.get(plugin.getMessages(), "bank-wallet-line").replace("%amount%", format.tokens(walletTokens)),
                        MessageUtil.get(plugin.getMessages(), "bank-balance-line").replace("%amount%", format.tokens(bankTokens))
                )));
        inventory.setItem(TOKENS_WITHDRAW_SLOT, simpleItem(Material.RED_STAINED_GLASS_PANE,
                MessageUtil.get(plugin.getMessages(), "bank-withdraw-button"),
                List.of(MessageUtil.get(plugin.getMessages(), "bank-withdraw-tokens-lore").replace("%amount%", format.tokens(bankTokens)))));

        inventory.setItem(GOLD_DEPOSIT_SLOT, simpleItem(Material.LIME_STAINED_GLASS_PANE,
                MessageUtil.get(plugin.getMessages(), "bank-deposit-button"),
                List.of(MessageUtil.get(plugin.getMessages(), "bank-deposit-gold-lore").replace("%amount%", format.gold(walletGold)))));
        inventory.setItem(GOLD_INFO_SLOT, simpleItem(Material.GOLD_INGOT,
                MessageUtil.get(plugin.getMessages(), "bank-gold-name"),
                List.of(
                        MessageUtil.get(plugin.getMessages(), "bank-wallet-line").replace("%amount%", format.gold(walletGold)),
                        MessageUtil.get(plugin.getMessages(), "bank-balance-line").replace("%amount%", format.gold(bankGold))
                )));
        inventory.setItem(GOLD_WITHDRAW_SLOT, simpleItem(Material.RED_STAINED_GLASS_PANE,
                MessageUtil.get(plugin.getMessages(), "bank-withdraw-button"),
                List.of(MessageUtil.get(plugin.getMessages(), "bank-withdraw-gold-lore").replace("%amount%", format.gold(bankGold)))));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BankGuiHolder)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case CLOSE_SLOT -> player.closeInventory();
            case TOKENS_DEPOSIT_SLOT -> {
                long amount = plugin.getBankManager().depositAllTokens(player);
                if (amount > 0) {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-deposit-success")
                            .replace("%amount%", format.tokens(amount)).replace("%currency%", "Tokens"));
                } else {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-nothing-to-deposit"));
                }
                render(event.getInventory(), player);
            }
            case TOKENS_WITHDRAW_SLOT -> {
                long amount = plugin.getBankManager().withdrawAllTokens(player);
                if (amount > 0) {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-withdraw-success")
                            .replace("%amount%", format.tokens(amount)).replace("%currency%", "Tokens"));
                } else {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-nothing-to-withdraw"));
                }
                render(event.getInventory(), player);
            }
            case GOLD_DEPOSIT_SLOT -> {
                double amount = plugin.getBankManager().depositAllGold(player);
                if (amount > 0) {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-deposit-success")
                            .replace("%amount%", format.gold(amount)).replace("%currency%", "Gold"));
                } else {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-nothing-to-deposit"));
                }
                render(event.getInventory(), player);
            }
            case GOLD_WITHDRAW_SLOT -> {
                double amount = plugin.getBankManager().withdrawAllGold(player);
                if (amount > 0) {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-withdraw-success")
                            .replace("%amount%", format.gold(amount)).replace("%currency%", "Gold"));
                } else {
                    player.sendMessage(MessageUtil.get(plugin.getMessages(), "bank-nothing-to-withdraw"));
                }
                render(event.getInventory(), player);
            }
            default -> {
            }
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

    /** Kleine, lokale Zahlformatierung mit deutschem Tausenderpunkt, ohne eine weitere Klassendatei dafuer zu brauchen. */
    private static final class DecimalFormatHolder {
        private final java.text.DecimalFormat tokenFormat = new java.text.DecimalFormat("#,##0",
                java.text.DecimalFormatSymbols.getInstance(Locale.GERMANY));
        private final java.text.DecimalFormat goldFormat = new java.text.DecimalFormat("#,##0.00",
                java.text.DecimalFormatSymbols.getInstance(Locale.GERMANY));

        String tokens(long amount) {
            return tokenFormat.format(amount);
        }

        String gold(double amount) {
            return goldFormat.format(amount);
        }
    }
}
