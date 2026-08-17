package de.frostberg.homes.staff.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /vanish, /v - unsichtbar fuer alle ohne frostberg.vanish, ohne Join-/Quit-Meldung. */
public class VanishCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public VanishCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        boolean nowVanished = plugin.getVanishManager().toggle(player);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), nowVanished ? "vanish-on" : "vanish-off"));
        return true;
    }
}
