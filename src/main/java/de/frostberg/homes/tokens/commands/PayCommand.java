package de.frostberg.homes.tokens.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /pay tokens <spieler> <anzahl>
 * /pay gold <spieler> <betrag>
 *
 * Bewusst KEINE eigene Geld-Logik in beiden Zweigen:
 * - "tokens": wird 1:1 an PlayerPoints weitergereicht (dessen "/tokens pay"
 *   kuemmert sich um Guthabenpruefung, Selbstzahlung, Spam-Schutz usw.)
 * - "gold": wir sprechen direkt mit Vault (net.milkbowl.vault.economy.Economy),
 *   der Vermittlungsschicht, bei der EssentialEconomy sich registriert hat.
 *   EssentialEconomy hat selbst keine Moeglichkeit, seinen eigenen "/pay"-
 *   Befehl umzubenennen, deshalb sichern wir uns "/pay" ueber "loadbefore"
 *   in der plugin.yml und rufen die Ueberweisung direkt ueber die Vault-API
 *   auf statt ueber einen Befehl - das ist zuverlaessiger als zu versuchen,
 *   einen fremden, nicht umbenannten Befehl anzusprechen.
 */
public class PayCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public PayCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "usage-pay"));
            return true;
        }

        String type = args[0].toLowerCase();

        if (type.equals("tokens")) {
            handleTokens(player, args);
            return true;
        }

        if (type.equals("gold")) {
            handleGold(player, args);
            return true;
        }

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "usage-pay"));
        return true;
    }

    // ---------------------------------------------------------------
    // /pay tokens <spieler> <anzahl> - Bruecke zu PlayerPoints
    // ---------------------------------------------------------------

    private void handleTokens(Player player, String[] args) {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "tokens-not-installed"));
            return;
        }

        String targetName = args[1];
        String amountText = args[2];

        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException ex) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "invalid-number"));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "invalid-number"));
            return;
        }

        // 1:1-Weiterleitung an PlayerPoints (dort umbenannt zu "tokens") -
        // alle Sicherheits- und Fehlermeldungen (Guthaben, Selbstzahlung,
        // Spam) kommen von dort.
        player.performCommand("tokens pay " + targetName + " " + amount);
    }

    // ---------------------------------------------------------------
    // /pay gold <spieler> <betrag> - direkter Vault-Aufruf (EssentialEconomy)
    // ---------------------------------------------------------------

    private void handleGold(Player player, String[] args) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-not-installed"));
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-not-installed"));
            return;
        }
        Economy economy = rsp.getProvider();

        String targetName = args[1];
        String amountText = args[2];

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException ex) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "invalid-number"));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "invalid-number"));
            return;
        }

        OfflinePlayer target = resolveTarget(targetName);
        if (target == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found")
                    .replace("%player%", targetName));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-pay-self"));
            return;
        }

        if (!economy.has(player, amount)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-pay-insufficient"));
            return;
        }

        EconomyResponse withdraw = economy.withdrawPlayer(player, amount);
        if (!withdraw.transactionSuccess()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-pay-insufficient"));
            return;
        }

        EconomyResponse deposit = economy.depositPlayer(target, amount);
        if (!deposit.transactionSuccess()) {
            // Fehlgeschlagene Einzahlung beim Ziel wieder gutschreiben, damit
            // das Geld nicht verloren geht.
            economy.depositPlayer(player, amount);
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "unknown-error"));
            return;
        }

        String amountFormatted = economy.format(amount);

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-pay-sent")
                .replace("%player%", targetName)
                .replace("%amount%", amountFormatted));

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(MessageUtil.get(plugin.getMessages(), "gold-pay-received")
                    .replace("%player%", player.getName())
                    .replace("%amount%", amountFormatted));
        }
    }

    /**
     * Sucht einen Spieler zuerst online (exakter Name), sonst offline ueber
     * dessen bekannten Namen. Gibt null zurueck, wenn niemand mit diesem
     * Namen je auf dem Server war.
     */
    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline;
        }

        return null;
    }

    // ---------------------------------------------------------------
    // Tab-Complete
    // ---------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("tokens", "gold"), args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("tokens") || args[0].equalsIgnoreCase("gold"))) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
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
