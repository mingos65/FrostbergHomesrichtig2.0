package de.frostberg.homes.stats.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

/** /top - Rangliste nach Spielzeit, absteigend. */
public class TopCommand implements CommandExecutor {

    private static final int LIMIT = 10;

    private final FrostbergHomes plugin;

    public TopCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<UUID> top = plugin.getPlaytimeManager().getTop(LIMIT);
        if (top.isEmpty()) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "top-empty"));
            return true;
        }

        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "top-header"));
        int rank = 1;
        for (UUID uuid : top) {
            String name = plugin.getPlaytimeManager().getStoredName(uuid, uuid.toString());
            String time = plugin.getPlaytimeManager().format(plugin.getPlaytimeManager().getTotalSeconds(uuid));
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "top-entry")
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%player%", name)
                    .replace("%time%", time));
            rank++;
        }
        return true;
    }
}
