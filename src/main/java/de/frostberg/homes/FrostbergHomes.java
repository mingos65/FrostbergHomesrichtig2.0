package de.frostberg.homes;

import de.frostberg.homes.commands.DeleteHomeCommand;
import de.frostberg.homes.commands.HomeCommand;
import de.frostberg.homes.commands.HomesCommand;
import de.frostberg.homes.commands.SetHomeCommand;
import de.frostberg.homes.listener.PlayerDataListener;
import de.frostberg.homes.manager.HomeManager;
import de.frostberg.homes.tokens.commands.PayCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FrostbergHomes extends JavaPlugin {

    private HomeManager homeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.homeManager = new HomeManager(this);

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

    private void registerCommands() {
        SetHomeCommand setHomeCommand = new SetHomeCommand(this);
        DeleteHomeCommand deleteHomeCommand = new DeleteHomeCommand(this);
        HomeCommand homeCommand = new HomeCommand(this);
        HomesCommand homesCommand = new HomesCommand(this);

        getCommand("set").setExecutor(setHomeCommand);
        getCommand("set").setTabCompleter(setHomeCommand);

        getCommand("delete").setExecutor(deleteHomeCommand);
        getCommand("delete").setTabCompleter(deleteHomeCommand);

        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);

        getCommand("homes").setExecutor(homesCommand);
        getCommand("homes").setTabCompleter(homesCommand);

        PayCommand payCommand = new PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        // HomeCommand hoert zusaetzlich auf PlayerQuitEvent, um einen laufenden
        // Warmup-Countdown beim Verlassen des Servers sauber abzubrechen
        getServer().getPluginManager().registerEvents(homeCommand, this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
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
}
