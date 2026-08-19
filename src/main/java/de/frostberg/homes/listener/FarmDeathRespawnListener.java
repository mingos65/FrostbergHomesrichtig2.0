package de.frostberg.homes.listener;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.farmwelt.FarmType;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Wer in einer der drei Farmwelten stirbt, respawnt am gesetzten Spawnpunkt
 * (settings.spawn-world, per /setspawn gesetzt, aktuell plotwelt65) statt am
 * Weltspawn der Farmwelt selbst oder einem Bett dort - die Farmwelten sind
 * nur zum Farmen gedacht, kein sinnvoller Ort zum Wiederbeleben.
 *
 * player.getWorld() liefert innerhalb von PlayerRespawnEvent noch die Welt,
 * in der der Spieler gestorben ist - der eigentliche Weltwechsel passiert erst
 * nach der Event-Verarbeitung, wenn die (ggf. hier geaenderte) Respawn-Location
 * angewendet wird.
 */
public class FarmDeathRespawnListener implements Listener {

    private final FrostbergHomes plugin;

    public FarmDeathRespawnListener(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        String diedInWorld = event.getPlayer().getWorld().getName();
        boolean diedInFarmWorld = false;
        for (FarmType type : FarmType.values()) {
            String farmWorldName = plugin.getConfig().getString("settings.farm-worlds." + type.getConfigKey() + ".world");
            if (diedInWorld.equals(farmWorldName)) {
                diedInFarmWorld = true;
                break;
            }
        }
        if (!diedInFarmWorld) {
            return;
        }

        String spawnWorldName = plugin.getConfig().getString("settings.spawn-world");
        World spawnWorld = spawnWorldName == null ? null : Bukkit.getWorld(spawnWorldName);
        if (spawnWorld == null) {
            return;
        }

        event.setRespawnLocation(spawnWorld.getSpawnLocation());
        event.getPlayer().sendMessage(MessageUtil.get(plugin.getMessages(), "farmwelt-death-respawn"));
    }
}
