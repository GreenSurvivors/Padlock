package me.crafter.mc.lockettepro;

import me.crafter.mc.lockettepro.command.Command;
import me.crafter.mc.lockettepro.config.Config;
import me.crafter.mc.lockettepro.dependency.Dependency;
import me.crafter.mc.lockettepro.listener.BlockDebugListener;
import me.crafter.mc.lockettepro.listener.BlockEnvironmentListener;
import me.crafter.mc.lockettepro.listener.BlockInventoryMoveListener;
import me.crafter.mc.lockettepro.listener.BlockPlayerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class LockettePro extends JavaPlugin {
    private static final boolean needcheckhand = true;
    private static Plugin plugin;
    private final boolean debug = false;

    public static Plugin getPlugin() {
        return plugin;
    }

    public static boolean needCheckHand() {
        return needcheckhand;
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Read config
        new Config(this);

        // Register Listeners
        // If debug mode is not on, debug listener won't register
        if (debug) getServer().getPluginManager().registerEvents(new BlockDebugListener(), this);
        getServer().getPluginManager().registerEvents(new BlockPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockEnvironmentListener(), this);
        getServer().getPluginManager().registerEvents(new BlockInventoryMoveListener(), this);


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
        new Dependency(this);
    }

    @Override
    public void onDisable() {
    }
}
