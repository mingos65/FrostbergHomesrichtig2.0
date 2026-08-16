package de.frostberg.homes.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.Home;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Klick-basiertes GUI fuer /home und /homes: 4x9-Uebersicht mit 14 zentrierten
 * Betten pro Seite (2 Seiten = 28 Homes maximal), gruen = gesetzt, blau =
 * frei, grau = durch Rang gesperrt. Pro gesetztem Home ein Detail-Menu
 * (Teleportieren/Koordinaten/Umbenennen/Loeschen) sowie eine Amboss-
 * Umbenennung und eine Loesch-Bestaetigung. Alle Inventare tragen einen
 * HomeGuiHolder, damit dieser Listener sie zuverlaessig erkennt statt sich
 * auf den Fenstertitel zu verlassen.
 */
public class HomesGuiListener implements Listener {

    // Slot-Indizes der 14 Betten pro Seite in der 36er-Uebersicht (Reihe 2 + 3,
    // je 7 Betten mittig, 1 Slot Rand links/rechts) - dieselben Slots auf
    // beiden Seiten, nur die dahinterliegende Home-Nummer verschiebt sich.
    private static final int[] HOME_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int HOMES_PER_PAGE = HOME_SLOTS.length;
    private static final int TOTAL_PAGES = 2;
    private static final int MAX_SLOTS = HOMES_PER_PAGE * TOTAL_PAGES; // 28
    private static final int NEXT_PAGE_SLOT = 35;
    private static final int PREV_PAGE_SLOT = 27;

    private final FrostbergHomes plugin;

    public HomesGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Oeffnen: Hauptmenue
    // ---------------------------------------------------------------

    public void openMenu(Player player) {
        openMenu(player, 0);
    }

