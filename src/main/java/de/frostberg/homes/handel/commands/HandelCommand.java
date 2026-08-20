package de.frostberg.homes.handel.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /handel &lt;spieler&gt; - schickt eine klickbare Handelsanfrage, die der Zielspieler annehmen/ablehnen kann. */
public class HandelCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public HandelCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[0]));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-cannot-self"));
            return true;
        }
        if (plugin.getTradeManager().hasActiveSession(player.getUniqueId()) || plugin.getTradeManager().hasActiveSession(target.getUniqueId())) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-already-trading"));
            return true;
        }

        plugin.getTradeManager().createRequest(target.getUniqueId(), player.getUniqueId());
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "handel-request-sent").replace("%player%", target.getName()));

        String text = MessageUtil.get(plugin.getMessages(), "handel-request-received").replace("%player%", player.getName());
        Component message = MessageUtil.toComponent(text)
                .append(button("handel-accept-button", "handel-accept-hover", "/handelaccept"))
                .append(MessageUtil.toComponent(MessageUtil.get(plugin.getMessages(), "handel-button-separator")))
                .append(button("handel-deny-button", "handel-deny-hover", "/handeldeny"));
        target.sendMessage(message);
        return true;
    }

    private Component button(String labelPath, String hoverPath, String runCommand) {
        return MessageUtil.toComponent(MessageUtil.get(plugin.getMessages(), labelPath))
                .clickEvent(ClickEvent.runCommand(runCommand))
                .hoverEvent(HoverEvent.showText(MessageUtil.toComponent(MessageUtil.get(plugin.getMessages(), hoverPath))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                names.add(online.getName());
            }
        }
        return names;
    }
}
