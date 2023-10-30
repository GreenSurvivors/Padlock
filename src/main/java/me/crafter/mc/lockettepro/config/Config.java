package me.crafter.mc.lockettepro.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

//todo @Contract("_, !null -> !null")
public class Config { //todo
    public static boolean worldguard = false;
    public static boolean coreprotect = false;
    private static Plugin plugin;
    private static FileConfiguration config;
    private static FileConfiguration lang;
    private static String langfilename = "lang.yml";
    private static String invalidString = "[Invalid]"; //todo^2
    private static Set<Material> lockables = new HashSet<>();
    private static String privatestring = "[Private]";
    private static Set<String> additionalstrings = new HashSet<>();
    private static Set<String> everyonestrings = new HashSet<>();
    private static Set<String> timerstrings = new HashSet<>();
    private static String defaultadditionalstring = "[More Users]";
    private static byte enablequickprotect = (byte) 1;
    private static boolean blockinterfereplacement = true;
    private static boolean blockitemtransferin = false;
    private static boolean blockitemtransferout = false;
    private static int cachetime = 0;
    private static boolean cacheenabled = false;
    private static byte blockhopperminecart = 0;
    private static boolean lockexpire = false;
    private static double lockexpiredays = 60D;
    private static long lockdefaultcreatetime = -1L;
    private static String lockexpirestring = "";
    private static Set<String> protectionexempt = new HashSet<>();

    public Config(Plugin _plugin) {
        plugin = _plugin;
        reload();
    }

    public static void reload() {
        initDefaultConfig();
        initAdditionalFiles();
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        worldguard = config.getBoolean("worldguard", true);
        coreprotect = config.getBoolean("coreprotect", true);
        langfilename = config.getString("language-file-name", "lang.yml");
        lang = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), langfilename));
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

        List<String> additionalstringlist = config.getStringList("additional-signs");
        List<String> everyonestringlist = config.getStringList("everyone-signs");
        List<String> protectionexemptstringlist = config.getStringList("protection-exempt");
        privatestring = config.getString("private-sign");
        additionalstrings = new HashSet<>(additionalstringlist);
        everyonestrings = new HashSet<>(everyonestringlist);
        protectionexempt = new HashSet<>(protectionexemptstringlist);

        List<String> timerstringlist = config.getStringList("timer-signs");
        List<String> timerstringlist2 = new ArrayList<>();
        for (String timerstring : timerstringlist) {
            if (timerstring.contains("@")) timerstringlist2.add(timerstring);
        }
        timerstrings = new HashSet<>(timerstringlist2);

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
        lockexpirestring = ChatColor.translateAlternateColorCodes('&',
                config.getString("lock-expire-string", "&3[Expired]"));
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

    public static void initDefaultConfig() {
        plugin.saveDefaultConfig();
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        config.addDefault("language-file-name", "lang.yml");
        config.addDefault("enable-quick-protect", true);
        config.addDefault("block-interfere-placement", true);
        config.addDefault("block-item-transfer-in", false);
        config.addDefault("block-item-transfer-out", true);
        config.addDefault("block-hopper-minecart", "remove");
        config.addDefault("cache-time-seconds", 0);

        String[] private_signs = {"[Private]", "[private]"};
        config.addDefault("private-signs", private_signs);
        String[] additional_signs = {"[More Users]", "[more Users]"};
        config.addDefault("additional-signs", additional_signs);
        String[] everyone_signs = {"[Everyone]", "[everyone]"};
        config.addDefault("everyone-signs", everyone_signs);
        String[] timer_signs = {"[Timer:@]", "[timer:@]"};
        config.addDefault("timer-signs", timer_signs);
        List<String> lockablesList = new ArrayList<>();
        Tag.DOORS.getValues().stream().map(Material::name).forEach(lockablesList::add);
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

    public static void initAdditionalFiles() {
        String[] availablefiles = {"lang.yml", "lang_zh-cn.yml", "lang_es.yml", "lang_it.yml"};
        for (String filename : availablefiles) {
            File langfile = new File(plugin.getDataFolder(), filename);
            if (!langfile.exists()) {
                plugin.saveResource(filename, false);
            }
        }
    }

    public static byte getQuickProtectAction() {
        return enablequickprotect;
    }

    public static boolean isInterferePlacementBlocked() {
        return blockinterfereplacement;
    }

    public static boolean isItemTransferInBlocked() {
        return blockitemtransferin;
    }

    public static boolean isItemTransferOutBlocked() {
        return blockitemtransferout;
    }

    public static byte getHopperMinecartAction() {
        return blockhopperminecart;
    }

    public static boolean isLockExpire() {
        return lockexpire;
    }

    public static Double getLockExpireDays() {
        return lockexpiredays;
    }

    public static long getLockDefaultCreateTimeUnix() {
        return lockdefaultcreatetime;
    }

    public static String getLockExpireString() {
        return lockexpirestring;
    }

    public static @NotNull Component getCmdHelp(String subCommand) {
        return getLangComp("cmd.help." + subCommand);
    }

    public static @NotNull Component getLangComp(@NotNull String path) { // todo lang in minimessage format
        return MiniMessage.miniMessage().deserialize(lang.getString(path, ""));
    }

    public static boolean isLockable(Material material) {
        return lockables.contains(material);
    }

    public static Component getInvalidString() {
        return MiniMessage.miniMessage().deserialize(invalidString);
    }

    public static boolean isPrivateSignComp(Component message) { //todo ignore casing
        return MiniMessage.miniMessage().deserialize(privatestring).contains(message);
    }

    @Deprecated(forRemoval = true)
    public static boolean isPrivateSignString(String message) {
        return privatestring.contains(message);
    }

    public static boolean isAdditionalSignString(String message) {
        return additionalstrings.contains(message);
    }

    public static boolean isEveryoneSignString(String message) {
        return everyonestrings.contains(message);
    }

    public static boolean isTimerSignString(String message) {
        for (String timerstring : timerstrings) {
            String[] splitted = timerstring.split("@", 2);
            if (message.startsWith(splitted[0]) && message.endsWith(splitted[1])) {
                return true;
            }
        }
        return false;
    }

    public static int getTimer(String message) {
        for (String timerstring : timerstrings) {
            String[] splitted = timerstring.split("@", 2);
            if (message.startsWith(splitted[0]) && message.endsWith(splitted[1])) {
                String newmessage = message.replace(splitted[0], "").replace(splitted[1], "");
                try {
                    int seconds = Integer.parseInt(newmessage);
                    return Math.min(seconds, 20);
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    public static String getPrivateString() {
        return privatestring;
    }

    public static String getDefaultAdditionalString() {
        return defaultadditionalstring;
    }

    public static int getCacheTimeMillis() {
        return cachetime;
    }

    public static boolean isCacheEnabled() {
        return cacheenabled;
    }

    public static boolean isProtectionExempted(String against) {
        return protectionexempt.contains(against);
    }
}
