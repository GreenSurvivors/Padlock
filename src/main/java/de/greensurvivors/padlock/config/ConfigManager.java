package de.greensurvivors.padlock.config;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.impl.MiscUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * As the name might suggest: this will manage the config options of this plugin.
 */
public class ConfigManager {
    private final static @NotNull String MC_NAMESPACE = NamespacedKey.MINECRAFT.toUpperCase(Locale.ENGLISH) + ":";
    private final @NotNull Padlock plugin;
    // please note: While fallback values are defined here, these are in fact NOT the default options. They are just used in the unfortunate case loading them goes wrong.
    // if you want to change default options, have also a look into resources/config.yaml
    private final ConfigOption<Boolean> IMPORT_FROM_LOCKETTEPRO = new ConfigOption<>("import-fromLockettePro", false);
    private final ConfigOption<String> LANG_FILENAME = new ConfigOption<>("language-file-name", "lang/lang_en.yml");
    private final ConfigOption<Boolean> DEPENDENCY_WORLDGUARD_ENABLED = new ConfigOption<>("dependency.worldguard.enabled", true);
    private final ConfigOption<Boolean> DEPENDENCY_WORLDGUARD_OVERWRITE = new ConfigOption<>("dependency.worldguard.overwrite", false);
    private final ConfigOption<Set<Material>> LOCKABLES = new ConfigOption<>("lockables", new HashSet<>()); //todo auto add inventory-blocks
    private final ConfigOption<QuickProtectOption> QUICKPROTECT_TYPE = new ConfigOption<>("lock.quick-lock.type", QuickProtectOption.NOT_SNEAKING_REQUIRED);
    private final ConfigOption<Boolean> LOCK_BLOCKS_INTERFERE = new ConfigOption<>("lock.blocked.interfere", true);
    private final ConfigOption<Boolean> LOCK_BLOCKS_ITEM_TRANSFER_IN = new ConfigOption<>("lock.blocked.item-transfer.in", false);
    private final ConfigOption<Boolean> LOCK_BLOCKS_ITEM_TRANSFER_OUT = new ConfigOption<>("lock.blocked.item-transfer.out", true);
    private final ConfigOption<@Range(from = 0, to = Integer.MAX_VALUE) Integer> ITEM_TRANSFER_COOLDOWN = new ConfigOption<>("lock.blocked.item-transfer.cooldown-ticks", 1200);
    private final ConfigOption<HopperMinecartMoveItemOption> LOCK_BLOCKS_HOPPER_MINECART = new ConfigOption<>("lock.blocked.hopper-minecart", HopperMinecartMoveItemOption.REMOVE);
    private final ConfigOption<Set<ProtectionExemption>> LOCK_EXEMPTIONS = new ConfigOption<>("lock.exemptions", Set.of());
    private final ConfigOption<Long> LOCK_EXPIRE_DAYS = new ConfigOption<>("lock.expire.days", 999L);
    private final ConfigOption<Integer> CACHE_SECONDS = new ConfigOption<>("cache.seconds", 0);
    private final ConfigOption<String> BEDROCK_PREFIX = new ConfigOption<>("bedrock-prefix", ".");

    public ConfigManager(@NotNull Padlock plugin) {
        this.plugin = plugin;
    }


    /**
     * reload the config and language files,
     * will save default config
     */
    public void reload() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        if (config.getBoolean(IMPORT_FROM_LOCKETTEPRO.getPath(), IMPORT_FROM_LOCKETTEPRO.getFallbackValue())) {
            getFromLegacy();
        }
        IMPORT_FROM_LOCKETTEPRO.setValue(false);

        //reload Language files
        plugin.getMessageManager().reload(config.getString(LANG_FILENAME.getPath(), LANG_FILENAME.getFallbackValue()));

        //dependency
        DEPENDENCY_WORLDGUARD_ENABLED.setValue(config.getBoolean(DEPENDENCY_WORLDGUARD_ENABLED.getPath(), DEPENDENCY_WORLDGUARD_ENABLED.getFallbackValue()));
        DEPENDENCY_WORLDGUARD_OVERWRITE.setValue(config.getBoolean(DEPENDENCY_WORLDGUARD_OVERWRITE.getPath(), DEPENDENCY_WORLDGUARD_OVERWRITE.getFallbackValue()));

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

