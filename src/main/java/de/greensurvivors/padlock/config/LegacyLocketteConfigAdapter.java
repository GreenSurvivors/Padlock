package de.greensurvivors.padlock.config;

import de.greensurvivors.padlock.impl.MiscUtils;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads LockettePro config.
 * for whomever and why ever to update this adapter:
 * Please be aware that this class may look a lot like the config class in LockettePro and this indeed intentional.
 * However, please be aware that there were made changes, like using Enums or giving the cache time in seconds back.
 */
@Deprecated(forRemoval = true)
class LegacyLocketteConfigAdapter {
    private boolean worldguard = false;
    private boolean coreprotect = false;
    private Set<Material> lockables = new HashSet<>();
    private ConfigManager.QuickProtectOption enablequickprotect = null;
    private boolean blockinterfereplacement = true;
    private boolean blockitemtransferin = false;
    private boolean blockitemtransferout = false;
    private int cachetime = 0;
    private boolean cacheenabled = false;
    private ConfigManager.HopperMinecartMoveItemOption blockhopperminecart = null;
    private double lockexpiredays = 60D;
    private long lockdefaultcreatetime = -1L;
    private Set<ConfigManager.ProtectionExemption> protectionexempt = new HashSet<>();

    protected LegacyLocketteConfigAdapter() {
    }

    protected void reload(@NotNull Plugin plugin) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/LockettePro/config.yml"));

        worldguard = config.getBoolean("worldguard", true);
        coreprotect = config.getBoolean("coreprotect", true);
        String enablequickprotectstring = config.getString("enable-quick-protect", "true");

        switch (enablequickprotectstring.toLowerCase()) {
            case "false" -> enablequickprotect = ConfigManager.QuickProtectOption.NO_QUICKLOCK;
            case "sneak" -> enablequickprotect = ConfigManager.QuickProtectOption.SNEAK_REQUIRED;
            default -> enablequickprotect = ConfigManager.QuickProtectOption.NOT_SNEAKING_REQUIRED;
        }
        blockinterfereplacement = config.getBoolean("block-interfere-placement", true);
        blockitemtransferin = config.getBoolean("block-item-transfer-in", false);
        blockitemtransferout = config.getBoolean("block-item-transfer-out", true);

        // load Material set of lockable blocks
        List<String> stringList = config.getStringList("protection-exempt");
        protectionexempt = new HashSet<>();
        for (String string : stringList) {
            ConfigManager.ProtectionExemption protectionExemtion = MiscUtils.getEnum(ConfigManager.ProtectionExemption.class, string);

            if (protectionExemtion != null) {
                protectionexempt.add(protectionExemtion);
            } else {
                plugin.getLogger().warning("Couldn't get exemtion from legacy \"" + string + "\" for lock exemtion list. Ignoring.");
            }
        }

        cachetime = config.getInt("cache-time-seconds", 0);
        cacheenabled = (config.getInt("cache-time-seconds", 0) > 0);
        if (cacheenabled) {
            plugin.getLogger().info("Cache is enabled! In case of inconsistency, turn off immediately.");
        }

        String blockhopperminecartstring = config.getString("block-hopper-minecart", "remove");
        switch (blockhopperminecartstring.toLowerCase()) {
            case "true" -> blockhopperminecart = ConfigManager.HopperMinecartMoveItemOption.BLOCKED;
            case "false" -> blockhopperminecart = ConfigManager.HopperMinecartMoveItemOption.ALLOWED;
            default -> blockhopperminecart = ConfigManager.HopperMinecartMoveItemOption.REMOVE;
        }

        lockexpiredays = config.getDouble("lock-expire-days", 999.9D);
        lockdefaultcreatetime = config.getLong("lock-default-create-time-unix", -1L);
        if (lockdefaultcreatetime < -1L) lockdefaultcreatetime = -1L;
        List<String> unprocesseditems = config.getStringList("lockables");
        lockables = new HashSet<>();
        for (String unprocesseditem : unprocesseditems) {
            if (unprocesseditem.equals("*")) {
                Collections.addAll(lockables, Material.values());
                plugin.getLogger().info("All blocks are default to be lockable!");
                plugin.getLogger().info("Add '-<Material>' to exempt a block, such as '-STONE'!");
                continue;
            }
            boolean add = true;
            if (unprocesseditem.startsWith("-")) {
                add = false;
                unprocesseditem = unprocesseditem.substring(1);
            }
            Material material = Material.getMaterial(unprocesseditem);
            if (material == null || !material.isBlock()) {
                plugin.getLogger().warning(unprocesseditem + " is not a block!");
            } else {
                if (add) {
                    lockables.add(material);
                } else {
                    lockables.remove(material);
                }
            }
        }
        lockables.removeAll(Tag.SIGNS.getValues());
        lockables.remove(Material.SCAFFOLDING);
    }

    protected ConfigManager.QuickProtectOption getQuickProtectAction() {
        return enablequickprotect;
    }

    protected boolean isInterferePlacementBlocked() {
        return blockinterfereplacement;
    }

    protected boolean isItemTransferInBlocked() {
        return blockitemtransferin;
    }

    protected boolean isItemTransferOutBlocked() {
        return blockitemtransferout;
    }

    protected ConfigManager.HopperMinecartMoveItemOption getHopperMinecartAction() {
        return blockhopperminecart;
    }

    protected Double getLockExpireDays() {
        return lockexpiredays;
    }

    protected long getLockDefaultCreateTimeUnix() {
        return lockdefaultcreatetime;
    }

    protected int getCacheTimeSeconds() {
        return cachetime;
    }

    protected boolean isCacheEnabled() {
        return cacheenabled;
    }

    protected boolean workWithWorldguard() {
        return worldguard;
    }

    protected boolean workWithCoreprotect() {
        return coreprotect;
    }

    protected Set<ConfigManager.ProtectionExemption> getProtectionExemptions() {
        return protectionexempt;
    }

    protected Set<Material> getLockables() {
        return lockables;
    }
}