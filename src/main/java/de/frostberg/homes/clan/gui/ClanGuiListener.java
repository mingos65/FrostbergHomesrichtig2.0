package de.frostberg.homes.clan.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.clan.model.Clan;
import de.frostberg.homes.util.ColorUtil;
import de.frostberg.homes.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
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
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * GUI-Teil des Clan-Systems: /clan list zeigt Spielerkoepfe der Clan-Leader
 * (paginiert, 45 pro Seite + Navigationsreihe), Klick auf einen Kopf zeigt
 * alle Mitglieder dieses Clans als eigene Kopf-Uebersicht. /clan delete und
 * /clan leave oeffnen je eine Ja/Nein-Sicherheitsabfrage. Aufbau analog zu
 * de.frostberg.homes.gui.HomesGuiListener - eigener Holder-Marker statt
 * Titel-Vergleich.
 */
public class ClanGuiListener implements Listener {

    private static final int PAGE_SIZE = 45; // Reihen 1-5, Reihe 6 = Navigation
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int MEMBERS_BACK_SLOT = 49;

    private final FrostbergHomes plugin;

    public ClanGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Clan-Liste (Leader-Koepfe)
    // ---------------------------------------------------------------

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
        lore.add(MessageUtil.get(plugin.getMessages(), "clan-list-entry-hint"));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------
    // Mitglieder-Uebersicht (Koepfe + Namen + Rolle)
    // ---------------------------------------------------------------

    public void openMembers(Player player, String clanName, int returnPage) {
        Optional<Clan> clanOpt = plugin.getClanManager().getClan(clanName);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        ClanGuiHolder holder = new ClanGuiHolder(ClanGuiHolder.Type.MEMBERS, clanName, returnPage);
        String title = MessageUtil.get(plugin.getMessages(), "clan-members-title").replace("%clan%", clan.getName());
        Inventory inventory = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inventory);

        int slot = 0;
        for (Map.Entry<UUID, Clan.Role> entry : clan.getMembers().entrySet()) {
            if (slot >= PAGE_SIZE) {
                break; // Sicherheitsnetz falls max-members sehr hoch konfiguriert wurde
            }
            inventory.setItem(slot, buildMemberHead(entry.getKey(), entry.getValue()));
            slot++;
        }

