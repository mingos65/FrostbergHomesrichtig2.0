package de.frostberg.homes.quest.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.quest.manager.QuestManager;
import de.frostberg.homes.quest.model.PlayerQuestData;
import de.frostberg.homes.quest.model.Quest;
import de.frostberg.homes.quest.model.QuestCategory;
import de.frostberg.homes.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /quest, /quests - oeffnet ohne Argumente das GUI, sonst Router fuer die
 * Unterbefehle top (fuer alle) sowie reload/reset/info/broadcast (Admin).
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private final FrostbergHomes plugin;

    public QuestCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
                return true;
            }
            plugin.getQuestGuiListener().openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "top" -> handleTop(sender);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "info" -> handleInfo(sender, args);
            case "broadcast" -> handleBroadcast(sender, args);
            default -> {
                if (sender instanceof Player player) {
                    plugin.getQuestGuiListener().openMain(player);
                } else {
                    sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
                }
            }
        }
        return true;
    }

    private void handleTop(CommandSender sender) {
        List<QuestManager.TopEntry> top = plugin.getQuestManager().getTopPlayers(10);
        if (top.isEmpty()) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-top-empty"));
            return;
        }

        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-top-header"));
        int rank = 1;
        for (QuestManager.TopEntry entry : top) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.uuid());
            String name = offline.getName() != null ? offline.getName() : entry.uuid().toString();
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-top-entry")
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%player%", name)
                    .replace("%amount%", String.valueOf(entry.total())));
            rank++;
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("quest.admin")) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
            return;
        }
        plugin.getQuestManager().reload();
        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-reload-success"));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quest.admin")) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-reset-usage"));
            return;
        }

        OfflinePlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[1]));
            return;
        }

        QuestCategory category = parseCategory(args[2]);
        if (category == null) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-category-invalid"));
            return;
        }

        plugin.getQuestManager().resetPlayer(target.getUniqueId(), category);
        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-reset-success")
                .replace("%player%", String.valueOf(target.getName()))
                .replace("%category%", plugin.getQuestManager().categoryDisplayName(category)));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quest.admin")) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-info-usage"));
            return;
        }

        OfflinePlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[1]));
            return;
        }

        PlayerQuestData data = plugin.getQuestManager().getData(target.getUniqueId());
        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-info-header").replace("%player%", String.valueOf(target.getName())));

        for (QuestCategory category : QuestCategory.values()) {
            List<Quest> active = plugin.getQuestManager().getActiveQuests(category);
            int done = 0;
            for (Quest quest : active) {
                if (data.getProgress(category, quest.getId()) >= quest.getAmount()) {
                    done++;
                }
            }
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-info-line")
                    .replace("%category%", plugin.getQuestManager().categoryDisplayName(category))
                    .replace("%done%", String.valueOf(done))
                    .replace("%total%", String.valueOf(active.size())));
        }

        sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-info-stats")
                .replace("%streak%", String.valueOf(data.getStreak()))
                .replace("%total%", String.valueOf(data.getTotalCompleted())));
    }

    private void handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quest.admin")) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "quest-broadcast-usage"));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getQuestManager().broadcastAdminMessage(message);
    }

    private QuestCategory parseCategory(String input) {
        return switch (input.toLowerCase()) {
            case "daily" -> QuestCategory.DAILY;
            case "weekly" -> QuestCategory.WEEKLY;
            case "monthly" -> QuestCategory.MONTHLY;
            default -> null;
        };
    }

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
            List<String> options = new ArrayList<>(List.of("top"));
            if (sender.hasPermission("quest.admin")) {
                options.addAll(List.of("reload", "reset", "info", "broadcast"));
            }
            return filter(options, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("info"))) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
            return filter(List.of("daily", "weekly", "monthly"), args[2]);
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
