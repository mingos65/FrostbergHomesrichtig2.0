package de.frostberg.homes.clan.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.clan.model.Clan;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GUI-Teil des Clan-Systems: /clan list zeigt Spielerkoepfe der Clan-Leader
 * (paginiert, 45 pro Seite + Navigationsreihe), /clan delete oeffnet eine
 * Ja/Nein-Sicherheitsabfrage. Aufbau analog zu
 * de.frostberg.homes.gui.HomesGuiListener - eigener Holder-Marker statt
 * Titel-Vergleich.
 */
public class ClanGuiListener implements Listener {

    private static final int PAGE_SIZE = 45; // Reihen 1-5, Reihe 6 = Navigation
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;

    private final FrostbergHomes plugin;

    public ClanGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void openList(Player player, int page) {
        List<Clan> clans = new ArrayList<>(plugin.getClanManager().getAllClans());

        ClanGuiHolder holder = new ClanGuiHolder(ClanGuiHolder.Type.LIST, null, page);
        int totalPages = Math.max(1, (clans.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        String title = MessageUtil.get(plugin.getMessages(), "clan-list-title")
                .replace("%page%", String.valueOf(page + 1))
                .replace("%pages%", String.valueOf(totalPages));
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inventory);

        int offset = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && offset + i < clans.size(); i++) {
            inventory.setItem(i, buildClanHead(clans.get(offset + i)));
        }

        if (page > 0) {
            inventory.setItem(PREV_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "homes-gui-prev-page")));
        }
        if (offset + PAGE_SIZE < clans.size()) {
            inventory.setItem(NEXT_PAGE_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "homes-gui-next-page")));
        }

        player.openInventory(inventory);
    }

    private ItemStack buildClanHead(Clan clan) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeaderUuid());
        meta.setOwningPlayer(leader);
        meta.setDisplayName(MessageUtil.color("&a&l" + clan.getName()));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.get(plugin.getMessages(), "clan-list-entry-tag").replace("%tag%", clan.getTag() != null ? clan.getTag() : "-"));
        lore.add(MessageUtil.get(plugin.getMessages(), "clan-list-entry-leader").replace("%player%", leader.getName() != null ? leader.getName() : "?"));
        lore.add(MessageUtil.get(plugin.getMessages(), "clan-list-entry-members")
                .replace("%count%", String.valueOf(clan.getMemberCount()))
                .replace("%max%", String.valueOf(plugin.getClanManager().getMaxMembers())));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public void openConfirmDelete(Player player, String clanName) {
        ClanGuiHolder holder = new ClanGuiHolder(ClanGuiHolder.Type.CONFIRM_DELETE, clanName, 0);
        String title = MessageUtil.get(plugin.getMessages(), "clan-confirm-delete-title").replace("%clan%", clanName);
        Inventory inventory = Bukkit.createInventory(holder, 9, MessageUtil.color(title));
        holder.setInventory(inventory);

        inventory.setItem(2, simpleItem(Material.LIME_CONCRETE, MessageUtil.get(plugin.getMessages(), "homes-gui-confirm-yes-name")));
        inventory.setItem(6, simpleItem(Material.RED_CONCRETE, MessageUtil.get(plugin.getMessages(), "homes-gui-confirm-no-name")));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ClanGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }

        int slot = event.getSlot();

        if (holder.getType() == ClanGuiHolder.Type.LIST) {
            if (slot == NEXT_PAGE_SLOT) {
                openList(player, holder.getPage() + 1);
            } else if (slot == PREV_PAGE_SLOT) {
                openList(player, holder.getPage() - 1);
            }
            // Klick auf einen Kopf hat aktuell keine Aktion - reine Uebersicht
            return;
        }

        if (holder.getType() == ClanGuiHolder.Type.CONFIRM_DELETE) {
            if (slot == 2) {
                confirmDelete(player, holder.getClanName());
            } else if (slot == 6) {
                player.closeInventory();
            }
        }
    }

    private void confirmDelete(Player player, String clanName) {
        Optional<Clan> clanOpt = plugin.getClanManager().getClan(clanName);
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }

        Clan clan = clanOpt.get();
        plugin.getClanManager().deleteClan(clan);

        for (UUID uuid : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-disbanded").replace("%clan%", clan.getName()));
            }
        }

        player.closeInventory();
    }

    private ItemStack simpleItem(Material material, String coloredName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(coloredName));
        item.setItemMeta(meta);
        return item;
    }
}
