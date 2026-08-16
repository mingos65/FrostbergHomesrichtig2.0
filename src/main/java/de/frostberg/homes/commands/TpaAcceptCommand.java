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
 * /tpaccept - nimmt die aktuell an einen selbst gerichtete TPA-Anfrage an.
 * Wird auch beim Klick auf den [Annehmen]-Button in der Anfrage-Nachricht
 * ausgefuehrt (siehe TpaCommand#sendClickableRequest).
 */
public class TpaAcceptCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public TpaAcceptCommand(FrostbergHomes plugin) {
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
            requester.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-accepted")
                    .replace("%player%", player.getName()));
        }

        Player teleportingPlayer = Bukkit.getPlayer(request.getTeleportingUuid());
        Player destinationPlayer = Bukkit.getPlayer(request.getDestinationUuid());

        if (teleportingPlayer != null && destinationPlayer != null) {
            plugin.getTpaManager().startAcceptedTeleport(teleportingPlayer, destinationPlayer);
        }

        return true;
    }
}
