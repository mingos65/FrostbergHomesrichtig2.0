package de.frostberg.homes;

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
import de.frostberg.homes.gui.HomesGuiListener;
import de.frostberg.homes.listener.PlayerDataListener;
import de.frostberg.homes.manager.HomeManager;
import de.frostberg.homes.manager.TpaManager;
import de.frostberg.homes.quest.commands.QuestCommand;
import de.frostberg.homes.quest.gui.QuestGuiListener;
import de.frostberg.homes.quest.listener.QuestProgressListener;
import de.frostberg.homes.quest.manager.QuestManager;
import de.frostberg.homes.tokens.commands.PayCommand;
import de.frostberg.homes.util.ClanPlaceholderExpansion;
import de.frostberg.homes.util.QuestPlaceholderExpansion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

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

        registerCommands();
        registerListeners();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholderExpansion(this).register();
            new QuestPlaceholderExpansion(this).register();
        }

        getLogger().info("FrostbergHomes wurde aktiviert.");
        logSoftDependencies();
    }

    @Override
    public void onDisable() {
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
     * Speichert die mitgelieferte messages.yml beim ersten Start (falls noch
     * nicht vorhanden), laedt sie danach in den Speicher und ergaenzt fehlende
     * Schluessel aus neueren Updates automatisch (siehe mergeMissingDefaults).
     * Getrennt von config.yml, damit alle Chat-/GUI-Texte an einer eigenen,
     * uebersichtlichen Stelle stehen (siehe MessageUtil).
     */
    private void loadMessages() {
        saveResource("messages.yml", false);
        File file = new File(getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
        mergeMissingDefaults(file);
    }

    /**
     * Ergaenzt Nachrichten-Schluessel, die in der mitgelieferten (neueren)
     * messages.yml existieren, in der bereits vorhandenen Datei auf der
     * Festplatte aber fehlen - ohne dort bestehende (ggf. angepasste) Werte
     * zu ueberschreiben. Verhindert "[Fehlende Nachricht: ...]" nach einem
     * Update, ohne dass die Datei jedes Mal manuell geloescht werden muss.
     */
    private void mergeMissingDefaults(File file) {
        try (InputStream defaultStream = getResource("messages.yml")) {
            if (defaultStream == null) {
                return;
            }

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            boolean changed = false;

            for (String key : defaults.getKeys(false)) {
                if (!messages.contains(key)) {
                    messages.set(key, defaults.get(key));
                    changed = true;
                }
            }

            if (changed) {
                messages.save(file);
                getLogger().info("messages.yml wurde um neue Standard-Texte aus diesem Update ergaenzt.");
            }
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Konnte messages.yml nicht um fehlende Standard-Texte ergaenzen.", ex);
        }
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

        getCommand("spawn").setExecutor(new SpawnCommand(this, false));
        getCommand("farmwelt").setExecutor(new SpawnCommand(this, true));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this, false));
        getCommand("setfarmwelt").setExecutor(new SetSpawnCommand(this, true));

        this.clanCommand = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);
        getCommand("cc").setExecutor(new ClanChatCommand(this));

        QuestCommand questCommand = new QuestCommand(this);
        getCommand("quest").setExecutor(questCommand);
        getCommand("quest").setTabCompleter(questCommand);
        getCommand("quests").setExecutor(questCommand);
        getCommand("quests").setTabCompleter(questCommand);

        // HomeCommand hoert zusaetzlich auf PlayerQuitEvent, um einen laufenden
        // Warmup-Countdown beim Verlassen des Servers sauber abzubrechen
        getServer().getPluginManager().registerEvents(homeCommand, this);

        // ClanCommand hoert ebenfalls auf PlayerQuitEvent, um einen laufenden
        // Base-Warmup und offene Einladungen aufzuraeumen
        getServer().getPluginManager().registerEvents(clanCommand, this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

        // Raeumt offene TPA-Anfragen und laufende Warmup-Countdowns beim Quit auf
        getServer().getPluginManager().registerEvents(tpaManager, this);

        getServer().getPluginManager().registerEvents(homesGuiListener, this);
        getServer().getPluginManager().registerEvents(clanGuiListener, this);

        // QuestManager hoert selbst auf Join/Quit (Erinnerungen, Laden/Speichern)
        getServer().getPluginManager().registerEvents(questManager, this);
        getServer().getPluginManager().registerEvents(questGuiListener, this);
        getServer().getPluginManager().registerEvents(new QuestProgressListener(this), this);
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
}
