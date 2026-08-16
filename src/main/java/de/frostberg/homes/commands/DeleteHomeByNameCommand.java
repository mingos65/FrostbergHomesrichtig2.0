package de.frostberg.homes.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.model.Home;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /delhome <name> - Text-Alternative zu /delete home [nr]: sucht das Home mit
 * passendem Anzeigenamen (siehe /sethome) und loescht es.
 */
public class DeleteHomeByNameCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public DeleteHomeByNameCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "usage-delhome"));
            return true;
        }

        String name = String.join(" ", args);

        Map<Integer, Home> homes = plugin.getHomeManager().getHomes(player.getUniqueId());
        Integer matchedNumber = null;
        for (Home home : homes.values()) {
            if (home.getName() != null && home.getName().equalsIgnoreCase(name)) {
                matchedNumber = home.getNumber();
                break;
            }
        }

        if (matchedNumber == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "delhome-not-found")
                    .replace("%name%", name));
            return true;
        }

        plugin.getHomeManager().deleteHome(player.getUniqueId(), matchedNumber);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "home-deleted")
                .replace("%nr%", String.valueOf(matchedNumber)));
        return true;
    }
}
