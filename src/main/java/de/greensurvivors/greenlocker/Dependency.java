package de.greensurvivors.greenlocker;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Dependency {
    protected static WorldGuardPlugin worldguard = null;
    private static CoreProtectAPI coreProtectAPI = null;

    public static void setPluginAndLoad(GreenLocker plugin) {
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

    public static @Nullable String getCoreProtectAPIVersion() {
        if (coreProtectAPI != null) {
            return String.valueOf(coreProtectAPI.APIVersion());
        } else {
            return null;
        }
    }

    public static @Nullable WorldGuardPlugin getWorldguard() {
        return worldguard;
    }

    public static boolean isProtectedFrom(@NotNull Block block, @NotNull Player player) {
        if (worldguard != null) {
            return !worldguard.createProtectionQuery().testBlockPlace(player, block.getLocation(), block.getType());
        }
        return false;
    }

    public static void logPlacement(@NotNull Player player, @NotNull Block block) {
        if (coreProtectAPI != null && coreProtectAPI.isEnabled()) {
            coreProtectAPI.logPlacement(player.getName(), block.getLocation(), block.getType(), block.getBlockData());
        }
    }
}
