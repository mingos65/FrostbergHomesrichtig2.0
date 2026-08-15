package de.frostberg.homes.tokens.commands;

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

/**
 * /pay tokens <spieler> <anzahl>
 *
 * Bewusst KEINE eigene Geld-Logik: Dieser Befehl prueft nur das Format und
 * reicht die eigentliche Ueberweisung an PlayerPoints weiter (dessen
 * "/tokens pay"-Befehl kuemmert sich um Guthabenpruefung, Selbstzahlung,
 * Spam-Schutz usw. - das wollen wir nicht neu erfinden).
 *
 * WICHTIG: PlayerPoints' Hauptbefehl wurde in commands/points.yml von
 * "points" auf "tokens" umbenannt - genau dieser Name wird hier unten
 * beim Weiterleiten verwendet. Falls der Name dort nochmal geaendert wird,
 * muss die Zeile mit player.performCommand(...) entsprechend mitgeaendert
 * werden.
 */
public class PayCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public PayCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getConfig(), "player-only"));
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("tokens")) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "usage-pay"));
            return true;
        }

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "tokens-not-installed"));
            return true;
        }

        String targetName = args[1];
        String amountText = args[2];

        // Nur Format pruefen (positive ganze Zahl) - ob genug Guthaben da ist,
        // entscheidet PlayerPoints selbst beim eigentlichen Ausfuehren.
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException ex) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "invalid-number"));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(MessageUtil.get(plugin.getConfig(), "invalid-number"));
            return true;
        }

        // 1:1-Weiterleitung an PlayerPoints (dort umbenannt zu "tokens") -
        // alle Sicherheits- und Fehlermeldungen (Guthaben, Selbstzahlung,
        // Spam) kommen von dort.
        player.performCommand("tokens pay " + targetName + " " + amount);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Collections.singletonList("tokens"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tokens")) {
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
