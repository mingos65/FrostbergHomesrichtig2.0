package de.frostberg.homes.armorstand.gui;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /ast-Menue: bearbeitet die 7 wichtigsten Bool'schen Eigenschaften eines
 * Armor Stands per Klick, ohne NBT-Befehle. 27 Slots, Toggle-Buttons
 * verteilt mit Luecken (gleicher "nicht vollgemuellt"-Stil wie Shop/Home),
 * gruen = an, grau = aus.
 */
public class ArmorStandGuiListener implements Listener {

    private enum Property {
        SMALL(1, "ast-small") {
            boolean get(ArmorStand stand) { return stand.isSmall(); }
            void set(ArmorStand stand, boolean value) { stand.setSmall(value); }
        },
        ARMS(3, "ast-arms") {
            boolean get(ArmorStand stand) { return stand.hasArms(); }
            void set(ArmorStand stand, boolean value) { stand.setArms(value); }
        },
        BASEPLATE(5, "ast-baseplate") {
            boolean get(ArmorStand stand) { return stand.hasBasePlate(); }
            void set(ArmorStand stand, boolean value) { stand.setBasePlate(value); }
        },
        GRAVITY(7, "ast-gravity") {
            boolean get(ArmorStand stand) { return stand.hasGravity(); }
            void set(ArmorStand stand, boolean value) { stand.setGravity(value); }
        },
        VISIBLE(11, "ast-visible") {
            boolean get(ArmorStand stand) { return stand.isVisible(); }
            void set(ArmorStand stand, boolean value) { stand.setVisible(value); }
        },
        MARKER(13, "ast-marker") {
            boolean get(ArmorStand stand) { return stand.isMarker(); }
            void set(ArmorStand stand, boolean value) { stand.setMarker(value); }
        },
        INVULNERABLE(15, "ast-invulnerable") {
            boolean get(ArmorStand stand) { return stand.isInvulnerable(); }
            void set(ArmorStand stand, boolean value) { stand.setInvulnerable(value); }
        };

        final int slot;
        final String nameKey;

        Property(int slot, String nameKey) {
            this.slot = slot;
            this.nameKey = nameKey;
        }

        abstract boolean get(ArmorStand stand);
        abstract void set(ArmorStand stand, boolean value);
    }

    private static final int CLOSE_SLOT = 22;

    private final FrostbergHomes plugin;

    public ArmorStandGuiListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, ArmorStand target) {
        ArmorStandGuiHolder holder = new ArmorStandGuiHolder(target.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                MessageUtil.color(MessageUtil.get(plugin.getMessages(), "ast-gui-title")));
        holder.setInventory(inventory);

        render(inventory, target);

        List<String> closeLore = List.of(MessageUtil.get(plugin.getMessages(), "ast-close-lore"));
        inventory.setItem(CLOSE_SLOT, simpleItem(Material.ARROW,
                MessageUtil.get(plugin.getMessages(), "ast-close"), closeLore));

        player.openInventory(inventory);
    }

    private void render(Inventory inventory, ArmorStand stand) {
        for (Property property : Property.values()) {
            boolean value = property.get(stand);
            Material icon = value ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            String name = MessageUtil.get(plugin.getMessages(), property.nameKey);
            List<String> lore = List.of(MessageUtil.get(plugin.getMessages(), value ? "ast-state-on" : "ast-state-off"));
            inventory.setItem(property.slot, simpleItem(icon, name, lore));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ArmorStandGuiHolder holder)) {
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

        UUID armorStandId = holder.getArmorStandId();
        Entity entity = Bukkit.getEntity(armorStandId);
        if (!(entity instanceof ArmorStand stand) || !stand.isValid()) {
            player.closeInventory();
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "ast-gone"));
            return;
        }

        for (Property property : Property.values()) {
            if (property.slot == slot) {
                property.set(stand, !property.get(stand));
                render(event.getInventory(), stand);
                return;
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
