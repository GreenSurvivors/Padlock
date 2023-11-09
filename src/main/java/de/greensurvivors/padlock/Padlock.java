package de.greensurvivors.padlock;

import de.greensurvivors.padlock.command.Command;
import de.greensurvivors.padlock.config.ConfigManager;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.openabledata.OpenableToggleManager;
import de.greensurvivors.padlock.listener.BlockDebugListener;
import de.greensurvivors.padlock.listener.BlockEnvironmentListener;
import de.greensurvivors.padlock.listener.BlockInventoryMoveListener;
import de.greensurvivors.padlock.listener.BlockPlayerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class Padlock extends JavaPlugin {
    private static Padlock plugin;
    private final boolean debug = false;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private OpenableToggleManager openableToggleManager;

    public static Padlock getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Read config
        messageManager = new MessageManager(this);
        configManager = new ConfigManager(this);
        configManager.reload();

        openableToggleManager = new OpenableToggleManager(this);

        // Register Listeners
        // If debug mode is not on, debug listener won't register
        if (debug) getServer().getPluginManager().registerEvents(new BlockDebugListener(), this);
        getServer().getPluginManager().registerEvents(new BlockPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockEnvironmentListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockInventoryMoveListener(this), this);

        //register command
        for (String commandStr : this.getDescription().getCommands().keySet()) {
            PluginCommand mainCommand = getCommand(commandStr);
            if (mainCommand != null) {
                Command lockCmd = new Command(this);

                mainCommand.setExecutor(lockCmd);
                mainCommand.setTabCompleter(lockCmd);
            } else {
                getLogger().log(Level.SEVERE, "Couldn't register command '" + commandStr + "'!");
            }
        }

        // Dependency
        Dependency.setPluginAndLoad(this);
    }

    @Override
    public void onDisable() {
        openableToggleManager.cancelAllTasks();
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public OpenableToggleManager getOpenableToggleManager() {
        return openableToggleManager;
    }
}