        inventory.setItem(MEMBERS_BACK_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "homes-gui-detail-back-name")));

        player.openInventory(inventory);
    }

    private ItemStack buildMemberHead(UUID uuid, Clan.Role role) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        OfflinePlayer member = Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(member);
        String color = switch (role) {
            case LEADER -> "&c&l";
            case MOD -> "&e&l";
            case MEMBER -> "&f";
        };
        meta.setDisplayName(MessageUtil.color(color + (member.getName() != null ? member.getName() : "?")));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.get(plugin.getMessages(), "clan-members-entry-role").replace("%role%", role.name()));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------
    // Loesch-Bestaetigung
    // ---------------------------------------------------------------

    public void openConfirmDelete(Player player, String clanName) {
        ClanGuiHolder holder = new ClanGuiHolder(ClanGuiHolder.Type.CONFIRM_DELETE, clanName, 0);
        String title = MessageUtil.get(plugin.getMessages(), "clan-confirm-delete-title").replace("%clan%", clanName);
        Inventory inventory = Bukkit.createInventory(holder, 9, MessageUtil.color(title));
        holder.setInventory(inventory);

        inventory.setItem(2, simpleItem(Material.LIME_CONCRETE, MessageUtil.get(plugin.getMessages(), "homes-gui-confirm-yes-name")));
        inventory.setItem(6, simpleItem(Material.RED_CONCRETE, MessageUtil.get(plugin.getMessages(), "homes-gui-confirm-no-name")));

        player.openInventory(inventory);
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

    // ---------------------------------------------------------------
    // Verlassen-Bestaetigung
    // ---------------------------------------------------------------

    public void openConfirmLeave(Player player, String clanName) {
        ClanGuiHolder holder = new ClanGuiHolder(ClanGuiHolder.Type.CONFIRM_LEAVE, clanName, 0);
        String title = MessageUtil.get(plugin.getMessages(), "clan-confirm-leave-title").replace("%clan%", clanName);
        Inventory inventory = Bukkit.createInventory(holder, 9, MessageUtil.color(title));
        holder.setInventory(inventory);

        inventory.setItem(2, simpleItem(Material.LIME_CONCRETE, MessageUtil.get(plugin.getMessages(), "clan-leave-confirm-yes-name")));
        inventory.setItem(6, simpleItem(Material.RED_CONCRETE, MessageUtil.get(plugin.getMessages(), "clan-leave-confirm-no-name")));

        player.openInventory(inventory);
    }

    private void confirmLeave(Player player, String clanName) {
        Optional<Clan> clanOpt = plugin.getClanManager().getClan(clanName);
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }

        Clan clan = clanOpt.get();
        plugin.getClanManager().removeMember(clan, player.getUniqueId());

        if (clan.getMemberCount() == 0) {
            plugin.getClanManager().deleteClan(clan);
        } else {
            for (UUID uuid : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    member.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-member-left").replace("%player%", player.getName()));
                }
            }
        }

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-left").replace("%clan%", clan.getName()));
        player.closeInventory();
    }

    // ---------------------------------------------------------------
    // Farben-Shop (Clan-Tag-Farbe mit Gold kaufen)
    // ---------------------------------------------------------------

    private static final int[] COLOR_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int COLOR_BACK_SLOT = 22;

    public void openColorShop(Player player, String clanName) {
        ClanGuiHolder holder = new ClanGuiHolder(ClanGuiHolder.Type.COLOR, clanName, 0);
        String title = MessageUtil.get(plugin.getMessages(), "clan-color-shop-title");
        Inventory inventory = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inventory);

        Optional<Clan> clanOpt = plugin.getClanManager().getClan(clanName);
        if (clanOpt.isEmpty()) {
            player.openInventory(inventory);
            return;
        }
        Clan clan = clanOpt.get();

        List<Map<?, ?>> colors = plugin.getConfig().getMapList("clan.colors");
        for (int i = 0; i < colors.size() && i < COLOR_SLOTS.length; i++) {
            inventory.setItem(COLOR_SLOTS[i], buildColorItem(colors.get(i), clan));
        }

        inventory.setItem(COLOR_BACK_SLOT, simpleItem(Material.ARROW, MessageUtil.get(plugin.getMessages(), "homes-gui-detail-back-name")));

        player.openInventory(inventory);
    }

    private ItemStack buildColorItem(Map<?, ?> entry, Clan clan) {
        String name = String.valueOf(entry.get("name"));
        String code = String.valueOf(entry.get("code"));
        long price = Long.parseLong(String.valueOf(entry.get("price")));

        boolean active = code.equals(clan.getTagColor()) || (clan.getTagColor() == null && code.equals("&b"));

        ItemStack item = new ItemStack(active ? Material.LIME_DYE : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.applyColorCode(code, name));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.get(plugin.getMessages(), "clan-color-shop-price").replace("%price%", String.valueOf(price)));
        lore.add(MessageUtil.get(plugin.getMessages(), active ? "clan-color-shop-active" : "clan-color-shop-select"));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void handleColorClick(Player player, String clanName, int slot) {
        if (slot == COLOR_BACK_SLOT) {
            player.closeInventory();
            return;
        }

        int index = -1;
        for (int i = 0; i < COLOR_SLOTS.length; i++) {
            if (COLOR_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return;
        }

        Optional<Clan> clanOpt = plugin.getClanManager().getClan(clanName);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        List<Map<?, ?>> colors = plugin.getConfig().getMapList("clan.colors");
        if (index >= colors.size()) {
            return;
        }
        Map<?, ?> entry = colors.get(index);
        String code = String.valueOf(entry.get("code"));
        long price = Long.parseLong(String.valueOf(entry.get("price")));

        if (code.equals(clan.getTagColor()) || (clan.getTagColor() == null && code.equals("&b"))) {
            openColorShop(player, clanName);
            return; // bereits aktiv, kein erneuter Kauf noetig
        }

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-not-installed"));
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-not-installed"));
            return;
        }
        Economy economy = rsp.getProvider();

        if (!economy.has(player, price)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-color-shop-insufficient"));
            return;
        }
        EconomyResponse response = economy.withdrawPlayer(player, price);
        if (!response.transactionSuccess()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "unknown-error"));
            return;
        }

        clan.setTagColor(code);
        plugin.getClanManager().saveClan(clan);

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-color-shop-bought")
                .replace("%color%", ColorUtil.applyColorCode(code, String.valueOf(entry.get("name")))));

        openColorShop(player, clanName);
    }

    // ---------------------------------------------------------------
    // Klicks
    // ---------------------------------------------------------------

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

        switch (holder.getType()) {
            case LIST -> handleListClick(player, holder, slot);
            case MEMBERS -> handleMembersClick(player, holder, slot);
            case CONFIRM_DELETE -> {
                if (slot == 2) {
                    confirmDelete(player, holder.getClanName());
                } else if (slot == 6) {
                    player.closeInventory();
                }
            }
            case CONFIRM_LEAVE -> {
                if (slot == 2) {
                    confirmLeave(player, holder.getClanName());
                } else if (slot == 6) {
                    player.closeInventory();
                }
            }
            case COLOR -> handleColorClick(player, holder.getClanName(), slot);
        }
    }

    private void handleListClick(Player player, ClanGuiHolder holder, int slot) {
        if (slot == NEXT_PAGE_SLOT) {
            openList(player, holder.getPage() + 1);
            return;
        }
        if (slot == PREV_PAGE_SLOT) {
            openList(player, holder.getPage() - 1);
            return;
        }

        if (slot >= PAGE_SIZE) {
            return; // Rand-/Fuellslot
        }

        List<Clan> clans = new ArrayList<>(plugin.getClanManager().getAllClans());
        int index = holder.getPage() * PAGE_SIZE + slot;
        if (index >= clans.size()) {
            return;
        }

        openMembers(player, clans.get(index).getName(), holder.getPage());
    }

    private void handleMembersClick(Player player, ClanGuiHolder holder, int slot) {
        if (slot == MEMBERS_BACK_SLOT) {
            openList(player, holder.getPage());
        }
        // Klick auf einen Mitglied-Kopf hat aktuell keine Aktion - reine Uebersicht
    }

    private ItemStack simpleItem(Material material, String coloredName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(coloredName));
        item.setItemMeta(meta);
        return item;
    }
}
