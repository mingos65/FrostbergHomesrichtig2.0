package de.frostberg.homes.shop.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /shop oeffnet das Hauptmenue, /shop reload laedt shop-items.yml neu (fuer
 * Preisaenderungen ohne Serverneustart), analog zu /homes reload.
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public ShopCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("shop.admin")) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
                return true;
            }
            plugin.getShopManager().load();
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "shop-reloaded"));
            return true;
        }

        plugin.getShopGuiListener().openMain(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("shop.admin")) {
            return List.of("reload");
        }
        return List.of();
    }
}