    private void openMenu(Player player, int page) {
        HomeGuiHolder holder = new HomeGuiHolder(HomeGuiHolder.Type.MENU, 0, page);
        String title = MessageUtil.get(plugin.getMessages(), "homes-gui-title")
                .replace("%page%", String.valueOf(page + 1))
                .replace("%pages%", String.valueOf(TOTAL_PAGES));
        Inventory inventory = Bukkit.createInventory(holder, 36, MessageUtil.color(title));
        holder.setInventory(inventory);

        int limit = Math.min(plugin.getHomeManager().getHomeLimit(player), MAX_SLOTS);
        int offset = page * HOMES_PER_PAGE;

        for (int i = 1; i <= HOMES_PER_PAGE; i++) {
            int nr = offset + i;
            inventory.setItem(HOME_SLOTS[i - 1], buildHomeItem(player, nr, nr <= limit));
        }

        if (page < TOTAL_PAGES - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "homes-gui-next-page"), null));
        }
        if (page > 0) {
            inventory.setItem(PREV_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "homes-gui-prev-page"), null));
        }

        player.openInventory(inventory);
    }

    private ItemStack buildHomeItem(Player player, int nr, boolean unlocked) {
        if (!unlocked) {
            return simpleItem(Material.GRAY_STAINED_GLASS_PANE, MessageUtil.get(plugin.getMessages(), "homes-gui-slot-locked"), null);
        }

        Optional<Home> homeOpt = plugin.getHomeManager().getHome(player.getUniqueId(), nr);

        if (homeOpt.isEmpty()) {
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.get(plugin.getMessages(), "homes-gui-slot-lore-free").replace("%nr%", String.valueOf(nr)));
            return simpleItem(Material.LIGHT_BLUE_BED,
                    MessageUtil.get(plugin.getMessages(), "homes-gui-slot-name-free").replace("%nr%", String.valueOf(nr)),
                    lore);
        }

        Home home = homeOpt.get();
        String displayName = home.getName() != null
                ? MessageUtil.color("&a" + home.getName())
                : MessageUtil.color("&a" + MessageUtil.get(plugin.getMessages(), "homes-gui-slot-name-set").replace("%nr%", String.valueOf(nr)));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.get(plugin.getMessages(), "homes-gui-slot-lore-coords")
                .replace("%world%", home.getWorldName())
                .replace("%x%", String.valueOf(Math.round(home.getX())))
                .replace("%y%", String.valueOf(Math.round(home.getY())))
                .replace("%z%", String.valueOf(Math.round(home.getZ()))));
        lore.add(MessageUtil.get(plugin.getMessages(), "homes-gui-slot-lore-hint"));

        ItemStack item = new ItemStack(Material.LIME_BED);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------
    // Oeffnen: Detail-Menu fuer ein gesetztes Home
    // ---------------------------------------------------------------

    private void openDetail(Player player, int nr) {
        HomeGuiHolder holder = new HomeGuiHolder(HomeGuiHolder.Type.DETAIL, nr);
        String title = MessageUtil.get(plugin.getMessages(), "homes-gui-detail-title").replace("%nr%", String.valueOf(nr));
        Inventory inventory = Bukkit.createInventory(holder, 9, MessageUtil.color(title));
        holder.setInventory(inventory);

        inventory.setItem(1, simpleItem(Material.ENDER_PEARL,
                MessageUtil.get(plugin.getMessages(), "homes-gui-detail-teleport-name"),
                List.of(MessageUtil.get(plugin.getMessages(), "homes-gui-detail-teleport-lore"))));
        inventory.setItem(3, simpleItem(Material.PAPER,
                MessageUtil.get(plugin.getMessages(), "homes-gui-detail-coords-name"),
                List.of(MessageUtil.get(plugin.getMessages(), "homes-gui-detail-coords-lore"))));
        inventory.setItem(5, simpleItem(Material.NAME_TAG,
                MessageUtil.get(plugin.getMessages(), "homes-gui-detail-rename-name"),
                List.of(MessageUtil.get(plugin.getMessages(), "homes-gui-detail-rename-lore"))));
        inventory.setItem(7, simpleItem(Material.BARRIER,
                MessageUtil.get(plugin.getMessages(), "homes-gui-detail-delete-name"),
                List.of(MessageUtil.get(plugin.getMessages(), "homes-gui-detail-delete-lore"))));
        inventory.setItem(8, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "homes-gui-detail-back-name"), null));

        player.openInventory(inventory);
    }

    // ---------------------------------------------------------------
    // Oeffnen: Loesch-Bestaetigung
    // ---------------------------------------------------------------

    private void openConfirmDelete(Player player, int nr) {
        HomeGuiHolder holder = new HomeGuiHolder(HomeGuiHolder.Type.CONFIRM_DELETE, nr);
        String title = MessageUtil.get(plugin.getMessages(), "homes-gui-confirm-title").replace("%nr%", String.valueOf(nr));
        Inventory inventory = Bukkit.createInventory(holder, 9, MessageUtil.color(title));
        holder.setInventory(inventory);

        inventory.setItem(2, simpleItem(Material.LIME_CONCRETE, MessageUtil.get(plugin.getMessages(), "homes-gui-confirm-yes-name"), null));
        inventory.setItem(6, simpleItem(Material.RED_CONCRETE, MessageUtil.get(plugin.getMessages(), "homes-gui-confirm-no-name"), null));

        player.openInventory(inventory);
    }

    // ---------------------------------------------------------------
    // Oeffnen: Amboss-Umbenennung
    // ---------------------------------------------------------------

    private void openRename(Player player, int nr) {
        Optional<Home> homeOpt = plugin.getHomeManager().getHome(player.getUniqueId(), nr);
        if (homeOpt.isEmpty()) {
            return;
        }

        HomeGuiHolder holder = new HomeGuiHolder(HomeGuiHolder.Type.RENAME, nr);
        String title = MessageUtil.get(plugin.getMessages(), "homes-gui-rename-title").replace("%nr%", String.valueOf(nr));
        Inventory inventory = Bukkit.createInventory(holder, InventoryType.ANVIL, MessageUtil.color(title));
        holder.setInventory(inventory);

        String currentName = homeOpt.get().getName() != null
                ? homeOpt.get().getName()
                : MessageUtil.get(plugin.getMessages(), "homes-gui-slot-name-set").replace("%nr%", String.valueOf(nr));

        ItemStack input = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = input.getItemMeta();
        meta.setDisplayName(MessageUtil.color(currentName));
        input.setItemMeta(meta);
        inventory.setItem(0, input);

        player.openInventory(inventory);
    }

    // ---------------------------------------------------------------
    // Klicks
    // ---------------------------------------------------------------

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeGuiHolder holder) || holder.getType() != HomeGuiHolder.Type.RENAME) {
            return;
        }

        // Kein echter Amboss-Block dahinter - Umbenennen soll hier keine
        // XP-Level kosten, nur zum Text-Eintippen genutzt werden.
        ((AnvilInventory) event.getInventory()).setRepairCost(0);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof HomeGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!topInventory.equals(event.getClickedInventory())) {
            return;
        }

        int slot = event.getSlot();

        switch (holder.getType()) {
            case MENU -> handleMenuClick(player, event, slot, holder.getPage());
            case DETAIL -> handleDetailClick(player, holder.getHomeNumber(), slot);
            case CONFIRM_DELETE -> handleConfirmClick(player, holder.getHomeNumber(), slot);
            case RENAME -> handleRenameClick(player, holder.getHomeNumber(), slot, topInventory);
        }
    }

    private void handleMenuClick(Player player, InventoryClickEvent event, int slot, int page) {
        if (slot == NEXT_PAGE_SLOT && page < TOTAL_PAGES - 1) {
            openMenu(player, page + 1);
            return;
        }
        if (slot == PREV_PAGE_SLOT && page > 0) {
            openMenu(player, page - 1);
            return;
        }

        int slotIndex = -1;
        for (int i = 0; i < HOME_SLOTS.length; i++) {
            if (HOME_SLOTS[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex == -1) {
            return; // Rand-/Fuellslot, keine Aktion
        }

        int nr = page * HOMES_PER_PAGE + slotIndex + 1;

        int limit = Math.min(plugin.getHomeManager().getHomeLimit(player), MAX_SLOTS);
        if (nr > limit) {
            return; // gesperrter Slot
        }

        boolean set = plugin.getHomeManager().hasHome(player.getUniqueId(), nr);
        if (!set) {
            return; // freie/blaue Betten sind nicht anklickbar
        }

        if (event.isRightClick()) {
            openDetail(player, nr);
        } else if (event.isLeftClick()) {
            player.closeInventory();
            plugin.getHomeCommand().teleportToHome(player, nr);
        }
    }

    private void handleDetailClick(Player player, int nr, int slot) {
        switch (slot) {
            case 1 -> {
                player.closeInventory();
                plugin.getHomeCommand().teleportToHome(player, nr);
            }
            case 3 -> {
                Optional<Home> homeOpt = plugin.getHomeManager().getHome(player.getUniqueId(), nr);
                homeOpt.ifPresent(home -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "homes-gui-coords-chat")
                        .replace("%nr%", String.valueOf(nr))
                        .replace("%world%", home.getWorldName())
                        .replace("%x%", String.valueOf(Math.round(home.getX())))
                        .replace("%y%", String.valueOf(Math.round(home.getY())))
                        .replace("%z%", String.valueOf(Math.round(home.getZ())))));
            }
            case 5 -> openRename(player, nr);
            case 7 -> openConfirmDelete(player, nr);
            case 8 -> openMenu(player, pageForHomeNumber(nr));
            default -> {
                // kein interaktiver Slot
            }
        }
    }

    private void handleConfirmClick(Player player, int nr, int slot) {
        if (slot == 2) {
            plugin.getHomeManager().deleteHome(player.getUniqueId(), nr);
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "homes-gui-deleted").replace("%nr%", String.valueOf(nr)));
            openMenu(player, pageForHomeNumber(nr));
        } else if (slot == 6) {
            openMenu(player, pageForHomeNumber(nr));
        }
    }

    private int pageForHomeNumber(int nr) {
        return (nr - 1) / HOMES_PER_PAGE;
    }

    private void handleRenameClick(Player player, int nr, int slot, Inventory anvil) {
        if (slot != 2) {
            return; // nur der Ergebnis-Slot loest eine Aktion aus
        }

        String renameText = ((AnvilInventory) anvil).getRenameText();
        if (renameText == null || renameText.isBlank()) {
            return;
        }

        String newName = renameText.length() > 32 ? renameText.substring(0, 32) : renameText;
        plugin.getHomeManager().renameHome(player.getUniqueId(), nr, newName);

        player.closeInventory();
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "homes-gui-renamed")
                .replace("%nr%", String.valueOf(nr))
                .replace("%name%", MessageUtil.color(newName)));
        openMenu(player, pageForHomeNumber(nr));
    }

    // ---------------------------------------------------------------
    // Hilfsmittel
    // ---------------------------------------------------------------

    private ItemStack simpleItem(Material material, String coloredName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(coloredName));
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
