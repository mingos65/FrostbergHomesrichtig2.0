package de.frostberg.homes.armorstand.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/** /ast - oeffnet das Bearbeitungsmenue fuer den Armor Stand, den der Spieler gerade ansieht (max. 6 Bloecke entfernt). */
public class ArmorStandCommand implements CommandExecutor {

    private static final double MAX_DISTANCE = 6.0;
    private static final double RAY_SIZE = 0.3;

    private final FrostbergHomes plugin;

    public ArmorStandCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult result = world.rayTraceEntities(eye, eye.getDirection(), MAX_DISTANCE, RAY_SIZE,
                entity -> entity instanceof ArmorStand);

        if (result == null || !(result.getHitEntity() instanceof ArmorStand stand)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "ast-not-found"));
            return true;
        }

        plugin.getArmorStandGuiListener().open(player, stand);
        return true;
    }
}
