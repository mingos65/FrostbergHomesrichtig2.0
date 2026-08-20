package de.frostberg.homes.handel.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /handelaccept - nimmt die letzte offene Handelsanfrage an und startet das gemeinsame Handel-Fenster. */
public class HandelAcceptCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public HandelAcceptCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        UUID requesterId = plugin.getTradeManager().takeRequest(player.getUniqueId());
        if (requesterId == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-no-pending"));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-requester-offline"));
            return true;
        }
        if (plugin.getTradeManager().hasActiveSession(player.getUniqueId()) || plugin.getTradeManager().hasActiveSession(requester.getUniqueId())) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-already-trading"));
            return true;
        }

        plugin.getHandelGuiListener().startTrade(requester, player);
        return true;
    }
}
