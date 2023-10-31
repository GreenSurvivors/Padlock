package me.crafter.mc.lockettepro;

import me.crafter.mc.lockettepro.command.Command;
import me.crafter.mc.lockettepro.config.ConfigManager;
import me.crafter.mc.lockettepro.config.MessageManager;
import me.crafter.mc.lockettepro.listener.BlockDebugListener;
import me.crafter.mc.lockettepro.listener.BlockEnvironmentListener;
import me.crafter.mc.lockettepro.listener.BlockInventoryMoveListener;
import me.crafter.mc.lockettepro.listener.BlockPlayerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class LockettePro extends JavaPlugin {
    private static final boolean needcheckhand = true;
    private static LockettePro plugin;
    private final boolean debug = false;
    private ConfigManager configManager;
    private MessageManager messageManager;

    public static LockettePro getPlugin() {
        return plugin;
    }

    public static boolean needCheckHand() {
        return needcheckhand;
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Read config
        messageManager = new MessageManager(this);
        configManager = new ConfigManager(this);
        configManager.reload();

        // Register Listeners
        // If debug mode is not on, debug listener won't register
        if (debug) getServer().getPluginManager().registerEvents(new BlockDebugListener(), this);
        getServer().getPluginManager().registerEvents(new BlockPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockEnvironmentListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockInventoryMoveListener(this), this);


        this.getCommand("");

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
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
