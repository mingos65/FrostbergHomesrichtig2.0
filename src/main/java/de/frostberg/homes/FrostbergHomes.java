package de.frostberg.homes;

import de.frostberg.homes.armorstand.commands.ArmorStandCommand;
import de.frostberg.homes.armorstand.gui.ArmorStandGuiListener;
import de.frostberg.homes.bank.BankManager;
import de.frostberg.homes.bank.commands.BankCommand;
import de.frostberg.homes.bank.gui.BankGuiListener;
import de.frostberg.homes.chat.ChatColorManager;
import de.frostberg.homes.chat.ChatFormatListener;
import de.frostberg.homes.chat.ChatModeListener;
import de.frostberg.homes.chat.ChatModeManager;
import de.frostberg.homes.chat.commands.AdminChatCommand;
import de.frostberg.homes.chat.commands.ChatClearCommand;
import de.frostberg.homes.chat.commands.ChatColorCommand;
import de.frostberg.homes.chat.commands.TeamChatCommand;
import de.frostberg.homes.clan.commands.ClanChatCommand;
import de.frostberg.homes.clan.commands.ClanCommand;
import de.frostberg.homes.clan.gui.ClanGuiListener;
import de.frostberg.homes.clan.manager.ClanManager;
import de.frostberg.homes.commands.AdminTpCommand;
import de.frostberg.homes.commands.DeleteHomeByNameCommand;
import de.frostberg.homes.commands.DeleteHomeCommand;
import de.frostberg.homes.commands.HomeCommand;
import de.frostberg.homes.commands.HomesCommand;
import de.frostberg.homes.commands.SetHomeByNameCommand;
import de.frostberg.homes.commands.SetHomeCommand;
import de.frostberg.homes.commands.SetSpawnCommand;
import de.frostberg.homes.commands.SpawnCommand;
import de.frostberg.homes.commands.TpaAcceptCommand;
import de.frostberg.homes.commands.TpaCommand;
import de.frostberg.homes.commands.TpaDenyCommand;
import de.frostberg.homes.enderchest.EnderchestManager;
import de.frostberg.homes.enderchest.commands.EnderchestCommand;
import de.frostberg.homes.enderchest.gui.EnderchestGuiListener;
import de.frostberg.homes.farmwelt.commands.FarmweltCommand;
import de.frostberg.homes.farmwelt.commands.SetFarmweltCommand;
import de.frostberg.homes.farmwelt.gui.FarmweltGuiListener;
import de.frostberg.homes.gui.HomesGuiListener;
import de.frostberg.homes.lagclear.LagClearManager;
import de.frostberg.homes.lagclear.commands.LagClearCommand;
import de.frostberg.homes.listener.FarmDeathRespawnListener;
import de.frostberg.homes.listener.PlayerDataListener;
import de.frostberg.homes.manager.HomeManager;
import de.frostberg.homes.manager.TpaManager;
import de.frostberg.homes.quest.commands.QuestCommand;
import de.frostberg.homes.quest.gui.QuestGuiListener;
import de.frostberg.homes.quest.listener.QuestProgressListener;
import de.frostberg.homes.quest.manager.QuestManager;
import de.frostberg.homes.report.ReportManager;
import de.frostberg.homes.report.commands.ReportCommand;
import de.frostberg.homes.report.commands.ReportsCommand;
import de.frostberg.homes.shop.commands.ShopCommand;
import de.frostberg.homes.shop.gui.ShopGuiListener;
import de.frostberg.homes.shop.manager.ShopManager;
import de.frostberg.homes.stats.PlaytimeListener;
import de.frostberg.homes.stats.PlaytimeManager;
import de.frostberg.homes.stats.commands.SpielzeitCommand;
import de.frostberg.homes.stats.commands.StatsCommand;
import de.frostberg.homes.stats.commands.TopCommand;
import de.frostberg.homes.staff.CommandWatcherListener;
import de.frostberg.homes.staff.CommandWatcherManager;
import de.frostberg.homes.staff.VanishListener;
import de.frostberg.homes.staff.VanishManager;
import de.frostberg.homes.staff.commands.GameModeCommand;
import de.frostberg.homes.staff.commands.CommandWatcherCommand;
import de.frostberg.homes.staff.commands.VanishCommand;
import de.frostberg.homes.support.SupportListener;
import de.frostberg.homes.support.SupportManager;
import de.frostberg.homes.support.commands.SupportCommand;
import de.frostberg.homes.tokens.commands.PayCommand;
import de.frostberg.homes.util.ChatColorPlaceholderExpansion;
import de.frostberg.homes.util.ClanPlaceholderExpansion;
import de.frostberg.homes.util.CurrencyPlaceholderExpansion;
import de.frostberg.homes.util.QuestPlaceholderExpansion;
import de.frostberg.homes.util.VanishPlaceholderExpansion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class FrostbergHomes extends JavaPlugin {

    private HomeManager homeManager;
    private TpaManager tpaManager;
    private HomeCommand homeCommand;
    private HomesGuiListener homesGuiListener;
    private ClanManager clanManager;
    private ClanCommand clanCommand;
    private ClanGuiListener clanGuiListener;
    private QuestManager questManager;
    private QuestGuiListener questGuiListener;
    private ChatColorManager chatColorManager;
    private VanishManager vanishManager;
    private CommandWatcherManager commandWatcherManager;
    private ChatModeManager chatModeManager;
    private SupportManager supportManager;
    private ReportManager reportManager;
    private LagClearManager lagClearManager;
    private ArmorStandGuiListener armorStandGuiListener;
    private BankManager bankManager;
    private BankGuiListener bankGuiListener;
    private PlaytimeManager playtimeManager;
    private EnderchestManager enderchestManager;
    private EnderchestGuiListener enderchestGuiListener;
    private ShopManager shopManager;
    private ShopGuiListener shopGuiListener;
    private FarmweltGuiListener farmweltGuiListener;
    private FileConfiguration messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();

        this.homeManager = new HomeManager(this);
        this.tpaManager = new TpaManager(this);
        this.homesGuiListener = new HomesGuiListener(this);
        this.clanManager = new ClanManager(this);
        this.clanGuiListener = new ClanGuiListener(this);
        this.questGuiListener = new QuestGuiListener(this);
        // QuestManager erst NACH dem GUI-Listener bauen, da sein Konstruktor
        // bereits Reset-Checks/Broadcasts ausloesen kann, die Nachrichten
        // ueber plugin.getMessages() lesen - die steht zu diesem Zeitpunkt
        // schon bereit (siehe loadMessages() oben).
        this.questManager = new QuestManager(this);
        this.chatColorManager = new ChatColorManager(this);
        this.vanishManager = new VanishManager(this);
        this.commandWatcherManager = new CommandWatcherManager();
        this.chatModeManager = new ChatModeManager();
        this.supportManager = new SupportManager();
        this.reportManager = new ReportManager(this);
        this.lagClearManager = new LagClearManager(this);
        this.armorStandGuiListener = new ArmorStandGuiListener(this);
        this.bankManager = new BankManager(this);
        this.bankGuiListener = new BankGuiListener(this);
        this.playtimeManager = new PlaytimeManager(this);
        this.enderchestManager = new EnderchestManager(this);
        this.enderchestGuiListener = new EnderchestGuiListener(this);
        this.shopGuiListener = new ShopGuiListener(this);
        this.shopManager = new ShopManager(this);
        this.farmweltGuiListener = new FarmweltGuiListener(this);

        registerCommands();
        registerListeners();
        lagClearManager.start();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholderExpansion(this).register();
            new QuestPlaceholderExpansion(this).register();
            new ChatColorPlaceholderExpansion(this).register();
            new CurrencyPlaceholderExpansion(this).register();
            new VanishPlaceholderExpansion(this).register();
        }

        getLogger().info("FrostbergHomes wurde aktiviert.");
        logSoftDependencies();
    }

    @Override
    public void onDisable() {
        if (lagClearManager != null) {
            lagClearManager.stop();
        }
        if (homeManager != null) {
            homeManager.saveAll();
        }
        if (clanManager != null) {
            clanManager.saveAll();
        }
        if (questManager != null) {
            questManager.saveAll();
        }
        getLogger().info("FrostbergHomes wurde deaktiviert - alle Homes, Clans und Quest-Fortschritte wurden gespeichert.");
    }

    /**
     * Ueberschreibt messages.yml bei jedem Start mit der in der jar
     * mitgelieferten Version und laedt sie danach in den Speicher. Getrennt
     * von config.yml, damit alle Chat-/GUI-Texte an einer eigenen,
     * uebersichtlichen Stelle stehen (siehe MessageUtil).
     *
     * Bewusst ueberschreiben statt nur fehlende Keys zu ergaenzen: diese
     * Datei wird aktuell nicht vom Admin von Hand auf dem Server angepasst,
     * sondern nur ueber Updates dieses Plugins gepflegt. Ein reines
     * "Merge fehlender Keys" (die vorherige Variante) fing zwar neue
     * Nachrichten automatisch ein, liess aber Text-AENDERUNGEN an bereits
     * bestehenden Keys nie ankommen, ohne die Datei manuell zu loeschen.
     */
    private void loadMessages() {
        saveResource("messages.yml", true);
        File file = new File(getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    /** Laedt die messages.yml neu von der Festplatte (siehe /homes reload). */
    public void reloadMessages() {
        loadMessages();
    }

    private void registerCommands() {
        SetHomeCommand setHomeCommand = new SetHomeCommand(this);
        DeleteHomeCommand deleteHomeCommand = new DeleteHomeCommand(this);
        this.homeCommand = new HomeCommand(this);
        HomesCommand homesCommand = new HomesCommand(this);

        getCommand("set").setExecutor(setHomeCommand);
        getCommand("set").setTabCompleter(setHomeCommand);

        getCommand("delete").setExecutor(deleteHomeCommand);
        getCommand("delete").setTabCompleter(deleteHomeCommand);

        getCommand("sethome").setExecutor(new SetHomeByNameCommand(this));
        getCommand("delhome").setExecutor(new DeleteHomeByNameCommand(this));

        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);

        getCommand("homes").setExecutor(homesCommand);
        getCommand("homes").setTabCompleter(homesCommand);

        PayCommand payCommand = new PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        TpaCommand tpaCommand = new TpaCommand(this, false);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        TpaCommand tpaHereCommand = new TpaCommand(this, true);
        getCommand("tpahere").setExecutor(tpaHereCommand);
        getCommand("tpahere").setTabCompleter(tpaHereCommand);

        getCommand("tpaccept").setExecutor(new TpaAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpaDenyCommand(this));

        AdminTpCommand tpCommand = new AdminTpCommand(this, false);
        getCommand("tp").setExecutor(tpCommand);
        getCommand("tp").setTabCompleter(tpCommand);

        AdminTpCommand tpHereCommand = new AdminTpCommand(this, true);
        getCommand("tphere").setExecutor(tpHereCommand);
        getCommand("tphere").setTabCompleter(tpHereCommand);

        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));

        FarmweltCommand farmweltCommand = new FarmweltCommand(this);
        getCommand("farmwelt").setExecutor(farmweltCommand);
        getCommand("farmwelt").setTabCompleter(farmweltCommand);

        SetFarmweltCommand setFarmweltCommand = new SetFarmweltCommand(this);
        getCommand("setfarmwelt").setExecutor(setFarmweltCommand);
        getCommand("setfarmwelt").setTabCompleter(setFarmweltCommand);

        this.clanCommand = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);
        getCommand("cc").setExecutor(new ClanChatCommand(this));

        QuestCommand questCommand = new QuestCommand(this);
        getCommand("quest").setExecutor(questCommand);
        getCommand("quest").setTabCompleter(questCommand);
        getCommand("quests").setExecutor(questCommand);
        getCommand("quests").setTabCompleter(questCommand);

        ChatColorCommand chatColorCommand = new ChatColorCommand(this);
        getCommand("chatcolor").setExecutor(chatColorCommand);
        getCommand("chatcolor").setTabCompleter(chatColorCommand);

        GameModeCommand gameModeCommand = new GameModeCommand(this);
        getCommand("gm").setExecutor(gameModeCommand);
        getCommand("gm").setTabCompleter(gameModeCommand);

        VanishCommand vanishCommand = new VanishCommand(this);
        getCommand("vanish").setExecutor(vanishCommand);
        getCommand("v").setExecutor(vanishCommand);

        getCommand("cw").setExecutor(new CommandWatcherCommand(this));
        getCommand("chatclear").setExecutor(new ChatClearCommand(this));

        TeamChatCommand teamChatCommand = new TeamChatCommand(this);
        getCommand("teamchat").setExecutor(teamChatCommand);
        getCommand("tc").setExecutor(teamChatCommand);

        AdminChatCommand adminChatCommand = new AdminChatCommand(this);
        getCommand("adminchat").setExecutor(adminChatCommand);
        getCommand("ac").setExecutor(adminChatCommand);

        getCommand("support").setExecutor(new SupportCommand(this));

        ReportCommand reportCommand = new ReportCommand(this);
        getCommand("report").setExecutor(reportCommand);
        getCommand("report").setTabCompleter(reportCommand);
        getCommand("reports").setExecutor(new ReportsCommand(this));

        getCommand("laggclear").setExecutor(new LagClearCommand(this));

        getCommand("ast").setExecutor(new ArmorStandCommand(this));

        getCommand("bank").setExecutor(new BankCommand(this));

        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("spielzeit").setExecutor(new SpielzeitCommand(this));
        getCommand("top").setExecutor(new TopCommand(this));

        EnderchestCommand enderchestCommand = new EnderchestCommand(this);
        getCommand("ec").setExecutor(enderchestCommand);
        getCommand("enderchest").setExecutor(enderchestCommand);

        ShopCommand shopCommand = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);

        // HomeCommand hoert zusaetzlich auf PlayerQuitEvent, um einen laufenden
        // Warmup-Countdown beim Verlassen des Servers sauber abzubrechen
        getServer().getPluginManager().registerEvents(homeCommand, this);

        // ClanCommand hoert ebenfalls auf PlayerQuitEvent, um einen laufenden
        // Base-Warmup und offene Einladungen aufzuraeumen
        getServer().getPluginManager().registerEvents(clanCommand, this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmDeathRespawnListener(this), this);

        // Raeumt offene TPA-Anfragen und laufende Warmup-Countdowns beim Quit auf
        getServer().getPluginManager().registerEvents(tpaManager, this);

        getServer().getPluginManager().registerEvents(homesGuiListener, this);
        getServer().getPluginManager().registerEvents(clanGuiListener, this);

        // QuestManager hoert selbst auf Join/Quit (Erinnerungen, Laden/Speichern)
        getServer().getPluginManager().registerEvents(questManager, this);
        getServer().getPluginManager().registerEvents(questGuiListener, this);
        getServer().getPluginManager().registerEvents(new QuestProgressListener(this), this);

        // ChatModeListener MUSS vor ChatFormatListener registriert werden (gleiche
        // Prioritaet LOWEST, Bukkit fuehrt bei gleicher Prioritaet in Registrier-
        // reihenfolge aus) - siehe Kommentar in ChatModeListener.
        getServer().getPluginManager().registerEvents(new ChatModeListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandWatcherListener(this), this);
        getServer().getPluginManager().registerEvents(new SupportListener(this), this);
        getServer().getPluginManager().registerEvents(armorStandGuiListener, this);
        getServer().getPluginManager().registerEvents(bankGuiListener, this);
        getServer().getPluginManager().registerEvents(new PlaytimeListener(this), this);
        getServer().getPluginManager().registerEvents(enderchestGuiListener, this);
        getServer().getPluginManager().registerEvents(shopGuiListener, this);
        getServer().getPluginManager().registerEvents(farmweltGuiListener, this);
    }

    /**
     * Rein informatives Log beim Start, welche der kompatiblen Plugins
     * gefunden wurden. Keine der Abhaengigkeiten ist zwingend erforderlich.
     */
    private void logSoftDependencies() {
        PluginManager pm = getServer().getPluginManager();
        String[] softDependencies = {"LuckPerms", "Vault", "PlaceholderAPI", "PlotSquared", "Multiverse-Core", "TAB", "PlayerPoints"};

        for (String dependency : softDependencies) {
            boolean found = pm.getPlugin(dependency) != null;
            getLogger().info(dependency + ": " + (found ? "gefunden" : "nicht gefunden"));
        }
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public HomeCommand getHomeCommand() {
        return homeCommand;
    }

    public HomesGuiListener getHomesGuiListener() {
        return homesGuiListener;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ClanCommand getClanCommand() {
        return clanCommand;
    }

    public ClanGuiListener getClanGuiListener() {
        return clanGuiListener;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public QuestGuiListener getQuestGuiListener() {
        return questGuiListener;
    }

    public ChatColorManager getChatColorManager() {
        return chatColorManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public CommandWatcherManager getCommandWatcherManager() {
        return commandWatcherManager;
    }

    public ChatModeManager getChatModeManager() {
        return chatModeManager;
    }

    public SupportManager getSupportManager() {
        return supportManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public LagClearManager getLagClearManager() {
        return lagClearManager;
    }

    public ArmorStandGuiListener getArmorStandGuiListener() {
        return armorStandGuiListener;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public BankGuiListener getBankGuiListener() {
        return bankGuiListener;
    }

    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }

    public EnderchestManager getEnderchestManager() {
        return enderchestManager;
    }

    public EnderchestGuiListener getEnderchestGuiListener() {
        return enderchestGuiListener;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public ShopGuiListener getShopGuiListener() {
        return shopGuiListener;
    }

    public FarmweltGuiListener getFarmweltGuiListener() {
        return farmweltGuiListener;
    }
}
