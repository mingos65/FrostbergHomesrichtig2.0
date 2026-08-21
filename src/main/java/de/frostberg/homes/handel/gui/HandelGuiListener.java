package de.frostberg.homes.handel.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.handel.TradeSession;
import de.frostberg.homes.util.CurrencyBridge;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Gemeinsames Handel-Fenster fuer zwei Spieler (EIN Inventory-Objekt, beide
 * oeffnen es - wie eine echte Truhe mit zwei Betrachtern). Jede Seite darf
 * nur ihre eigene Item-Zone bearbeiten. Aendert sich Items ODER Betraege
 * einer Seite, werden BEIDE Bestaetigungen zurueckgesetzt - das ist der
 * Betrugsschutz gegen Last-Second-Swaps.
 */
public class HandelGuiListener implements Listener {

    private static final int[] A_ITEM_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] B_ITEM_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34};
    private static final int[] DIVIDER_SLOTS = {4, 13, 22, 31, 40, 49};
    private static final int HEAD_A_SLOT = 1;
    private static final int HEAD_B_SLOT = 7;
    private static final int A_TOKENS_SLOT = 37;
    private static final int A_GOLD_SLOT = 38;
    private static final int B_GOLD_SLOT = 42;
    private static final int B_TOKENS_SLOT = 43;
    private static final int A_CANCEL_SLOT = 46;
    private static final int A_CONFIRM_SLOT = 47;
    private static final int B_CONFIRM_SLOT = 51;
    private static final int B_CANCEL_SLOT = 52;

    private final FrostbergHomes plugin;
    private final DecimalFormat tokenFormat = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DecimalFormat goldFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));

    public HandelGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void startTrade(Player a, Player b) {
        TradeSession session = plugin.getTradeManager().startSession(a.getUniqueId(), b.getUniqueId());
        HandelGuiHolder holder = new HandelGuiHolder(session);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                MessageUtil.color(MessageUtil.get(plugin.getMessages(), "handel-gui-title")
                        .replace("%playerA%", a.getName())
                        .replace("%playerB%", b.getName())));
        holder.setInventory(inventory);
        session.setInventory(inventory);

        fillBorder(inventory);
        fillDivider(inventory);
        renderHeads(session);
        render(session);

        a.openInventory(inventory);
        b.openInventory(inventory);
    }

    private void render(TradeSession session) {
        Inventory inventory = session.getInventory();

        inventory.setItem(A_TOKENS_SLOT, currencyItem(Material.SUNFLOWER, "handel-tokens-name",
                tokenFormat.format(session.getTokensOffered(session.getPlayerA()))));
        inventory.setItem(A_GOLD_SLOT, currencyItem(Material.GOLD_INGOT, "handel-gold-name",
                goldFormat.format(session.getGoldOffered(session.getPlayerA()))));
        inventory.setItem(B_GOLD_SLOT, currencyItem(Material.GOLD_INGOT, "handel-gold-name",
                goldFormat.format(session.getGoldOffered(session.getPlayerB()))));
        inventory.setItem(B_TOKENS_SLOT, currencyItem(Material.SUNFLOWER, "handel-tokens-name",
                tokenFormat.format(session.getTokensOffered(session.getPlayerB()))));

        inventory.setItem(A_CONFIRM_SLOT, confirmItem(session.isConfirmed(session.getPlayerA())));
        inventory.setItem(B_CONFIRM_SLOT, confirmItem(session.isConfirmed(session.getPlayerB())));

        List<String> cancelLore = List.of(MessageUtil.get(plugin.getMessages(), "handel-cancel-lore"));
        ItemStack cancelItem = simpleItem(Material.BARRIER, MessageUtil.get(plugin.getMessages(), "handel-cancel"), cancelLore);
        inventory.setItem(A_CANCEL_SLOT, cancelItem.clone());
        inventory.setItem(B_CANCEL_SLOT, cancelItem.clone());
    }

    /** Kopf-Icons mit Skin/Name je Spieler oben links/rechts, damit auf einen Blick klar ist, welche Seite wem gehoert. */
    private void renderHeads(TradeSession session) {
        Inventory inventory = session.getInventory();
        inventory.setItem(HEAD_A_SLOT, headItem(session.getPlayerA()));
        inventory.setItem(HEAD_B_SLOT, headItem(session.getPlayerB()));
    }

    private ItemStack headItem(UUID uuid) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
        String name = owner.getName() != null ? owner.getName() : "?";
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(MessageUtil.color(MessageUtil.get(plugin.getMessages(), "handel-side-name").replace("%player%", name)));
            meta.setLore(List.of(MessageUtil.color(MessageUtil.get(plugin.getMessages(), "handel-side-lore"))));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack currencyItem(Material material, String nameKey, String amount) {
        String raw = MessageUtil.get(plugin.getMessages(), "handel-currency-lore").replace("%amount%", amount);
        List<String> lore = new ArrayList<>();
        for (String line : raw.split("\\n")) {
            lore.add(line);
        }
        return simpleItem(material, MessageUtil.get(plugin.getMessages(), nameKey), lore);
    }

    private ItemStack confirmItem(boolean confirmed) {
        Material material = confirmed ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String key = confirmed ? "handel-confirmed" : "handel-not-confirmed";
        String loreKey = confirmed ? "handel-confirm-lore-on" : "handel-confirm-lore-off";
        List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), loreKey));
        return simpleItem(material, MessageUtil.get(plugin.getMessages(), key), lore);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HandelGuiHolder holder)) {
            return;
        }
        TradeSession session = holder.getSession();
        Player clicker = (Player) event.getWhoClicked();
        UUID uuid = clicker.getUniqueId();

        int topSize = event.getView().getTopInventory().getSize();
        boolean inTopInventory = event.getRawSlot() < topSize;

        if (!inTopInventory) {
            // Eigenes Inventar des Spielers - normal erlaubt, nur Shift-Klick
            // verbieten (koennte sonst unkontrolliert in die falsche Zone
            // des anderen Spielers rutschen)
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        if (!session.isPlayerA(uuid) && !session.isPlayerB(uuid)) {
            return;
        }

        int slot = event.getSlot();

        if ((slot == A_CANCEL_SLOT && session.isPlayerA(uuid)) || (slot == B_CANCEL_SLOT && session.isPlayerB(uuid))) {
            cancelTrade(session, "handel-cancelled-by-player");
            return;
        }
        if (slot == A_CONFIRM_SLOT && session.isPlayerA(uuid)) {
            session.setConfirmed(uuid, !session.isConfirmed(uuid));
            render(session);
            tryComplete(session);
            return;
        }
        if (slot == B_CONFIRM_SLOT && session.isPlayerB(uuid)) {
            session.setConfirmed(uuid, !session.isConfirmed(uuid));
            render(session);
            tryComplete(session);
            return;
        }

        boolean isTokensSlot = (slot == A_TOKENS_SLOT && session.isPlayerA(uuid)) || (slot == B_TOKENS_SLOT && session.isPlayerB(uuid));
        boolean isGoldSlot = (slot == A_GOLD_SLOT && session.isPlayerA(uuid)) || (slot == B_GOLD_SLOT && session.isPlayerB(uuid));

        if (isTokensSlot) {
            requestAmount(clicker, session, true);
            return;
        }
        if (isGoldSlot) {
            requestAmount(clicker, session, false);
            return;
        }

        boolean inOwnItemZone = (session.isPlayerA(uuid) && contains(A_ITEM_SLOTS, slot))
                || (session.isPlayerB(uuid) && contains(B_ITEM_SLOTS, slot));
        if (inOwnItemZone) {
            event.setCancelled(false);
            session.resetConfirmations();
            Bukkit.getScheduler().runTask(plugin, () -> {
                inventoryStillOpen(session, () -> render(session));
            });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof HandelGuiHolder holder)) {
            return;
        }
        TradeSession session = holder.getSession();
        Player dragger = (Player) event.getWhoClicked();
        UUID uuid = dragger.getUniqueId();
        int topSize = event.getView().getTopInventory().getSize();

        int[] allowedSlots = session.isPlayerA(uuid) ? A_ITEM_SLOTS : B_ITEM_SLOTS;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                continue;
            }
            if (!contains(allowedSlots, rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        session.resetConfirmations();
        Bukkit.getScheduler().runTask(plugin, () -> inventoryStillOpen(session, () -> render(session)));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof HandelGuiHolder holder)) {
            return;
        }
        TradeSession session = holder.getSession();
        if (session.isFinished()) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        if (session.isAwaitingInput(uuid)) {
            // Wir haben das Fenster selbst geschlossen, um einen Betrag per Chat
            // abzufragen - kein Verbindungsabbruch, also nicht abbrechen.
            return;
        }
        cancelTrade(session, "handel-cancelled-disconnect");
    }

    /** Bricht die laufende Session des Spielers ab, falls vorhanden - aufgerufen beim Quit, auch waehrend einer Chat-Betragseingabe. */
    public void handleQuit(UUID uuid) {
        TradeSession session = plugin.getTradeManager().getSession(uuid);
        if (session != null && !session.isFinished()) {
            cancelTrade(session, "handel-cancelled-disconnect");
        }
    }

    private void requestAmount(Player player, TradeSession session, boolean tokens) {
        UUID uuid = player.getUniqueId();
        session.setAwaitingInput(uuid, true);
        player.closeInventory();
        player.sendMessage(MessageUtil.get(plugin.getMessages(), tokens ? "handel-prompt-tokens" : "handel-prompt-gold"));
        plugin.getChatInputManager().awaitInput(player, (p, input) -> handleAmountInput(p, session, input, tokens));
    }

    private void handleAmountInput(Player player, TradeSession session, String input, boolean tokens) {
        UUID uuid = player.getUniqueId();
        session.setAwaitingInput(uuid, false);

        if (session.isFinished()) {
            return;
        }

        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("abbrechen")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-input-cancelled"));
            player.openInventory(session.getInventory());
            return;
        }

        if (tokens) {
            long amount;
            try {
                amount = Long.parseLong(trimmed);
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-invalid-amount"));
                player.openInventory(session.getInventory());
                return;
            }
            if (amount < 0) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-invalid-amount"));
                player.openInventory(session.getInventory());
                return;
            }
            if (amount > CurrencyBridge.readTokenBalance(player)) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-insufficient-wallet"));
                player.openInventory(session.getInventory());
                return;
            }
            session.setTokensOffered(uuid, amount);
        } else {
            double amount;
            try {
                amount = Double.parseDouble(trimmed.replace(",", "."));
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-invalid-amount"));
                player.openInventory(session.getInventory());
                return;
            }
            if (amount < 0) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-invalid-amount"));
                player.openInventory(session.getInventory());
                return;
            }
            if (amount > CurrencyBridge.readGoldBalance(player)) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-insufficient-wallet"));
                player.openInventory(session.getInventory());
                return;
            }
            session.setGoldOffered(uuid, amount);
        }

        session.resetConfirmations();
        render(session);
        player.openInventory(session.getInventory());
    }

    private void inventoryStillOpen(TradeSession session, Runnable action) {
        if (!session.isFinished()) {
            action.run();
        }
    }

    private void tryComplete(TradeSession session) {
        if (!session.bothConfirmed()) {
            return;
        }
        executeTrade(session);
    }

    private void executeTrade(TradeSession session) {
        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());
        if (a == null || b == null) {
            cancelTrade(session, "handel-cancelled-disconnect");
            return;
        }

        long tokensA = session.getTokensOffered(session.getPlayerA());
        long tokensB = session.getTokensOffered(session.getPlayerB());
        double goldA = session.getGoldOffered(session.getPlayerA());
        double goldB = session.getGoldOffered(session.getPlayerB());

        if (tokensA > 0 && CurrencyBridge.readTokenBalance(a) < tokensA) {
            failTrade(session, a, b, "handel-failed-tokens", a.getName());
            return;
        }
        if (tokensB > 0 && CurrencyBridge.readTokenBalance(b) < tokensB) {
            failTrade(session, a, b, "handel-failed-tokens", b.getName());
            return;
        }
        if (goldA > 0 && CurrencyBridge.readGoldBalance(a) < goldA) {
            failTrade(session, a, b, "handel-failed-gold", a.getName());
            return;
        }
        if (goldB > 0 && CurrencyBridge.readGoldBalance(b) < goldB) {
            failTrade(session, a, b, "handel-failed-gold", b.getName());
            return;
        }

        List<ItemStack> itemsFromA = collectItems(session.getInventory(), A_ITEM_SLOTS);
        List<ItemStack> itemsFromB = collectItems(session.getInventory(), B_ITEM_SLOTS);

        if (tokensA > 0) {
            CurrencyBridge.takeTokens(a, tokensA);
            CurrencyBridge.giveTokens(b, tokensA);
        }
        if (tokensB > 0) {
            CurrencyBridge.takeTokens(b, tokensB);
            CurrencyBridge.giveTokens(a, tokensB);
        }
        if (goldA > 0) {
            CurrencyBridge.takeGold(a, goldA);
            CurrencyBridge.giveGold(b, goldA);
        }
        if (goldB > 0) {
            CurrencyBridge.takeGold(b, goldB);
            CurrencyBridge.giveGold(a, goldB);
        }

        giveItemsSafely(b, itemsFromA);
        giveItemsSafely(a, itemsFromB);

        session.setCompleted(true);
        a.closeInventory();
        b.closeInventory();
        a.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-success"));
        b.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-success"));
        plugin.getTradeManager().endSession(session);
    }

    private void failTrade(TradeSession session, Player a, Player b, String messageKey, String playerName) {
        String message = MessageUtil.get(plugin.getMessages(), messageKey).replace("%player%", playerName);
        a.sendMessage(message);
        b.sendMessage(message);
        session.resetConfirmations();
        render(session);
    }

    private void cancelTrade(TradeSession session, String messageKey) {
        if (session.isFinished()) {
            return;
        }
        session.setCancelled(true);
        // Falls gerade eine Seite auf eine Chat-Betragseingabe wartet, deren Ergebnis nicht
        // mehr verarbeiten - sonst wuerde ihre naechste normale Chatnachricht verschluckt.
        plugin.getChatInputManager().cancel(session.getPlayerA());
        plugin.getChatInputManager().cancel(session.getPlayerB());

        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());

        if (session.getInventory() != null) {
            returnItems(session.getInventory(), A_ITEM_SLOTS, a);
            returnItems(session.getInventory(), B_ITEM_SLOTS, b);
        }

        String message = MessageUtil.get(plugin.getMessages(), messageKey);
        if (a != null) {
            a.closeInventory();
            a.sendMessage(message);
        }
        if (b != null) {
            b.closeInventory();
            b.sendMessage(message);
        }
        plugin.getTradeManager().endSession(session);
    }

    private void returnItems(Inventory inventory, int[] slots, Player owner) {
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (owner != null) {
                giveItemsSafely(owner, List.of(item));
            }
            inventory.setItem(slot, null);
        }
    }

    private List<ItemStack> collectItems(Inventory inventory, int[] slots) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
                inventory.setItem(slot, null);
            }
        }
        return items;
    }

    /** Fuegt Items ins Inventar ein, droppt Ueberschuss sicher zu Fuessen statt ihn zu verlieren, falls das Inventar voll ist. */
    private void giveItemsSafely(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            var leftover = player.getInventory().addItem(item);
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
    }

    private boolean contains(int[] slots, int slot) {
        for (int s : slots) {
            if (s == slot) {
                return true;
            }
        }
        return false;
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

    /** Senkrechte Trennlinie in der Mittelspalte, damit sofort klar ist, welche Seite wem gehoert. */
    private void fillDivider(Inventory inventory) {
        ItemStack divider = simpleItem(Material.YELLOW_STAINED_GLASS_PANE, " ", null);
        for (int slot : DIVIDER_SLOTS) {
            inventory.setItem(slot, divider.clone());
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
