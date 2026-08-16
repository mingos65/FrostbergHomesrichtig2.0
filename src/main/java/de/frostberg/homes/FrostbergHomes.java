package de.frostberg.homes;

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
import de.frostberg.homes.tokens.commands.PayCommand;
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
    private FileConfiguration messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();

        this.homeManager = new HomeManager(this);
        this.tpaManager = new TpaManager(this);
        this.homesGuiListener = new HomesGuiListener(this);

        registerCommands();
        registerListeners();

        getLogger().info("FrostbergHomes wurde aktiviert.");
        logSoftDependencies();
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            homeManager.saveAll();
        }
        getLogger().info("FrostbergHomes wurde deaktiviert - alle Homes wurden gespeichert.");
    }

    /**
     * Speichert die mitgelieferte messages.yml beim ersten Start (falls noch
     * nicht vorhanden) und laedt sie danach in den Speicher. Getrennt von
     * config.yml, damit alle Chat-/GUI-Texte an einer eigenen, uebersichtlichen
     * Stelle stehen (siehe MessageUtil).
     */
    private void loadMessages() {
        saveResource("messages.yml", false);
        this.messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
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

        // HomeCommand hoert zusaetzlich auf PlayerQuitEvent, um einen laufenden
        // Warmup-Countdown beim Verlassen des Servers sauber abzubrechen
        getServer().getPluginManager().registerEvents(homeCommand, this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

        // Raeumt offene TPA-Anfragen und laufende Warmup-Countdowns beim Quit auf
        getServer().getPluginManager().registerEvents(tpaManager, this);

        getServer().getPluginManager().registerEvents(homesGuiListener, this);
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
}
