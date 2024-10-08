package de.greensurvivors.padlock;

import de.greensurvivors.padlock.command.ApplyPassword;
import de.greensurvivors.padlock.command.MainCommand;
import de.greensurvivors.padlock.config.ConfigManager;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.DependencyManager;
import de.greensurvivors.padlock.impl.LockCacheManager;
import de.greensurvivors.padlock.impl.openabledata.OpenableToggleManager;
import de.greensurvivors.padlock.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class Padlock extends JavaPlugin {
    private static Padlock plugin;
    /**
     * only for big problems or plugin development
     */
    private final boolean debug = false; // todo make this a setting in the config.
    private ConfigManager configManager;
    private MessageManager messageManager;
    private OpenableToggleManager openableToggleManager;
    private LockCacheManager lockCacheManager;
    private DependencyManager dependencyManager;

    public static Padlock getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        // don't allow LockettePro to run parallel. We provide everything it does.
        final @Nullable Plugin lockettePro = Bukkit.getPluginManager().getPlugin("LockettePro");
        if (lockettePro != null) {
            plugin.getLogger().warning("I found LockettePro and disabled it. Please remove LockettePro from your Plugin list!");

            // unregister lockette cmds
            Bukkit.getCommandMap().getKnownCommands().entrySet().removeIf(
                entry -> entry.getValue() instanceof PluginCommand cmd && cmd.getPlugin() == lockettePro);

            Bukkit.getPluginManager().disablePlugin(lockettePro);
        }

        openableToggleManager = new OpenableToggleManager(this);
        lockCacheManager = new LockCacheManager();

        // Read config
        messageManager = new MessageManager(this);
        configManager = new ConfigManager(this);
        configManager.reload();

        // Register Listeners
        // If debug mode is not on, debug listener won't register
        PluginManager pluginManager = getServer().getPluginManager();
        if (debug) {
            pluginManager.registerEvents(new BlockDebugListener(), this);
        }
        pluginManager.registerEvents(new BlockPlayerListener(this), this);
        pluginManager.registerEvents(new BlockEnvironmentListener(this), this);
        pluginManager.registerEvents(new BlockInventoryMoveListener(this), this);
        pluginManager.registerEvents(new ChatPlayerListener(this), this);

        //register commands
        MainCommand lockCmd = new MainCommand(this);

        PluginCommand mainCommand = getCommand("padlock");
        if (mainCommand != null) {

            mainCommand.setExecutor(lockCmd);
            mainCommand.setTabCompleter(lockCmd);
        } else {
            getLogger().log(Level.SEVERE, "Couldn't register command 'padlock'!");
        }

        ApplyPassword pwCmd = new ApplyPassword(plugin);
        PluginCommand pwCommand = getCommand("password");
        if (pwCommand != null) {

            pwCommand.setExecutor(pwCmd);
            pwCommand.setTabCompleter(pwCmd);
        } else {
            getLogger().log(Level.SEVERE, "Couldn't register command 'password'!");
        }


        // Dependencys
        dependencyManager = new DependencyManager(this);
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

    public LockCacheManager getLockCacheManager() {
        return lockCacheManager;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }
}
