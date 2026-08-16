package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.TpaRequest;
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
import java.util.Optional;

/**
 * /tpa <spieler> und /tpahere <spieler> - stellt eine Teleport-Anfrage.
 * Beide Befehle teilen sich diese Klasse (per "here"-Flag im Konstruktor),
 * da sich Ablauf und Pruefungen nur in Richtung und Nachricht unterscheiden.
 * Die Anfrage-Nachricht beim Ziel ist klickbar (siehe sendClickableRequest).
 */
public class TpaCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;
    private final boolean here;

    public TpaCommand(FrostbergHomes plugin, boolean here) {
        this.plugin = plugin;
        this.here = here;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), here ? "tpahere-usage" : "tpa-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "player-not-found")
                    .replace("%player%", args[0]));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-cannot-self"));
            return true;
        }

        Optional<TpaRequest> existing = plugin.getTpaManager().getPendingRequestFrom(player.getUniqueId());
        if (existing.isPresent()) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-already-pending")
                    .replace("%player%", existing.get().getTargetName()));
            return true;
        }

        TpaRequest.Type type = here ? TpaRequest.Type.TPA_HERE : TpaRequest.Type.TPA;

        plugin.getTpaManager().createRequest(player, target, type, () ->
                player.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-expired")
                        .replace("%player%", target.getName())));

        player.sendMessage(MessageUtil.get(plugin.getConfig(), "tpa-request-sent")
                .replace("%player%", target.getName()));

        sendClickableRequest(target, player);
        return true;
    }

    private void sendClickableRequest(Player target, Player sender) {
        String templatePath = here ? "tpahere-request-received" : "tpa-request-received";
        String text = MessageUtil.get(plugin.getConfig(), templatePath).replace("%player%", sender.getName());

        Component message = MessageUtil.toComponent(text)
                .append(button("tpa-accept-button", "tpa-accept-hover", "/tpaccept"))
                .append(MessageUtil.toComponent(MessageUtil.get(plugin.getConfig(), "tpa-button-separator")))
                .append(button("tpa-deny-button", "tpa-deny-hover", "/tpdeny"));

        target.sendMessage(message);
    }

    private Component button(String labelPath, String hoverPath, String runCommand) {
        return MessageUtil.toComponent(MessageUtil.get(plugin.getConfig(), labelPath))
                .clickEvent(ClickEvent.runCommand(runCommand))
                .hoverEvent(HoverEvent.showText(MessageUtil.toComponent(MessageUtil.get(plugin.getConfig(), hoverPath))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