                        string = string.toUpperCase(java.util.Locale.ENGLISH);
                        string = string.replaceAll("\\s+", "_");

                        if (!string.startsWith(MC_NAMESPACE)) {
                            string = MC_NAMESPACE + string;
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

                            string = string.toUpperCase(java.util.Locale.ENGLISH);
                            string = string.replaceAll("\\s+", "_");

                            if (!string.startsWith(MC_NAMESPACE)) {
                                string = MC_NAMESPACE + string;
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
        if (object instanceof QuickProtectOption quickProtectOption) {
            QUICKPROTECT_TYPE.setValue(quickProtectOption);
        } else if (object instanceof String string) {
            QuickProtectOption setting = MiscUtils.getEnum(QuickProtectOption.class, string);

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
            default -> plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + object + "\" for quick lock setting. Ignoring and using default value.");
        }*/

        LOCK_BLOCKS_INTERFERE.setValue(config.getBoolean(LOCK_BLOCKS_INTERFERE.getPath(), LOCK_BLOCKS_INTERFERE.getFallbackValue()));
        LOCK_BLOCKS_ITEM_TRANSFER_IN.setValue(config.getBoolean(LOCK_BLOCKS_ITEM_TRANSFER_IN.getPath(), LOCK_BLOCKS_ITEM_TRANSFER_IN.getFallbackValue()));
        LOCK_BLOCKS_ITEM_TRANSFER_OUT.setValue(config.getBoolean(LOCK_BLOCKS_ITEM_TRANSFER_OUT.getPath(), LOCK_BLOCKS_ITEM_TRANSFER_OUT.getFallbackValue()));
        ITEM_TRANSFER_COOLDOWN.setValue(Math.max(0, config.getInt(ITEM_TRANSFER_COOLDOWN.getPath(), ITEM_TRANSFER_COOLDOWN.getFallbackValue())));

        object = config.get(LOCK_BLOCKS_HOPPER_MINECART.getPath(), LOCK_BLOCKS_HOPPER_MINECART.getFallbackValue());
        if (Objects.requireNonNull(object) instanceof HopperMinecartMoveItemOption quickProtectOption) {
            LOCK_BLOCKS_HOPPER_MINECART.setValue(quickProtectOption);
        } else if (object instanceof String string) {
            HopperMinecartMoveItemOption setting = MiscUtils.getEnum(HopperMinecartMoveItemOption.class, string);

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
            case HopperMinecartMoveItemOption quickProtectOption ->
                    LOCK_BLOCKS_HOPPER_MINECART.setValue(quickProtectOption);
            case String string -> {
                HopperMinecartMoveItemOption setting = (HopperMinecartMoveItemOption) getEnumVal(string, HopperMinecartMoveItemOption.values());

                if (setting != null) {
                    LOCK_BLOCKS_HOPPER_MINECART.setValue(setting);
                } else {
                    plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + string + "\" for quick lock setting. Ignoring and using default value.");
                }
            }
            default -> plugin.getLogger().warning("Couldn't get QuickProtectOption \"" + object + "\" for quick lock setting. Ignoring and using default value.");
        }
        */

