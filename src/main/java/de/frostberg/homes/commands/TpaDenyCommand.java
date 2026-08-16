package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.TpaRequest;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /tpdeny - lehnt die aktuell an einen selbst gerichtete TPA-Anfrage ab.
 * Wird auch beim Klick auf den [Ablehnen]-Button in der Anfrage-Nachricht
 * ausgefuehrt (siehe TpaCommand#sendClickableRequest).
 */
public class TpaDenyCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public TpaDenyCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        TpaRequest request = plugin.getTpaManager().getPendingRequestTo(player.getUniqueId());
        if (request == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-no-pending-request"));
            return true;
        }

        plugin.getTpaManager().removeRequest(request);

        Player requester = Bukkit.getPlayer(request.getSenderUuid());
        if (requester != null) {
            requester.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-denied")
                    .replace("%player%", player.getName()));
        }

        player.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-deny-confirm"));
        return true;
    }
}
