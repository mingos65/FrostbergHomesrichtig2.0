package de.frostberg.homes.lagclear.commands;

import de.frostberg.homes.FrostbergHomes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /laggclear - loest sofort eine manuelle Bodenitem-Bereinigung aus (mit kurzer Vorwarnung). */
public class LagClearCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public LagClearCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLagClearManager().manualClear();
        return true;
    }
}
