package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /setspawn und /setfarmwelt - setzt den Spawnpunkt der Hauptwelt bzw. der
 * Farmwelt auf die aktuelle Position. Beide Befehle teilen sich diese Klasse
 * (per "farm"-Flag im Konstruktor), analog zu SpawnCommand. Verlangt, dass
 * der Admin tatsaechlich in der betroffenen Welt steht, da eine Spawn-Location
 * nur innerhalb ihrer eigenen Welt sinnvoll ist.
 */
public class SetSpawnCommand implements CommandExecutor {

    private final FrostbergHomes plugin;
    private final boolean farm;

    public SetSpawnCommand(FrostbergHomes plugin, boolean farm) {
        this.plugin = plugin;
        this.farm = farm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        World expectedWorld = farm ? Bukkit.getWorld("farm") : Bukkit.getWorlds().get(0);
        if (expectedWorld == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "farmwelt-not-loaded"));
            return true;
        }

        if (!player.getWorld().equals(expectedWorld)) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), farm ? "farmwelt-wrong-world" : "spawn-wrong-world")
                    .replace("%world%", expectedWorld.getName()));
            return true;
        }

        expectedWorld.setSpawnLocation(player.getLocation());
        player.sendMessage(MessageUtil.get(plugin.getConfig(), farm ? "farmwelt-set" : "spawn-set"));
        return true;
    }
}
