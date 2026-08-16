package de.frostberg.homes.quest.listener;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.quest.model.QuestType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sammelt Spieler-Aktionen ein und meldet sie an QuestManager#addProgress -
 * die eigentliche Pruefung (welche Quests aktiv sind, ob Ziel/Welt/Creative
 * passen) macht der Manager selbst. Jeder Handler prueft zuerst
 * hasActiveQuestOfType(), damit haeufige Events (Bloecke abbauen, Bewegung)
 * bei einem Server ohne passende aktive Quest keine unnoetige Arbeit machen.
 */
public class QuestProgressListener implements Listener {

    private final FrostbergHomes plugin;

    // Spieler-UUID -> angesammelte Teil-Bloecke seit dem letzten vollen Block
    // (PlayerMoveEvent liefert Bruchteile, WALK_DISTANCE zaehlt aber in ganzen Bloecken)
    private final Map<UUID, Double> distanceBuffer = new HashMap<>();

    public QuestProgressListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getQuestManager().hasActiveQuestOfType(QuestType.MINE_BLOCK)) {
            return;
        }
        plugin.getQuestManager().addProgress(event.getPlayer(), QuestType.MINE_BLOCK, event.getBlock().getType(), null, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getQuestManager().hasActiveQuestOfType(QuestType.PLACE_BLOCK)) {
            return;
        }
        plugin.getQuestManager().addProgress(event.getPlayer(), QuestType.PLACE_BLOCK, event.getBlockPlaced().getType(), null, 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getQuestManager().hasActiveQuestOfType(QuestType.KILL_ENTITY)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        plugin.getQuestManager().addProgress(killer, QuestType.KILL_ENTITY, null, event.getEntityType(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!plugin.getQuestManager().hasActiveQuestOfType(QuestType.CRAFT_ITEM)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRecipe() == null) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        if (result.getAmount() <= 0) {
            return;
        }

        int multiplier = event.isShiftClick() ? estimateShiftCraftMultiplier(event) : 1;
        long crafted = (long) result.getAmount() * Math.max(1, multiplier);

        plugin.getQuestManager().addProgress(player, QuestType.CRAFT_ITEM, result.getType(), null, crafted);
    }

    /**
     * Grobe Schaetzung, wie oft ein Shift-Klick craftet: die kleinste
     * Zutaten-Stapelgroesse im Crafting-Raster begrenzt, wie oft das Rezept
     * hintereinander ausgefuehrt werden kann. Kein exaktes Nachbauen der
     * Vanilla-Shift-Craft-Schleife (die zusaetzlich Inventarplatz beruecksichtigt),
     * aber fuer eine Quest-Fortschrittsanzeige ausreichend genau.
     */
    private int estimateShiftCraftMultiplier(CraftItemEvent event) {
        int min = Integer.MAX_VALUE;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                min = Math.min(min, ingredient.getAmount());
            }
        }
        return min == Integer.MAX_VALUE ? 1 : min;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!plugin.getQuestManager().hasActiveQuestOfType(QuestType.FISH)) {
            return;
        }

        Material caught = null;
        if (event.getCaught() instanceof Item item) {
            caught = item.getItemStack().getType();
        }
        plugin.getQuestManager().addProgress(event.getPlayer(), QuestType.FISH, caught, null, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getQuestManager().hasActiveQuestOfType(QuestType.WALK_DISTANCE)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        // Nur bei tatsaechlichem Blockwechsel reagieren - reduziert die
        // Eventflut von PlayerMoveEvent (feuert auch bei reinem Umschauen)
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        double distance = from.distance(to);
        if (distance > 10) {
            return; // vermutlich Teleport - nicht als Laufstrecke zaehlen
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        double buffered = distanceBuffer.getOrDefault(uuid, 0.0) + distance;
        long whole = (long) Math.floor(buffered);

        if (whole >= 1) {
            distanceBuffer.put(uuid, buffered - whole);
            plugin.getQuestManager().addProgress(player, QuestType.WALK_DISTANCE, null, null, whole);
        } else {
            distanceBuffer.put(uuid, buffered);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        distanceBuffer.remove(event.getPlayer().getUniqueId());
    }
}
