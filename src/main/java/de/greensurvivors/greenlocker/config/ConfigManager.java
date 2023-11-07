package de.greensurvivors.greenlocker.config;

import de.greensurvivors.greenlocker.GreenLocker;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConfigManager {
    private final GreenLocker plugin;

    // please note: While fallback values are defined here, these are in fact NOT the default options. They are just used in the unfortunate case loading them goes wrong.
    // if you want to change default options, have also a look into resources/config.yaml
    private final ConfigOption<Boolean> IMPORT_FROM_LOCKETTEPRO = new ConfigOption<>("import-fromLockettePro", false);
    private final ConfigOption<String> LANG_FILENAME = new ConfigOption<>("language-file-name", "lang/lang_en.yml");
    private final ConfigOption<Boolean> DEPENDENCY_WORLDGUARD_ENABLED = new ConfigOption<>("dependency.worldguard.enabled", false);
    private final ConfigOption<Boolean> DEPENDENCY_COREPROTECT_ENABLED = new ConfigOption<>("dependency.coreprotect.enabled", false);
    private final ConfigOption<Set<Material>> LOCKABLES = new ConfigOption<>("lockables", setUpLockableDefaults());
    private final ConfigOption<QuickProtectOption> QUICKPROTECT_TYPE = new ConfigOption<>("lock.quick-lock.type", QuickProtectOption.NOT_SNEAKING_REQUIRED);
    private final ConfigOption<Boolean> LOCK_BLOCKS_INTERFERE = new ConfigOption<>("lock.blocked.interfere", true);
    private final ConfigOption<Boolean> LOCK_BLOCKS_ITEM_TRANSFER_IN = new ConfigOption<>("lock.blocked.item-transfer.in", false);
    private final ConfigOption<Boolean> LOCK_BLOCKS_ITEM_TRANSFER_OUT = new ConfigOption<>("lock.blocked.item-transfer.out", true);
    private final ConfigOption<HopperMinecartBlockedOption> LOCK_BLOCKS_HOPPER_MINECART = new ConfigOption<>("lock.blocked.hopper-minecart", HopperMinecartBlockedOption.REMOVE);
    private final ConfigOption<Set<ProtectionExemption>> LOCK_EXEMPTIONS = new ConfigOption<>("lock.exemptions", Set.of());
    private final ConfigOption<Long> LOCK_EXPIRE_DAYS = new ConfigOption<>("lock.expire.days", 999L);
    //while this works intern with milliseconds, configurable are only seconds for easier handling of the config
    private final ConfigOption<Integer> CACHE_MILLISECONDS = new ConfigOption<>("cache.seconds", 0);

    public ConfigManager(GreenLocker plugin) {
        this.plugin = plugin;
    }

    @Deprecated(forRemoval = true) //todo
    private static HashSet<Material> setUpLockableDefaults() {
        HashSet<Material> lockablesSet = new HashSet<>(); //todo auto add inventory-blocks
        //(trap)doors
        lockablesSet.addAll(Tag.DOORS.getValues());
        lockablesSet.addAll(Tag.TRAPDOORS.getValues());
        //inventory blocks
        lockablesSet.addAll(Tag.SHULKER_BOXES.getValues());
        lockablesSet.add(Material.CHEST);
        lockablesSet.add(Material.TRAPPED_CHEST);
        lockablesSet.add(Material.CHISELED_BOOKSHELF);
        lockablesSet.add(Material.DECORATED_POT);
        lockablesSet.add(Material.LECTERN);
        lockablesSet.add(Material.BARREL);
        lockablesSet.add(Material.FURNACE);
        lockablesSet.add(Material.BLAST_FURNACE);
        lockablesSet.add(Material.SMOKER);
        lockablesSet.add(Material.HOPPER);
        lockablesSet.add(Material.DISPENSER);
        lockablesSet.add(Material.DROPPER);
        lockablesSet.add(Material.BREWING_STAND);
        lockablesSet.add(Material.JUKEBOX);
        lockablesSet.add(Material.BEACON);
        lockablesSet.addAll(Tag.CAMPFIRES.getValues());
        lockablesSet.addAll(Tag.ANVIL.getValues());
        //valuable Blocks
        lockablesSet.add(Material.DIAMOND_BLOCK);
        lockablesSet.add(Material.NETHERITE_BLOCK);
        // Maybe add Tag.BANNERS, Tag.BEDS, Tag.BEEHIVES, BrushableBlock, Conduit, CreatureSpawner, EnchantingTable

        return lockablesSet;

    }

    protected static <E extends Enum<E>> @Nullable Enum<E> getEnumVal(@NotNull String arg, @NotNull Enum<E>[] a) {
        for (Enum<E> value : a) {
            if (value.name().equalsIgnoreCase(arg)) {
                return value;
            }
        }

        return null;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        if (config.getBoolean(IMPORT_FROM_LOCKETTEPRO.getPath(), IMPORT_FROM_LOCKETTEPRO.getFallbackValue())) {
            getFromLegacy();
        }
        IMPORT_FROM_LOCKETTEPRO.setValue(false);

        //reload Language files
        plugin.getMessageManager().setLangFileName(config.getString(LANG_FILENAME.getPath(), LANG_FILENAME.getFallbackValue()));
        plugin.getMessageManager().reload();

        DEPENDENCY_WORLDGUARD_ENABLED.setValue(config.getBoolean(DEPENDENCY_WORLDGUARD_ENABLED.getPath(), DEPENDENCY_WORLDGUARD_ENABLED.getFallbackValue()));
        DEPENDENCY_COREPROTECT_ENABLED.setValue(config.getBoolean(DEPENDENCY_COREPROTECT_ENABLED.getPath(), DEPENDENCY_COREPROTECT_ENABLED.getFallbackValue()));

        // load Material set of lockable blocks
        List<?> objects = config.getList(LOCKABLES.getPath(), new ArrayList<>(LOCKABLES.getFallbackValue()));
        Set<Material> resultSet = new HashSet<>();

        Iterable<Tag<Material>> tagCache = null;
        for (Object object : objects) {
            if (object instanceof Material material) {
                resultSet.add(material);
            } else if (object instanceof String string) {
                if (string.equals("*")) {
                    Collections.addAll(resultSet, Material.values());
                    plugin.getLogger().info("All blocks are default to be lockable!");
                    plugin.getLogger().info("Add '-<Material>' to exempt a block, such as '-STONE'!");
                } else {
                    boolean add = true;

                    if (string.startsWith("-")) {
                        add = false;
                        string = string.substring(1);
                    }
                    Material material = Material.matchMaterial(string);

                    if (material != null) {
                        if (material.isBlock()) {
                            if (add) {
                                resultSet.add(material);
                            } else {
                                resultSet.remove(material);
                            }
                        } else {
                            plugin.getLogger().warning("\"" + string + " in lockable block list is not a block!");
                        }
                    } else { //try tags
                        // lazy initialisation
                        if (tagCache == null) {
                            tagCache = plugin.getServer().getTags(Tag.REGISTRY_BLOCKS, Material.class);
                        }

                        boolean found = false;

                        for (Tag<Material> tag : tagCache) {
                            if (tag.getKey().asString().equalsIgnoreCase(string)) {

                                resultSet.addAll(tag.getValues());
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            plugin.getLogger().warning("Couldn't get Material \"" + string + "\" for lockable block list. Ignoring.");
                        }
                    }
                }
            } else {
                if (object != null) {
                    plugin.getLogger().warning("Couldn't get Material \"" + object + "\" for lockable block list. Ignoring.");
                }
            }
            /* todo use this in next version
        switch (object) {
                case Material material -> resultSet.add(material);
                case String string -> {
                    if (string.equals("*")) {
                        Collections.addAll(resultSet, Material.values());
                        plugin.getLogger().info("All blocks are default to be lockable!");
                        plugin.getLogger().info("Add '-<Material>' to exempt a block, such as '-STONE'!");
                    } else {
                        boolean add = true;

                        if (string.startsWith("-")) {
                            add = false;
                            string = string.substring(1);
                        }
                        Material material = Material.matchMaterial(string);

                        if (material != null) {
                            if (material.isBlock()) {
                                if (add) {
                                    resultSet.add(material);
                                } else {
                                    resultSet.remove(material);
                                }
                            } else {
                                plugin.getLogger().warning("\"" + string + " in lockable block list is not a block!");
                            }
                        } else { //try tags
                            // lazy initialisation
                            if (tagCache == null) {
                                tagCache = plugin.getServer().getTags(Tag.REGISTRY_BLOCKS, Material.class);
                            }

                            boolean found = false;

                            for (Tag<Material> tag : tagCache) {
                                if (tag.getKey().asString().equalsIgnoreCase(string)) {

                                    resultSet.addAll(tag.getValues());
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                plugin.getLogger().warning("Couldn't get Material \"" + string + "\" for lockable block list. Ignoring.");
                            }
                        }
                    }
                }
                default ->
                        plugin.getLogger().warning("Couldn't get Material \"" + object + "\" for lockable block list. Ignoring.");
            }
         */
        }
        //never allow these!
        resultSet.removeAll(Tag.ALL_SIGNS.getValues());
        resultSet.remove(Material.SCAFFOLDING);
        resultSet.remove(Material.AIR);
        resultSet.remove(Material.CAVE_AIR);
        LOCKABLES.setValue(resultSet);

        Object object = config.get(QUICKPROTECT_TYPE.getPath(), QUICKPROTECT_TYPE.getFallbackValue());
        if (Objects.requireNonNull(object) instanceof QuickProtectOption quickProtectOption) {
            QUICKPROTECT_TYPE.setValue(quickProtectOption);
        } else if (object instanceof String string) {
            QuickProtectOption setting = (QuickProtectOption) getEnumVal(string, QuickProtectOption.values());

            if (setting != null) {
                QUICKPROTECT_TYPE.setValue(setting);
            } else {
                plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + string + "\" for quick lock setting. Ignoring and using default value.");
            }
        } else {
            plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + object + "\" for quick lock setting. Ignoring and using default value.");
        }
        /* todo use this next java version
        switch (object) {
            case QuickProtectOption quickProtectOption -> QUICKPROTECT_TYPE.setValue(quickProtectOption);
            case String string -> {
                QuickProtectOption setting = (QuickProtectOption) getEnumVal(string, QuickProtectOption.values());

                if (setting != null) {
                    QUICKPROTECT_TYPE.setValue(setting);
                } else {
                    plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + string + "\" for quick lock setting. Ignoring and using default value.");
                }
            }
            default ->
                    plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + object + "\" for quick lock setting. Ignoring and using default value.");
        }*/

        LOCK_BLOCKS_INTERFERE.setValue(config.getBoolean(LOCK_BLOCKS_INTERFERE.getPath(), LOCK_BLOCKS_INTERFERE.getFallbackValue()));
        LOCK_BLOCKS_ITEM_TRANSFER_IN.setValue(config.getBoolean(LOCK_BLOCKS_ITEM_TRANSFER_IN.getPath(), LOCK_BLOCKS_ITEM_TRANSFER_IN.getFallbackValue()));
        LOCK_BLOCKS_ITEM_TRANSFER_OUT.setValue(config.getBoolean(LOCK_BLOCKS_ITEM_TRANSFER_OUT.getPath(), LOCK_BLOCKS_ITEM_TRANSFER_OUT.getFallbackValue()));

        object = config.get(LOCK_BLOCKS_HOPPER_MINECART.getPath(), LOCK_BLOCKS_HOPPER_MINECART.getFallbackValue());
        if (Objects.requireNonNull(object) instanceof HopperMinecartBlockedOption quickProtectOption) {
            LOCK_BLOCKS_HOPPER_MINECART.setValue(quickProtectOption);
        } else if (object instanceof String string) {
            HopperMinecartBlockedOption setting = (HopperMinecartBlockedOption) getEnumVal(string, HopperMinecartBlockedOption.values());

            if (setting != null) {
                LOCK_BLOCKS_HOPPER_MINECART.setValue(setting);
            } else {
                plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + string + "\" for quick lock setting. Ignoring and using default value.");
            }
        } else {
            plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + object + "\" for quick lock setting. Ignoring and using default value.");
        }
        /* todo use this next java version
        switch (object) {
            case HopperMinecartBlockedOption quickProtectOption ->
                    LOCK_BLOCKS_HOPPER_MINECART.setValue(quickProtectOption);
            case String string -> {
                HopperMinecartBlockedOption setting = (HopperMinecartBlockedOption) getEnumVal(string, HopperMinecartBlockedOption.values());

                if (setting != null) {
                    LOCK_BLOCKS_HOPPER_MINECART.setValue(setting);
                } else {
                    plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + string + "\" for quick lock setting. Ignoring and using default value.");
                }
            }
            default ->
                    plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + object + "\" for quick lock setting. Ignoring and using default value.");
        }
        */

        // load Material set of lockable blocks
        objects = config.getList(LOCK_EXEMPTIONS.getPath(), new ArrayList<>(LOCK_EXEMPTIONS.getFallbackValue()));
        Set<ProtectionExemption> exemptions = new HashSet<>();
        for (Object exemptionObj : objects) {
            if (Objects.requireNonNull(exemptionObj) instanceof ProtectionExemption protectionExemtion) {
                exemptions.add(protectionExemtion);
            } else if (exemptionObj instanceof String string) {
                ProtectionExemption protectionExemtion = (ProtectionExemption) getEnumVal(string, ProtectionExemption.values());

                if (protectionExemtion != null) {
                    exemptions.add(protectionExemtion);
                } else {
                    plugin.getLogger().warning("Couldn't get exemtion \"" + string + "\" for lock exemtion list. Ignoring.");
                }
            } else {
                plugin.getLogger().warning("Couldn't get exemtion \"" + exemptionObj + "\" for lock exemtion list. Ignoring.");
            }
            /* todo use this next java version
            switch (exemptionObj) {
                case ProtectionExemption protectionExemtion -> exemptions.add(protectionExemtion);
                case String string -> {
                    ProtectionExemption protectionExemtion = (ProtectionExemption) getEnumVal(string, ProtectionExemption.values());

                    if (protectionExemtion != null) {
                        exemptions.add(protectionExemtion);
                    } else {
                        plugin.getLogger().warning("Couldn't get exemtion \"" + string + "\" for lock exemtion list. Ignoring.");
                    }
                }
                default ->
                        plugin.getLogger().warning("Couldn't get exemtion \"" + exemptionObj + "\" for lock exemtion list. Ignoring.");
            }
            */
        }
        LOCK_EXEMPTIONS.setValue(exemptions);

        LOCK_EXPIRE_DAYS.setValue(config.getLong(LOCK_EXPIRE_DAYS.getPath(), LOCK_EXPIRE_DAYS.getFallbackValue()));

        CACHE_MILLISECONDS.setValue(config.getInt(CACHE_MILLISECONDS.getPath(), CACHE_MILLISECONDS.getFallbackValue()) * 1000);
        if (CACHE_MILLISECONDS.getValueOrFallback() > 0) {
            plugin.getLogger().info("Cache is enabled! In case of inconsistency, turn off immediately.");
        }

        //todo something seems fishy here so this is disabled for now
        /*
        lockdefaultcreatetime = config.getLong("lock-default-create-time-unix", -1L);
        if (lockdefaultcreatetime < -1L) lockdefaultcreatetime = -1L;
        */
    }

    @Deprecated(forRemoval = true)
    private void getFromLegacy() {
        LegacyLocketteConfigAdapter adapter = new LegacyLocketteConfigAdapter();
        adapter.reload(plugin);

        FileConfiguration config = plugin.getConfig();

        config.set(DEPENDENCY_WORLDGUARD_ENABLED.getPath(), adapter.workWithWorldguard());
        config.set(DEPENDENCY_COREPROTECT_ENABLED.getPath(), adapter.workWithCoreprotect());
        config.set(LOCKABLES.getPath(), adapter.getLockables());
        config.set(QUICKPROTECT_TYPE.getPath(), adapter.getQuickProtectAction());
        config.set(LOCK_BLOCKS_INTERFERE.getPath(), adapter.isInterferePlacementBlocked());
        config.set(LOCK_BLOCKS_ITEM_TRANSFER_IN.getPath(), adapter.isItemTransferInBlocked());
        config.set(LOCK_BLOCKS_ITEM_TRANSFER_OUT.getPath(), adapter.isItemTransferOutBlocked());
        config.set(LOCK_BLOCKS_HOPPER_MINECART.getPath(), adapter.isItemTransferOutBlocked());
        config.set(LOCK_EXEMPTIONS.getPath(), adapter.getProtectionExemptions());
        config.set(LOCK_EXPIRE_DAYS.getPath(), adapter.getLockExpireDays());
        config.set(CACHE_MILLISECONDS.getPath(), adapter.getCacheTimeSeconds());

        plugin.saveConfig();
        plugin.reloadConfig();
    }

    public QuickProtectOption getQuickProtectAction() {
        return QUICKPROTECT_TYPE.getValueOrFallback();
    }

    public boolean isInterferePlacementBlocked() {
        return LOCK_BLOCKS_INTERFERE.getValueOrFallback();
    }

    public boolean isItemTransferInBlocked() {
        return LOCK_BLOCKS_ITEM_TRANSFER_IN.getValueOrFallback();
    }

    public boolean isItemTransferOutBlocked() {
        return LOCK_BLOCKS_ITEM_TRANSFER_OUT.getValueOrFallback();
    }

    public HopperMinecartBlockedOption getHopperMinecartAction() {
        return LOCK_BLOCKS_HOPPER_MINECART.getValueOrFallback();
    }

    public boolean doLocksExpire() {
        return LOCK_EXPIRE_DAYS.getValueOrFallback() > 0;
    }

    public Long getLockExpireDays() {
        return LOCK_EXPIRE_DAYS.getValueOrFallback();
    }

    public long getLockDefaultCreateTimeEpoch() {
        //todo something seems fishy here so this is disabled for now
        return -1L;
    }

    public boolean isLockable(Material material) {
        return LOCKABLES.getValueOrFallback().contains(material);
    }

    public int getCacheTimeMillis() {
        return CACHE_MILLISECONDS.getValueOrFallback();
    }

    public boolean isCacheEnabled() {
        return CACHE_MILLISECONDS.getValueOrFallback() > 0;
    }

    public boolean isProtectionExempted(ProtectionExemption against) {
        return LOCK_EXEMPTIONS.getValueOrFallback().contains(against);
    }

    public boolean shouldUseWorldguard() {
        return DEPENDENCY_WORLDGUARD_ENABLED.getValueOrFallback();
    }

    public boolean shouldUseCoreprotect() {
        return DEPENDENCY_COREPROTECT_ENABLED.getValueOrFallback();
    }

    public enum QuickProtectOption {
        NOT_SNEAKING_REQUIRED,
        SNEAK_NONRELEVANT,
        SNEAK_REQUIRED,
        OFF,
    }

    public enum ProtectionExemption {
        EXPLOSION,
        GROWTH,
        PISTON,
        REDSTONE,
        // entities
        VILLAGER, // open doors
        ENDERMAN,
        ENDER_DRAGON,
        WITHER,
        ZOMBIE,
        SILVERFISH // break blocks if they slither out of them
    }

    public enum HopperMinecartBlockedOption {
        TRUE,
        FALSE,
        REMOVE
    }
}
