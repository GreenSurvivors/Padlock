package de.greensurvivors.padlock.impl;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.greensurvivors.padlock.Padlock;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.bukkit.Bukkit.getServer;

/**
 * Manages how this plugin works together with other plugins
 */
public class DependencyManager {
    private final @Nullable WorldGuardPlugin worldguard;
    private final @Nullable Permission perms;
    private final @Nullable CoreProtectAPI coreProtectAPI;

    public DependencyManager(Padlock plugin) {
        // WorldGuard
        Plugin worldguardplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!plugin.getConfigManager().shouldUseWorldguard() || !(worldguardplugin instanceof WorldGuardPlugin)) {
            worldguard = null;
        } else {
            worldguard = (WorldGuardPlugin) worldguardplugin;
        }

        // Core protect
        if (plugin.getConfigManager().shouldUseCoreprotect() && Bukkit.getPluginManager().getPlugin("CoreProtect") != null && CoreProtect.getInstance().getAPI().APIVersion() >= 6) {
            if (CoreProtect.getInstance().getAPI().isEnabled()) {
                coreProtectAPI = CoreProtect.getInstance().getAPI();
            } else {
                coreProtectAPI = null;
                plugin.getLogger().warning("CoreProtect API is not enabled!");
            }
        } else {
            coreProtectAPI = null;
        }

        // Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);

            if (rsp != null) {
                perms = rsp.getProvider();
            } else {
                perms = null;
            }
        } else {
            perms = null;
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
     * get if permission VaultAPI is useable
     */
    public boolean isHookedIntoVault() {
        return perms != null;
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

    /**
     * @param world
     * @param offlinePlayer
     * @param permission
     * @return false if Vault was not installed.
     */
    public boolean getOfflinePermission(World world, OfflinePlayer offlinePlayer, org.bukkit.permissions.Permission permission) {
        if (perms != null) {
            return perms.playerHas(world.getName(), offlinePlayer, permission.getName());
        } else {
            return false;
        }
    }
}