        // load lock exemptions
        objects = config.getList(LOCK_EXEMPTIONS.getPath(), new ArrayList<>(LOCK_EXEMPTIONS.getFallbackValue()));
        Set<ProtectionExemption> exemptions = new HashSet<>();
        for (Object exemptionObj : objects) {
            if (exemptionObj instanceof ProtectionExemption protectionExemtion) {
                exemptions.add(protectionExemtion);
            } else if (exemptionObj instanceof String string) {
                ProtectionExemption protectionExemtion = MiscUtils.getEnum(ProtectionExemption.class, string);

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

        CACHE_SECONDS.setValue(Math.max(0, config.getInt(CACHE_SECONDS.getPath(), CACHE_SECONDS.getFallbackValue())));
        if (CACHE_SECONDS.getValueOrFallback() > 0) {
            plugin.getLockCacheManager().setExpirationTime(CACHE_SECONDS.getValueOrFallback(), TimeUnit.SECONDS);
            plugin.getLogger().info("Cache is enabled! In case of inconsistency, turn off immediately.");
        }

        BEDROCK_PREFIX.setValue(config.getString(BEDROCK_PREFIX.getPath(), BEDROCK_PREFIX.getFallbackValue()));
        MiscUtils.setBedrockPrefix(BEDROCK_PREFIX.getValueOrFallback());
    }

    /**
     * Bridge to load LockettePro configs for easy switch
     */
    @Deprecated(forRemoval = true)
    private void getFromLegacy() {
        LegacyLocketteConfigAdapter adapter = new LegacyLocketteConfigAdapter();
        adapter.reload(plugin);

        FileConfiguration config = plugin.getConfig();

        config.set(DEPENDENCY_WORLDGUARD_ENABLED.getPath(), adapter.workWithWorldguard());
        config.set(LOCKABLES.getPath(), adapter.getLockables());
        config.set(QUICKPROTECT_TYPE.getPath(), adapter.getQuickProtectAction());
        config.set(LOCK_BLOCKS_INTERFERE.getPath(), adapter.isInterferePlacementBlocked());
        config.set(LOCK_BLOCKS_ITEM_TRANSFER_IN.getPath(), adapter.isItemTransferInBlocked());
        config.set(LOCK_BLOCKS_ITEM_TRANSFER_OUT.getPath(), adapter.isItemTransferOutBlocked());
        config.set(LOCK_BLOCKS_HOPPER_MINECART.getPath(), adapter.isItemTransferOutBlocked());
        config.set(LOCK_EXEMPTIONS.getPath(), adapter.getProtectionExemptions());
        config.set(LOCK_EXPIRE_DAYS.getPath(), adapter.getLockExpireDays());
        config.set(CACHE_SECONDS.getPath(), adapter.getCacheTimeSeconds());

        plugin.saveConfig();
        plugin.reloadConfig();
    }

    public @NotNull QuickProtectOption getQuickProtectAction() {
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

    public int getItemTransferCooldown() {
        return ITEM_TRANSFER_COOLDOWN.getValueOrFallback();
    }

    public @NotNull HopperMinecartMoveItemOption getHopperMinecartAction() {
        return LOCK_BLOCKS_HOPPER_MINECART.getValueOrFallback();
    }

    public boolean doLocksExpire() {
        return LOCK_EXPIRE_DAYS.getValueOrFallback() > 0;
    }

    public @NotNull Long getLockExpireDays() {
        return LOCK_EXPIRE_DAYS.getValueOrFallback();
    }

    public boolean isLockable(Material material) {
        return LOCKABLES.getValueOrFallback().contains(material);
    }

    public int getCacheTimeSeconds() {
        return CACHE_SECONDS.getValueOrFallback();
    }

    public boolean isCacheEnabled() {
        return CACHE_SECONDS.getValueOrFallback() > 0;
    }

    public boolean isProtectionExempted(ProtectionExemption against) {
        return LOCK_EXEMPTIONS.getValueOrFallback().contains(against);
    }

    public boolean shouldUseWorldguard() {
        return DEPENDENCY_WORLDGUARD_ENABLED.getValueOrFallback();
    }

    public boolean shouldOverwriteWorldguard() {
        return DEPENDENCY_WORLDGUARD_OVERWRITE.getValueOrFallback();
    }

    public enum QuickProtectOption {
        /**
         * only quick protect if the player is NOT sneaking
         **/
        NOT_SNEAKING_REQUIRED,
        /**
         * quick protect, regardless if the player is sneaking or not (not recommended)
         */
        SNEAK_NONRELEVANT,
        /**
         * only quick protect if the player IS sneaking
         */
        SNEAK_REQUIRED,
        /**
         * don't quick protect
         */
        NO_QUICKLOCK,
    }

    /**
     * everything listed here is protected against, however one might want to not to,
     * so you can turn it off.
     */
    public enum ProtectionExemption {
        /**
         * tnt, creeper, endcrystal, every explosion.
         **/
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

    public enum HopperMinecartMoveItemOption {
        /**
         * doesn't allow Items to flow from the minecart into a container
         */
        BLOCKED,
        /**
         * allows minecarts to insert items into locked containers
         */
        ALLOWED,
        /**
         * breaks the mine-cart
         */
        REMOVE
    }
}
