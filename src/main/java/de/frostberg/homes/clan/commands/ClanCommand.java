package de.frostberg.homes.clan.commands;

import de.frostberg.homes.FrostbergHomes;
import de.frostberg.homes.clan.model.Clan;
import de.frostberg.homes.clan.model.ClanInvite;
import de.frostberg.homes.util.MessageUtil;
import de.frostberg.homes.util.TeleportWarmup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * /clan <unterbefehl> - Router fuer das komplette Clan-System, Muster wie
 * PayCommand (args[0] entscheidet den Zweig). Rollen (Leader/Mod/Member)
 * sind reine Clan-Daten, keine Bukkit-Permissions - Rechteprüfung passiert
 * hier anhand der im Clan gespeicherten Rolle. Nur "wer darf /clan ueberhaupt
 * benutzen" (clan.use) und "Admin-Override" (clan.admin) laufen ueber
 * Bukkit-Permissions (siehe plugin.yml).
 */
public class ClanCommand implements CommandExecutor, TabCompleter, Listener {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "delete", "disband", "invite", "accept", "deny", "leave", "kick",
            "promote", "demote", "info", "list", "chat", "setbase", "base", "rename", "bank", "color"
    );

    private final FrostbergHomes plugin;
    private final Map<UUID, BukkitTask> pendingBaseTeleports = new HashMap<>();

    public ClanCommand(FrostbergHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin.getMessages(), "player-only"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "delete", "disband" -> handleDelete(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "promote" -> handleSetRole(player, args, Clan.Role.MOD, "clan-promoted");
            case "demote" -> handleSetRole(player, args, Clan.Role.MEMBER, "clan-demoted");
            case "info" -> handleInfo(player, args);
            case "list" -> plugin.getClanGuiListener().openList(player, 0);
            case "chat" -> handleChat(player, args, 1);
            case "setbase" -> handleSetBase(player);
            case "base" -> handleBase(player);
            case "rename" -> handleRename(player, args);
            case "bank" -> handleBank(player, args);
            case "color" -> handleColor(player);
            default -> player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-usage"));
        }
        return true;
    }

    // ---------------------------------------------------------------
    // create / delete
    // ---------------------------------------------------------------

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-create-usage"));
            return;
        }
        // Tag ist seit dem Chat-System-Update Pflicht (vorher optional/automatisch
        // aus dem Namen abgeleitet) - ohne Tag keine sinnvolle Chat-/Tab-Anzeige.
        if (args.length < 3) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-tag-required"));
            return;
        }

        if (plugin.getClanManager().getClanOf(player.getUniqueId()).isPresent()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-already-in-clan"));
            return;
        }

        String name = args[1];
        if (name.length() < 2 || name.length() > 15) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-name-invalid-length"));
            return;
        }

        if (plugin.getClanManager().existsByName(name)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-name-taken"));
            return;
        }

        String tag = args[2];
        int tagMin = plugin.getConfig().getInt("clan.tag-min-length", 2);
        int tagMax = plugin.getConfig().getInt("clan.tag-max-length", 6);

        if (tag.length() < tagMin || tag.length() > tagMax || isBlocked(name) || isBlocked(tag)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-tag-invalid")
                    .replace("%min%", String.valueOf(tagMin))
                    .replace("%max%", String.valueOf(tagMax)));
            return;
        }

        plugin.getClanManager().createClan(name, tag, player);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-created").replace("%clan%", name));
    }

    private boolean isBlocked(String text) {
        List<String> blocked = plugin.getConfig().getStringList("clan.blocked-words");
        String lower = text.toLowerCase();
        for (String word : blocked) {
            if (lower.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void handleDelete(Player player) {
        Optional<Clan> clanOpt = plugin.getClanManager().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-not-in-clan"));
            return;
        }

        Clan clan = clanOpt.get();
        if (clan.getRole(player.getUniqueId()) != Clan.Role.LEADER && !player.hasPermission("clan.admin")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-not-leader"));
            return;
        }

        plugin.getClanGuiListener().openConfirmDelete(player, clan.getName());
    }

    // ---------------------------------------------------------------
    // invite / accept / deny / leave / kick
    // ---------------------------------------------------------------

    private void handleInvite(Player player, String[] args) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (clan.getRole(player.getUniqueId()) == Clan.Role.MEMBER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-permission-role"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-invite-usage"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "player-not-found").replace("%player%", args[1]));
            return;
        }

        if (plugin.getClanManager().getClanOf(target.getUniqueId()).isPresent()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-target-already-in-clan").replace("%player%", target.getName()));
            return;
        }

        if (clan.getMemberCount() >= plugin.getClanManager().getMaxMembers()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-full"));
            return;
        }

        plugin.getClanManager().createInvite(clan.getName(), target, player.getName(), () ->
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-invite-expired").replace("%player%", target.getName())));

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-invite-sent").replace("%player%", target.getName()));
        sendClickableInvite(target, clan.getName(), player.getName());
    }

    private void sendClickableInvite(Player target, String clanName, String inviterName) {
        String text = MessageUtil.get(plugin.getMessages(), "clan-invite-received")
                .replace("%player%", inviterName)
                .replace("%clan%", clanName);

        Component message = MessageUtil.toComponent(text)
                .append(button("tpa-accept-button", "clan-invite-accept-hover", "/clan accept"))
                .append(MessageUtil.toComponent(MessageUtil.get(plugin.getMessages(), "tpa-button-separator")))
                .append(button("tpa-deny-button", "clan-invite-deny-hover", "/clan deny"));

        target.sendMessage(message);
    }

    private Component button(String labelPath, String hoverPath, String runCommand) {
        return MessageUtil.toComponent(MessageUtil.get(plugin.getMessages(), labelPath))
                .clickEvent(ClickEvent.runCommand(runCommand))
                .hoverEvent(HoverEvent.showText(MessageUtil.toComponent(MessageUtil.get(plugin.getMessages(), hoverPath))));
    }

    private void handleAccept(Player player) {
        Optional<ClanInvite> inviteOpt = plugin.getClanManager().getPendingInvite(player.getUniqueId());
        if (inviteOpt.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-pending-invite"));
            return;
        }
        ClanInvite invite = inviteOpt.get();

        if (plugin.getClanManager().getClanOf(player.getUniqueId()).isPresent()) {
            plugin.getClanManager().removeInvite(invite);
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-already-in-clan"));
            return;
        }

        Optional<Clan> clanOpt = plugin.getClanManager().getClan(invite.getClanName());
        plugin.getClanManager().removeInvite(invite);

        if (clanOpt.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-pending-invite"));
            return;
        }

        Clan clan = clanOpt.get();
        if (clan.getMemberCount() >= plugin.getClanManager().getMaxMembers()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-full"));
            return;
        }

        plugin.getClanManager().addMember(clan, player.getUniqueId());
        broadcastToClan(clan, MessageUtil.get(plugin.getMessages(), "clan-joined").replace("%player%", player.getName()));
    }

    private void handleDeny(Player player) {
        Optional<ClanInvite> inviteOpt = plugin.getClanManager().getPendingInvite(player.getUniqueId());
        if (inviteOpt.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-pending-invite"));
            return;
        }

        ClanInvite invite = inviteOpt.get();
        plugin.getClanManager().removeInvite(invite);

        Player inviter = Bukkit.getPlayerExact(invite.getInviterName());
        if (inviter != null) {
            inviter.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-invite-denied").replace("%player%", player.getName()));
        }
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-deny-confirm"));
    }

    private void handleLeave(Player player) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (clan.getRole(player.getUniqueId()) == Clan.Role.LEADER && clan.getMemberCount() > 1) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-leader-must-transfer"));
            return;
        }

        plugin.getClanGuiListener().openConfirmLeave(player, clan.getName());
    }

    private void handleKick(Player player, String[] args) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (clan.getRole(player.getUniqueId()) == Clan.Role.MEMBER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-permission-role"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-kick-usage"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUuid = target != null ? target.getUniqueId() : null;

        if (targetUuid == null || !clan.isMember(targetUuid)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-target-not-member").replace("%player%", args[1]));
            return;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-cannot-kick-self"));
            return;
        }

        if (clan.getRole(targetUuid) == Clan.Role.LEADER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-cannot-kick-leader"));
            return;
        }

        plugin.getClanManager().removeMember(clan, targetUuid);
        broadcastToClan(clan, MessageUtil.get(plugin.getMessages(), "clan-kicked").replace("%player%", target.getName()));
        target.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-you-were-kicked").replace("%clan%", clan.getName()));
    }

    // ---------------------------------------------------------------
    // promote / demote
    // ---------------------------------------------------------------

    private void handleSetRole(Player player, String[] args, Clan.Role newRole, String successKey) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (clan.getRole(player.getUniqueId()) != Clan.Role.LEADER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-not-leader"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-role-usage"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUuid = target != null ? target.getUniqueId() : null;

        if (targetUuid == null || !clan.isMember(targetUuid) || clan.getRole(targetUuid) == Clan.Role.LEADER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-target-not-member").replace("%player%", args[1]));
            return;
        }

        plugin.getClanManager().setRole(clan, targetUuid, newRole);
        broadcastToClan(clan, MessageUtil.get(plugin.getMessages(), successKey).replace("%player%", target.getName()));
    }

    // ---------------------------------------------------------------
    // info / chat / rename
    // ---------------------------------------------------------------

    private void handleInfo(Player player, String[] args) {
        Optional<Clan> clanOpt = args.length >= 2
                ? plugin.getClanManager().getClan(args[1])
                : plugin.getClanManager().getClanOf(player.getUniqueId());

        if (clanOpt.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-not-found"));
            return;
        }

        Clan clan = clanOpt.get();
        String leaderName = Bukkit.getOfflinePlayer(clan.getLeaderUuid()).getName();

        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-info-header").replace("%clan%", clan.getName()));
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-info-tag").replace("%tag%", clan.getTag() != null ? clan.getTag() : "-"));
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-info-leader").replace("%player%", leaderName != null ? leaderName : "?"));
        player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-info-members")
                .replace("%count%", String.valueOf(clan.getMemberCount()))
                .replace("%max%", String.valueOf(plugin.getClanManager().getMaxMembers())));
    }

    /** Oeffentlich, damit ClanChatCommand (/cc) denselben Ablauf nutzen kann. */
    public void handleChat(Player player, String[] args, int messageStartIndex) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }

        if (args.length <= messageStartIndex) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-chat-usage"));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, messageStartIndex, args.length));
        String formatted = MessageUtil.get(plugin.getMessages(), "clan-chat-format")
                .replace("%player%", player.getName())
                .replace("%message%", message);

        broadcastToClan(clanOpt.get(), formatted);
    }

    private void handleRename(Player player, String[] args) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (clan.getRole(player.getUniqueId()) != Clan.Role.LEADER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-not-leader"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-rename-usage"));
            return;
        }

        String newName = args[1];
        if (newName.length() < 3 || newName.length() > 24 || isBlocked(newName) || !plugin.getClanManager().renameClan(clan, newName)) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-name-taken"));
            return;
        }

        broadcastToClan(clan, MessageUtil.get(plugin.getMessages(), "clan-renamed").replace("%clan%", newName));
    }

    // ---------------------------------------------------------------
    // setbase / base
    // ---------------------------------------------------------------

    private void handleSetBase(Player player) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (clan.getRole(player.getUniqueId()) == Clan.Role.MEMBER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-permission-role"));
            return;
        }

        clan.setBase(player.getLocation());
        plugin.getClanManager().saveClan(clan);
        broadcastToClan(clan, MessageUtil.get(plugin.getMessages(), "clan-base-set"));
    }

    private void handleBase(Player player) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (!clan.hasBase() || clan.getBaseLocation() == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-base"));
            return;
        }

        TeleportWarmup.start(plugin, player, pendingBaseTeleports, "clan.admin", null, () -> {
            player.teleport(clan.getBaseLocation());
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-base-success"));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TeleportWarmup.cancel(pendingBaseTeleports, event.getPlayer().getUniqueId());
        plugin.getClanManager().clearInvitesOf(event.getPlayer().getUniqueId());
    }

    // ---------------------------------------------------------------
    // bank (Tokens + Gold, duenne Bruecke zu PlayerPoints/Vault)
    // ---------------------------------------------------------------

    private void handleBank(Player player, String[] args) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-bank-balance")
                    .replace("%tokens%", String.valueOf(clan.getTokensBalance()))
                    .replace("%gold%", String.valueOf(clan.getGoldBalance())));
            return;
        }

        String action = args[1].toLowerCase();
        if (!action.equals("deposit") && !action.equals("withdraw")) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-bank-usage"));
            return;
        }

        boolean withdraw = action.equals("withdraw");
        if (withdraw && clan.getRole(player.getUniqueId()) == Clan.Role.MEMBER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-permission-role"));
            return;
        }

        if (args.length < 4) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-bank-usage"));
            return;
        }

        String currency = args[2].toLowerCase();
        if (currency.equals("tokens")) {
            handleBankTokens(player, clan, withdraw, args[3]);
        } else if (currency.equals("gold")) {
            handleBankGold(player, clan, withdraw, args[3]);
        } else {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-bank-usage"));
        }
    }

    /**
     * Reicht wie /pay tokens 1:1 an PlayerPoints weiter (dessen eigene
     * /tokens take|give-Befehle, ueber die Konsole ausgefuehrt, damit die
     * Admin-Permission nicht beim aufrufenden Spieler liegen muss). Der
     * Klan-Kontostand ist eine eigene Zahl in unserer Clan-Datei, keine
     * eigene Geld-Logik - wir verschieben nur zwischen Spieler-Guthaben und
     * dieser Zahl.
     */
    private void handleBankTokens(Player player, Clan clan, boolean withdraw, String amountText) {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "tokens-not-installed"));
            return;
        }

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

        if (withdraw && clan.getTokensBalance() < amount) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-bank-insufficient"));
            return;
        }

        String command = (withdraw ? "tokens give " : "tokens take ") + player.getName() + " " + amount;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        clan.setTokensBalance(clan.getTokensBalance() + (withdraw ? -amount : amount));
        plugin.getClanManager().saveClan(clan);

        player.sendMessage(MessageUtil.get(plugin.getMessages(), withdraw ? "clan-bank-withdrawn" : "clan-bank-deposited")
                .replace("%amount%", amount + " Tokens"));
    }

    private void handleBankGold(Player player, Clan clan, boolean withdraw, String amountText) {
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

        if (withdraw) {
            if (clan.getGoldBalance() < amount) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-bank-insufficient"));
                return;
            }
            clan.setGoldBalance(clan.getGoldBalance() - amount);
            economy.depositPlayer(player, amount);
        } else {
            if (!economy.has(player, amount)) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "gold-pay-insufficient"));
                return;
            }
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            if (!response.transactionSuccess()) {
                player.sendMessage(MessageUtil.get(plugin.getMessages(), "unknown-error"));
                return;
            }
            clan.setGoldBalance(clan.getGoldBalance() + amount);
        }

        plugin.getClanManager().saveClan(clan);
        player.sendMessage(MessageUtil.get(plugin.getMessages(), withdraw ? "clan-bank-withdrawn" : "clan-bank-deposited")
                .replace("%amount%", economy.format(amount)));
    }

    // ---------------------------------------------------------------
    // color (Clan-Tag-Farbe mit Gold kaufen)
    // ---------------------------------------------------------------

    private void handleColor(Player player) {
        Optional<Clan> clanOpt = requireClan(player);
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        if (clan.getRole(player.getUniqueId()) == Clan.Role.MEMBER) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-no-permission-role"));
            return;
        }

        plugin.getClanGuiListener().openColorShop(player, clan.getName());
    }

    // ---------------------------------------------------------------
    // Hilfsmittel
    // ---------------------------------------------------------------

    private Optional<Clan> requireClan(Player player) {
        Optional<Clan> clanOpt = plugin.getClanManager().getClanOf(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(MessageUtil.get(plugin.getMessages(), "clan-not-in-clan"));
        }
        return clanOpt;
    }

    private void broadcastToClan(Clan clan, String message) {
        for (UUID uuid : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(message);
            }
        }
    }

    // ---------------------------------------------------------------
    // Tab-Complete
    // ---------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && List.of("invite", "kick", "promote", "demote").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("bank")) {
            return filter(Arrays.asList("deposit", "withdraw"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("bank")) {
            return filter(Arrays.asList("tokens", "gold"), args[2]);
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
