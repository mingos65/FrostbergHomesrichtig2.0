package de.frostberg.homes.enderchest.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ec, /enderchest - oeffnet die eigene Mehrseiten-Enderchest. /ec name <seite> <name> benennt eine Seite um. */
public class EnderchestCommand implements CommandExecutor {

    private final FrostbergHomes plugin;

    public EnderchestCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("name")) {
            handleRename(player, args);
            return true;
        }

        int limit = plugin.getEnderchestManager().getPageLimit(player);
        if (limit <= 1) {
            plugin.getEnderchestGuiListener().openPage(player, 0);
        } else {
            plugin.getEnderchestGuiListener().openSelector(player);
        }
        return true;
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "ec-name-usage"));
            return;
        }

        int pageNumber;
        try {
            pageNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "ec-name-usage"));
            return;
        }

        int limit = plugin.getEnderchestManager().getPageLimit(player);
        if (pageNumber < 1 || pageNumber > limit) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "ec-invalid-page").replace("%limit%", String.valueOf(limit)));
            return;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) {
                nameBuilder.append(' ');
            }
            nameBuilder.append(args[i]);
        }

        plugin.getEnderchestManager().setPageName(player.getUniqueId(), pageNumber - 1, nameBuilder.toString());
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "ec-name-set")
                .replace("%page%", String.valueOf(pageNumber))
                .replace("%name%", nameBuilder.toString()));
    }
}
