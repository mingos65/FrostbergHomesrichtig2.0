package de.frostberg.homes.report.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /report &lt;spieler&gt; &lt;grund&gt; - meldet einen Spieler dem Team, mit Cooldown gegen Spam. */
public class ReportCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public ReportCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "report-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[0]));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "report-cannot-self"));
            return true;
        }

        if (plugin.getReportManager().isOnCooldown(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "report-cooldown")
                    .replace("%seconds%", String.valueOf(plugin.getReportManager().getRemainingCooldownSeconds(player.getUniqueId()))));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                reasonBuilder.append(' ');
            }
            reasonBuilder.append(args[i]);
        }
        String reason = reasonBuilder.toString();

        plugin.getReportManager().addReport(player, target, reason);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "report-sent").replace("%player%", target.getName()));

        String broadcast = MessageUtil.get(plugin.getMessages(), "report-broadcast")
                .replace("%reporter%", player.getName())
                .replace("%reported%", target.getName())
                .replace("%reason%", reason);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("report.view")) {
                staff.sendMessage(broadcast);
            }
        }
        return true;
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
        return names.isEmpty() ? Collections.emptyList() : names;
    }
}
