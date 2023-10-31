package me.crafter.mc.lockettepro.config;

import me.crafter.mc.lockettepro.LockettePro;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

//todo @Contract("_, !null -> !null")
public class ConfigManager { //todo
    private boolean worldguard = false;
    private boolean coreprotect = false;
    private final LockettePro plugin;
    private FileConfiguration config;
    private Set<Material> lockables = new HashSet<>();
    private byte enablequickprotect = 1;
    private boolean blockinterfereplacement = true;
    private boolean blockitemtransferin = false;
    private boolean blockitemtransferout = false;
    private int cachetime = 0;
    private boolean cacheenabled = false;
    private byte blockhopperminecart = 0;
    private boolean lockexpire = false;
    private double lockexpiredays = 60D;
    private long lockdefaultcreatetime = -1L;
    private Set<String> protectionexempt = new HashSet<>();

    public ConfigManager(LockettePro plugin) {
        this.plugin = plugin;
    }

    public void reload() {//todo
        initDefaultConfig();
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));

        //reload Language files
        plugin.getMessageManager().setLangFileName(config.getString("language-file-name", "lang_en.yml"));
        plugin.getMessageManager().reload();

        worldguard = config.getBoolean("worldguard", true);
        coreprotect = config.getBoolean("coreprotect", true);
        String enablequickprotectstring = config.getString("enable-quick-protect", "true");

        switch (enablequickprotectstring.toLowerCase()) {
            case "true" -> enablequickprotect = 1;
            case "false" -> enablequickprotect = 0;
            case "sneak" -> enablequickprotect = 2;
            default -> enablequickprotect = 1;
        }
        blockinterfereplacement = config.getBoolean("block-interfere-placement", true);
        blockitemtransferin = config.getBoolean("block-item-transfer-in", false);
        blockitemtransferout = config.getBoolean("block-item-transfer-out", true);

        cachetime = config.getInt("cache-time-seconds", 0) * 1000;
        cacheenabled = (config.getInt("cache-time-seconds", 0) > 0);
        if (cacheenabled) {
            plugin.getLogger().info("Cache is enabled! In case of inconsistency, turn off immediately.");
        }

        String blockhopperminecartstring = config.getString("block-hopper-minecart", "remove");
        switch (blockhopperminecartstring.toLowerCase()) {
            case "true" -> blockhopperminecart = 1;
            case "false" -> blockhopperminecart = 0;
            case "remove" -> blockhopperminecart = 2;
            default -> blockhopperminecart = 2;
        }

        lockexpire = config.getBoolean("lock-expire", false);
        lockexpiredays = config.getDouble("lock-expire-days", 999.9D);
        lockdefaultcreatetime = config.getLong("lock-default-create-time-unix", -1L);
        if (lockdefaultcreatetime < -1L) lockdefaultcreatetime = -1L;
        List<String> unprocesseditems = config.getStringList("lockables");
        lockables = new HashSet<Material>();
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

    private void initDefaultConfig() {//todo
        plugin.saveDefaultConfig();
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        config.addDefault("language-file-name", "lang.yml");
        config.addDefault("enable-quick-protect", true);
        config.addDefault("block-interfere-placement", true);
        config.addDefault("block-item-transfer-in", false);
        config.addDefault("block-item-transfer-out", true);
        config.addDefault("block-hopper-minecart", "remove");
        config.addDefault("cache-time-seconds", 0);

        List<String> protectionexemptstringlist = config.getStringList("protection-exempt");
        protectionexempt = new HashSet<>(protectionexemptstringlist);

        List<String> lockablesList = new ArrayList<>();
        Tag.DOORS.getValues().stream().map(Material::name).forEach(lockablesList::add); //todo auto (trap)doors, inventory-blocks, Netherrite, ..?
        lockablesList.addAll(List.of("CHEST", "TRAPPED_CHEST", "FURNACE", "BURNING_FURNACE", "HOPPER", "BREWING_STAND", "DIAMOND_BLOCK", "LECTERN"));
        String[] lockables = lockablesList.toArray(String[]::new);
        config.addDefault("lockables", lockables);
        String[] protection_exempt = {"nothing"};
        config.addDefault("protection-exempt", protection_exempt);
        config.addDefault("lock-expire", false);
        config.addDefault("lock-expire-days", 999.9D);
        config.addDefault("lock-default-create-time-unix", -1L);
        config.addDefault("lock-expire-string", "&3[Expired]");

        config.options().copyDefaults(true);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "couldn't save config.yml", e);
        }
    }

    public byte getQuickProtectAction() {
        return enablequickprotect;
    }

    public boolean isInterferePlacementBlocked() {
        return blockinterfereplacement;
    }

    public boolean isItemTransferInBlocked() {
        return blockitemtransferin;
    }

    public boolean isItemTransferOutBlocked() {
        return blockitemtransferout;
    }

    public byte getHopperMinecartAction() {
        return blockhopperminecart;
    }

    public boolean isLockExpire() {
        return lockexpire;
    }

    public Double getLockExpireDays() {
        return lockexpiredays;
    }

    public long getLockDefaultCreateTimeUnix() {
        return lockdefaultcreatetime;
    }

    public boolean isLockable(Material material) {
        return lockables.contains(material);
    }

    public int getCacheTimeMillis() {
        return cachetime;
    }

    public boolean isCacheEnabled() {
        return cacheenabled;
    }

    public boolean isProtectionExempted(String against) {
        return protectionexempt.contains(against);
    }

    public boolean getWorldguard() {
        return worldguard;
    }

    public boolean getCoreprotect() {
        return coreprotect;
    }
}
