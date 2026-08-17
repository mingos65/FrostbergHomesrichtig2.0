package de.frostberg.homes.chat.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.util.ColorUtil;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * /chatcolor - eigene Chat-Farbe einstellen. Einfache Legacy-Farben sind fuer
 * alle Spieler frei (frostbergchat.color.use), Hex-Farben und Verlaeufe
 * brauchen frostbergchat.color.rgb, Fettschrift frostbergchat.color.bold.
 * Die eigentliche Anwendung passiert nicht hier: fuer feste Farben liest
 * QuestPlaceholderExpansion... nein, ChatColorPlaceholderExpansion die
 * gespeicherte Farbe per PlaceholderAPI (fuer FancyChats Format-String),
 * fuer Verlaeufe faerbt ChatFormatListener die eigentliche Nachricht direkt.
 */
public class ChatColorCommand implements CommandExecutor, TabCompleter {

    private static final Map<String, String> NAMED_COLORS = new LinkedHashMap<>();

    static {
        NAMED_COLORS.put("schwarz", "&0");
        NAMED_COLORS.put("dunkelblau", "&1");
        NAMED_COLORS.put("dunkelgruen", "&2");
        NAMED_COLORS.put("tuerkis", "&3");
        NAMED_COLORS.put("dunkelrot", "&4");
        NAMED_COLORS.put("lila", "&5");
        NAMED_COLORS.put("gold", "&6");
        NAMED_COLORS.put("grau", "&7");
        NAMED_COLORS.put("dunkelgrau", "&8");
        NAMED_COLORS.put("blau", "&9");
        NAMED_COLORS.put("gruen", "&a");
        NAMED_COLORS.put("aqua", "&b");
        NAMED_COLORS.put("rot", "&c");
        NAMED_COLORS.put("pink", "&d");
        NAMED_COLORS.put("gelb", "&e");
        NAMED_COLORS.put("weiss", "&f");
    }

    private final FrostbergHomes plugin;

    public ChatColorCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-usage"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reset" -> {
                plugin.getChatColorManager().clearColor(player.getUniqueId());
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-reset"));
            }
            case "bold", "fett" -> handleBold(player);
            case "hex" -> handleHex(player, args);
            case "gradient", "verlauf" -> handleGradient(player, args);
            default -> handleNamedColor(player, sub);
        }
        return true;
    }

    private void handleNamedColor(Player player, String name) {
        String code = NAMED_COLORS.get(name);
        if (code == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-invalid-name"));
            return;
        }

        plugin.getChatColorManager().setColor(player.getUniqueId(), code);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-set")
                .replace("%color%", ColorUtil.applyColorCode(code, name)));
    }

    private void handleHex(Player player, String[] args) {
        if (!player.hasPermission("frostbergchat.color.rgb")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-no-permission-rgb"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-usage"));
            return;
        }

        String hex = args[1].replace("#", "");
        if (!ColorUtil.isValidHex(hex)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-invalid-hex"));
            return;
        }

        String code = "&#" + hex;
        plugin.getChatColorManager().setColor(player.getUniqueId(), code);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-set")
                .replace("%color%", ColorUtil.applyColorCode(code, "#" + hex.toUpperCase())));
    }

    private void handleGradient(Player player, String[] args) {
        if (!player.hasPermission("frostbergchat.color.rgb")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-no-permission-rgb"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-usage"));
            return;
        }

        String from = args[1].replace("#", "");
        String to = args[2].replace("#", "");
        if (!ColorUtil.isValidHex(from) || !ColorUtil.isValidHex(to)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-invalid-hex"));
            return;
        }

        String code = "gradient:" + from + ":" + to;
        plugin.getChatColorManager().setColor(player.getUniqueId(), code);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-set")
                .replace("%color%", ColorUtil.gradient("Verlauf", from, to)));
    }

    private void handleBold(Player player) {
        if (!player.hasPermission("frostbergchat.color.bold")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "chatcolor-no-permission-bold"));
            return;
        }

        boolean newValue = !plugin.getChatColorManager().isBold(player.getUniqueId());
        plugin.getChatColorManager().setBold(player.getUniqueId(), newValue);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), newValue ? "chatcolor-bold-on" : "chatcolor-bold-off"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(NAMED_COLORS.keySet());
            options.add("reset");
            options.add("bold");
            if (sender.hasPermission("frostbergchat.color.rgb")) {
                options.add("hex");
                options.add("gradient");
            }
            return filter(options, args[0]);
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
