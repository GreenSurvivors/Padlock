package de.greensurvivors.padlock.impl;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.greensurvivors.padlock.Padlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages how this plugin works together with other plugins
 */
public class DependencyManager {
    private final @Nullable WorldGuardPlugin worldguard;

    public DependencyManager(Padlock plugin) {
        // WorldGuard
        Plugin worldguardplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!plugin.getConfigManager().shouldUseWorldguard() || !(worldguardplugin instanceof WorldGuardPlugin)) {
            worldguard = null;
        } else {
            worldguard = (WorldGuardPlugin) worldguardplugin;
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
     * get if worldguard would stop from breaking the block
     */
    public boolean isProtectedFromBreak(@NotNull Block block, @NotNull Player player) {
        if (worldguard != null) {
            return !worldguard.createProtectionQuery().testBlockBreak(player, block);
        }
        return false;
    }
}
