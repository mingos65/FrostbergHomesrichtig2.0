package de.frostberg.homes.stats.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.CurrencyBridge;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

/** /stats [spieler] - Uebersicht aus Spielzeit, Wallet- und Bankguthaben. */
public class StatsCommand implements CommandExecutor {

    private final FrostbergHomes plugin;
    private final DecimalFormat tokenFormat = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DecimalFormat goldFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));

    public StatsCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean self = args.length == 0;
        OfflinePlayer target;

        if (self) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
                return true;
            }
            target = player;
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer resolved = Bukkit.getOfflinePlayer(args[0]);
            if (!resolved.hasPlayedBefore() && !resolved.isOnline()) {
                sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[0]));
                return true;
            }
            target = resolved;
        }

        UUID uuid = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : plugin.getPlaytimeManager().getStoredName(uuid, args.length > 0 ? args[0] : "?");

        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-header").replace("%player%", name));
        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-playtime")
                .replace("%time%", plugin.getPlaytimeManager().format(plugin.getPlaytimeManager().getTotalSeconds(uuid))));

        boolean hidden = !self && plugin.getBankManager().isHidden(uuid) && !sender.hasPermission("bank.viewhidden");

        if (target.isOnline() && target.getPlayer() != null) {
            Player online = target.getPlayer();
            long tokens = CurrencyBridge.readTokenBalance(online);
            double gold = CurrencyBridge.readGoldBalance(online);
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-wallet-tokens")
                    .replace("%amount%", tokens < 0 ? "?" : tokenFormat.format(tokens)));
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-wallet-gold")
                    .replace("%amount%", gold < 0 ? "?" : goldFormat.format(gold)));
        } else {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-wallet-offline"));
        }

        if (hidden) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-bank-hidden"));
        } else {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-bank-tokens")
                    .replace("%amount%", tokenFormat.format(plugin.getBankManager().getBankTokens(uuid))));
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "stats-bank-gold")
                    .replace("%amount%", goldFormat.format(plugin.getBankManager().getBankGold(uuid))));
        }
        return true;
    }
}
