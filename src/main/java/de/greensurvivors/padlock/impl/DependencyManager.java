package de.greensurvivors.padlock.impl;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.greensurvivors.padlock.Padlock;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages how this plugin works together with other plugins
 */
public class DependencyManager {
    private final WorldGuardPlugin worldguard;
    private CoreProtectAPI coreProtectAPI = null;

    public DependencyManager(Padlock plugin) {
        // WorldGuard
        Plugin worldguardplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!plugin.getConfigManager().shouldUseWorldguard() || !(worldguardplugin instanceof WorldGuardPlugin)) {
            worldguard = null;
        } else {
            worldguard = (WorldGuardPlugin) worldguardplugin;
        }

        if (plugin.getConfigManager().shouldUseCoreprotect() && Bukkit.getPluginManager().getPlugin("CoreProtect") != null && CoreProtect.getInstance().getAPI().APIVersion() >= 6) {
            coreProtectAPI = CoreProtect.getInstance().getAPI();
            if (!coreProtectAPI.isEnabled()) {
                coreProtectAPI = null;
                plugin.getLogger().warning("CoreProtect API is not enabled!");
            }
        }
    }

    /**
     * get API version of core protect
     */
    public @Nullable String getCoreProtectAPIVersion() {
        if (coreProtectAPI != null) {
            return String.valueOf(coreProtectAPI.APIVersion());
        } else {
            return null;
        }
    }

    /**
     * get version of worldguard
     */
    public @Nullable String getWorldguardVersion() {
        if (worldguard == null) {
            return null;
        } else {
            return worldguard.getPluginMeta().getVersion();
        }
    }

    /**
     * get if worldguard would stop from placing the sign themselves (since we place it via this plugin when quick protecting)
     */
    public boolean isProtectedFrom(@NotNull Block block, @NotNull Player player) {
        if (worldguard != null) {
            return !worldguard.createProtectionQuery().testBlockPlace(player, block.getLocation(), block.getType());
        }
        return false;
    }

    /**
     * log placement of quick protected sign as if the player had placed it
     */
    public void logPlacement(@NotNull Player player, @NotNull Block block) {
        if (coreProtectAPI != null && coreProtectAPI.isEnabled()) {
            coreProtectAPI.logPlacement(player.getName(), block.getLocation(), block.getType(), block.getBlockData());
        }
    }
}
