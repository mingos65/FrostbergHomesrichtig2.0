package de.frostberg.homes.enderchest.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ec, /enderchest - oeffnet die eigene Mehrseiten-Enderchest, immer auf Seite 1 (weiterblaettern per Pfeil-Buttons im GUI). */
public class EnderchestCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public EnderchestCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        plugin.getEnderchestGuiListener().openPage(player, 0);
        return true;
    }
}
