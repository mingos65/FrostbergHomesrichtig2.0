package de.frostberg.homes.bank.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /bank oeffnet das Bank-GUI, /bank hide versteckt das eigene Guthaben vor anderen (z.B. /stats). */
public class BankCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public BankCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("hide")) {
            boolean nowHidden = !plugin.getBankManager().isHidden(player.getUniqueId());
            plugin.getBankManager().setHidden(player.getUniqueId(), nowHidden);
            player.sendMessage(MessageUtil.get(plugin.getMessages(), nowHidden ? "bank-hide-on" : "bank-hide-off"));
            return true;
        }

        plugin.getBankGuiListener().open(player);
        return true;
    }
}
