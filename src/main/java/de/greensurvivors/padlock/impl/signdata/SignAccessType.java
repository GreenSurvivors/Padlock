package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class SignAccessType {
    private final static NamespacedKey accessTypeKey = new NamespacedKey(Padlock.getPlugin(), "LockAccessType");

    public static void setAccessType(@NotNull Sign sign, @NotNull AccessType accessType, boolean shouldUpdateDisplay) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            Padlock.getPlugin().getLockCacheManager().removeFromCache(sign);
        }

        PersistentDataContainer container = sign.getPersistentDataContainer();

        container.set(accessTypeKey, PersistentDataType.STRING, accessType.name());
        sign.update();

        if (shouldUpdateDisplay) {
            SignDisplay.updateDisplay(sign);
        }
    }

    /**
     * unless you work with player input use {@link #getAccessType(Sign, boolean)} instead!
     */
    public static AccessType getAccessTypeFromComp(@NotNull Component line) {
        MessageManager manager = Padlock.getPlugin().getMessageManager();

        if (manager.isSignComp(line, MessageManager.LangPath.PRIVATE_SIGN)) {
            return AccessType.PRIVATE;
        } else if (manager.isSignComp(line, MessageManager.LangPath.PUBLIC_SIGN)) {
            return AccessType.PUBLIC;
        } else if (manager.isSignComp(line, MessageManager.LangPath.DONATION_SIGN)) {
            return AccessType.DONATION;
        } else if (manager.isSignComp(line, MessageManager.LangPath.DISPLAY_SIGN)) {
            return AccessType.DISPLAY;
        } else if (manager.isSignComp(line, MessageManager.LangPath.SUPPLY_SIGN)) {
            return AccessType.SUPPLY;
        } else if (manager.isLegacySignComp(line, MessageManager.LangPath.LEGACY_PRIVATE_SIGN)) {
            return AccessType.PRIVATE;
        } else {
            return null;
        }
    }

    /**
     * will start an update process, if the sign is a legacy sign
     */
    public static @Nullable AccessType getAccessType(@NotNull Sign sign, boolean ignoreCache) {
        if (!ignoreCache && Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            return Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(sign.getLocation()).getAccessType();
        } else {
            String accessTypeStr = sign.getPersistentDataContainer().get(accessTypeKey, PersistentDataType.STRING);

            AccessType accessType;
            if (accessTypeStr == null) {
                accessType = getLegacySetting(sign);
                if (accessType != null) {
                    PadlockAPI.updateLegacySign(sign);
                } else {
                    return null;
                }
            } else {
                accessType = MiscUtils.getEnum(AccessType.class, accessTypeStr);

                if (accessType == null) {
                    accessType = AccessType.PRIVATE;
                }

            }

            return accessType;
        }
    }

    /**
     * update a legacy lockette lock sign with potential an everyone line on it.
     * Will not update the Display of the sign afterwarts to not overwrite other unimported data like timers
     */
    @Deprecated(forRemoval = true)
    public static void updateLegacy(@NotNull Sign sign) {
        AccessType type = getLegacySetting(sign);

        if (type != null) {
            setAccessType(sign, type, false);
        } else {
            Padlock.getPlugin().getLogger().warning("couldn't get an access type to update from. Using private. sign at: " + sign.getLocation());
            setAccessType(sign, AccessType.PRIVATE, false);
        }
    }

    /**
     * checks if a line would be a line of an everyone sign.
     * This is only available to make sure the line can safely interpreted as a username.
     * Please don't use this to get data of a legacy sign.
     * Use {@link #updateLegacy(Sign)} and then {@link #getAccessType(Sign)}
     */
    @Deprecated(forRemoval = true)
    public static boolean isLegacyEveryOneComp(@NotNull Component component) {
        return Padlock.getPlugin().getMessageManager().isLegacySignComp(component, MessageManager.LangPath.LEGACY_EVERYONE_SIGN);
    }

    /**
     * returns {@link AccessType#PUBLIC} if at least one sign is a legacy lockette everyone line.
     */
    @Deprecated(forRemoval = true)
    private static @Nullable AccessType getLegacySetting(@NotNull Sign sign) {
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            if (Padlock.getPlugin().getMessageManager().isLegacySignComp(line, MessageManager.LangPath.LEGACY_PRIVATE_SIGN)) {
                return AccessType.PRIVATE;
            } else if (Padlock.getPlugin().getMessageManager().isLegacySignComp(line, MessageManager.LangPath.LEGACY_EVERYONE_SIGN)) {
                return AccessType.PUBLIC;
            }
        }

        return null;
    }

    /**
     * update a legacy lockette additional sign with potential an everyone line on it.
     */
    public static void updateLegacyFromAdditional(Sign lockSign, Sign additional) {
        AccessType type = getLegacySetting(additional);

        // not every additional sign has an "everyone" on it, and this is the only access type they can have
        if (type != null) {
            setAccessType(lockSign, type, true);
        }
    }

    public enum AccessType {
        PRIVATE(false),
        PUBLIC(false), // everyone is member
        DONATION(true),
        DISPLAY(true),
        SUPPLY(true);

        private final boolean isInventoryHolderOnly;

        AccessType(boolean isInventoryHolderOnly) {
            this.isInventoryHolderOnly = isInventoryHolderOnly;
        }

        public boolean doesQualifyAs(Block block) { //todo
            return !isInventoryHolderOnly || block.getState() instanceof InventoryHolder;
        }
    }
}
