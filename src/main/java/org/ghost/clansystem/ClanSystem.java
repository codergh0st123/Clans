package org.ghost.clansystem;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.ghost.clansystem.commands.AdminCommand;
import org.ghost.clansystem.commands.ClanCommand;
import org.ghost.clansystem.database.DatabaseManager;
import org.ghost.clansystem.gui.GuiManager;
import org.ghost.clansystem.hooks.PlaceholderHook;
import org.ghost.clansystem.listeners.ClanListener;
import org.ghost.clansystem.managers.ClanManager;
import org.ghost.clansystem.managers.PacketInterceptor;
import org.ghost.clansystem.utils.ConfigManager;

public class ClanSystem extends JavaPlugin {

    private static ClanSystem instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private ClanManager clanManager;
    private ConfigManager configManager;
    private GuiManager guiManager;
    private PacketInterceptor packetInterceptor;

    @Override
    public void onEnable() {
        instance = this;
        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib dependency not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        clanManager = new ClanManager(this);
        guiManager = new GuiManager(this);
        packetInterceptor = new PacketInterceptor(this);

        getServer().getPluginManager().registerEvents(new ClanListener(this), this);
        getCommand("clan").setExecutor(new ClanCommand(this));
        getCommand("clanadmin").setExecutor(new AdminCommand(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).unregister();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return true;
    }

    public static ClanSystem getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ClanManager getClanManager() { return clanManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public GuiManager getGuiManager() { return guiManager; }
}
