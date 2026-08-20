package de.frostberg.homes.report.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/** /reports &lt;spieler&gt; - zeigt dem Team die gespeicherte Melde-Historie eines Spielers. */
public class ReportsCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public ReportsCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("report.view")) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "reports-usage"));
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[0]));
            return true;
        }

        List<Map<String, Object>> reports = plugin.getReportManager().getReports(target);
        if (reports.isEmpty()) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "reports-empty").replace("%player%", args[0]));
            return true;
        }

        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "reports-header")
                .replace("%player%", args[0])
                .replace("%count%", String.valueOf(reports.size())));
        for (Map<String, Object> entry : reports) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "reports-entry")
                    .replace("%reporter%", String.valueOf(entry.get("reporter")))
                    .replace("%reason%", String.valueOf(entry.get("reason")))
                    .replace("%time%", String.valueOf(entry.get("time"))));
        }
        return true;
    }
}
