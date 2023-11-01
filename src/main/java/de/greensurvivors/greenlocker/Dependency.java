package de.greensurvivors.greenlocker;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Dependency {
    protected static WorldGuardPlugin worldguard = null;
    private static CoreProtectAPI coreProtectAPI; //todo
    private static GreenLocker plugin;

    public static void setPluginAndLoad(GreenLocker plugin) {
        Dependency.plugin = plugin;
        // WorldGuard
        Plugin worldguardplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!plugin.getConfigManager().getWorldguard() || !(worldguardplugin instanceof WorldGuardPlugin)) {
            worldguard = null;
        } else {
            worldguard = (WorldGuardPlugin) worldguardplugin;
        }

        if (plugin.getConfigManager().getCoreprotect() && Bukkit.getPluginManager().getPlugin("CoreProtect") != null && CoreProtect.getInstance().getAPI().APIVersion() == 6) {
            coreProtectAPI = CoreProtect.getInstance().getAPI();
            if (!coreProtectAPI.isEnabled()) {
                coreProtectAPI = null;
                plugin.getLogger().warning("CoreProtect API is not enabled!");
            }
        }
    }

    public static WorldGuardPlugin getWorldguard() {
        return worldguard;
    }

    public static boolean isProtectedFrom(Block block, Player player) {
        if (worldguard != null) {
            return !worldguard.createProtectionQuery().testBlockPlace(player, block.getLocation(), block.getType());
        }
        return false;
    }


    public static void logPlacement(Player player, Block block) {
        if (coreProtectAPI != null && coreProtectAPI.isEnabled()) {
            coreProtectAPI.logPlacement(player.getName(), block.getLocation(), block.getType(), block.getBlockData());
        }
    }
}
