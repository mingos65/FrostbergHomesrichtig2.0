package de.frostberg.homes.support.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.support.SupportManager;
import de.frostberg.homes.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /support &lt;nachricht&gt; - je nach Zustand des Spielers entweder eine
 * neue Hilfe-Anfrage eroeffnen (kein aktives Ticket) oder eine Nachricht an
 * den zugeteilten Gegenpart weiterleiten (aktives Ticket). /support accept
 * &lt;spieler&gt; (Supporter) uebernimmt eine offene Anfrage, /support close
 * beendet das eigene Ticket - fuer beide Seiten nutzbar.
 */
public class SupportCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public SupportCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "support-usage"));
            return true;
        }

        SupportManager manager = plugin.getSupportManager();

        if (args[0].equalsIgnoreCase("close")) {
            handleClose(player, manager);
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            handleAccept(player, manager, args);
            return true;
        }

        String message = String.join(" ", args);
        SupportManager.Ticket active = manager.findTicketFor(player.getUniqueId());
        if (active != null) {
            relay(active, player, message);
            return true;
        }

        SupportManager.Ticket ticket = manager.createTicket(player.getUniqueId(), message);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "support-request-sent"));
        broadcastToStaff(player, message);
        return true;
    }

    private void handleClose(Player player, SupportManager manager) {
        SupportManager.Ticket ticket = manager.findTicketFor(player.getUniqueId());
        if (ticket == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "support-no-active"));
            return;
        }

        Player requester = Bukkit.getPlayer(ticket.getRequester());
        if (requester != null) {
            requester.sendMessage(MessageUtil.get(plugin.getMessages(), "support-closed"));
        }
        UUID claimedBy = ticket.getClaimedBy();
        if (claimedBy != null) {
            Player staff = Bukkit.getPlayer(claimedBy);
            if (staff != null) {
                staff.sendMessage(MessageUtil.get(plugin.getMessages(), "support-closed"));
            }
        }
        manager.close(ticket.getRequester());
    }

    private void handleAccept(Player player, SupportManager manager, String[] args) {
        if (!player.hasPermission("support.accept")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "support-usage"));
            return;
        }

        Player requester = Bukkit.getPlayerExact(args[1]);
        if (requester == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[1]));
            return;
        }

        SupportManager.Ticket ticket = manager.getTicketByRequester(requester.getUniqueId());
        if (ticket == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "support-not-found"));
            return;
        }
        if (ticket.getClaimedBy() != null) {
            String staffName = Bukkit.getOfflinePlayer(ticket.getClaimedBy()).getName();
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "support-already-claimed")
                    .replace("%staff%", staffName == null ? "?" : staffName));
            return;
        }

        ticket.setClaimedBy(player.getUniqueId());
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "support-accepted-staff")
                .replace("%player%", requester.getName()));
        requester.sendMessage(MessageUtil.get(plugin.getMessages(), "support-accepted-player")
                .replace("%staff%", player.getName()));
    }

    private void relay(SupportManager.Ticket ticket, Player sender, String message) {
        UUID otherUuid = sender.getUniqueId().equals(ticket.getRequester()) ? ticket.getClaimedBy() : ticket.getRequester();
        if (otherUuid == null) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "support-not-claimed-yet"));
            return;
        }
        Player other = Bukkit.getPlayer(otherUuid);
        if (other == null) {
            return;
        }

        String formatted = MessageUtil.get(plugin.getMessages(), "support-message-format")
                .replace("%player%", sender.getName())
                .replace("%message%", message);
        other.sendMessage(formatted);
        sender.sendMessage(formatted);
    }

    private void broadcastToStaff(Player requester, String message) {
        String text = MessageUtil.get(plugin.getMessages(), "support-broadcast")
                .replace("%player%", requester.getName())
                .replace("%message%", message);

        Component full = MessageUtil.toComponent(text).append(
                MessageUtil.toComponent(MessageUtil.get(plugin.getMessages(), "support-accept-button"))
                        .clickEvent(ClickEvent.runCommand("/support accept " + requester.getName()))
                        .hoverEvent(HoverEvent.showText(MessageUtil.toComponent(
                                MessageUtil.get(plugin.getMessages(), "support-accept-hover"))))
        );

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("support.accept")) {
                staff.sendMessage(full);
            }
        }
    }
}
