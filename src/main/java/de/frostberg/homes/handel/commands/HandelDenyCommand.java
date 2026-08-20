package de.frostberg.homes.handel.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /handeldeny - lehnt die letzte offene Handelsanfrage ab. */
public class HandelDenyCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public HandelDenyCommand(FrostbergHomes plugin) {
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

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-denied-self"));
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null) {
            requester.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-denied-other").replace("%player%", player.getName()));
        }
        return true;
    }
}
