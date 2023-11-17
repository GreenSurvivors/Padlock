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
    private final static NamespacedKey lockTypeKey = new NamespacedKey(Padlock.getPlugin(), "LockAccessType");

    public static void setAccessType(@NotNull Sign sign, AccessType accessType, boolean shouldUpdateDisplay) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        container.set(lockTypeKey, PersistentDataType.STRING, accessType.name());
        sign.update();

        if (shouldUpdateDisplay) {
            SignDisplay.updateDisplay(sign);
        }
    }

    public static boolean isAnyAccessComp(@NotNull Component line) {
        MessageManager manager = Padlock.getPlugin().getMessageManager();

        return manager.isSignComp(line, MessageManager.LangPath.PRIVATE_SIGN); //todo
    }

    /**
     * will start an update process, if the sign is a legacy sign
     */
    public static @Nullable AccessType getAccessType(@NotNull Sign sign) {
        String hasEveryOneAccess = sign.getPersistentDataContainer().get(lockTypeKey, PersistentDataType.STRING);

        AccessType accessType;
        if (hasEveryOneAccess == null) {
            accessType = getLegacySetting(sign);

            PadlockAPI.updateLegacySign(sign);
        } else {
            accessType = MiscUtils.getEnum(AccessType.class, hasEveryOneAccess);

            if (accessType == null) {
                accessType = AccessType.PRIVATE;
            }

        }

        return accessType;
    }

    //public static Component get //todo

    /**
     * update a legacy lockette lock sign with potential an everyone line on it.
     * Will not update the Display of the sign afterwarts to not overwrite other unimported data like timers
     */
    @Deprecated(forRemoval = true)
    public static void updateLegacy(@NotNull Sign sign) {
        setAccessType(sign, getLegacySetting(sign), false);
    }

    /**
     * checks if a line would be a line of an everyone sign.
     * This is only available to make sure the line can safely interpreted as a username.
     * Please don't use this to get data of a legacy sign.
     * Use {@link #updateLegacy(Sign)} and then {@link #getAccessType(Sign)}
     */
    @Deprecated(forRemoval = true)
    private static boolean isLegacyEveryOneComp(@NotNull Component component) {
        return Padlock.getPlugin().getMessageManager().isSignComp(component, MessageManager.LangPath.EVERYONE_SIGN);
    }

    /**
     * returns {@link AccessType#PUBLIC} if at least one sign is a legacy lockette everyone line.
     */
    @Deprecated(forRemoval = true)
    private static AccessType getLegacySetting(@NotNull Sign sign) {
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            if (Padlock.getPlugin().getMessageManager().isSignComp(line, MessageManager.LangPath.EVERYONE_SIGN)) {
                return AccessType.PUBLIC;
            }
        }

        return AccessType.PRIVATE;
    }

    /**
     * update a legacy lockette additional sign with potential an everyone line on it.
     */
    public static void updateLegacyFromAdditional(Sign lockSign, Sign additional) {
        setAccessType(lockSign, getLegacySetting(additional), true);
    }

    public enum AccessType {
        PRIVATE(false),
        PUBLIC(false), // everyone is member
        DONATION(true),
        DISPLAY(true);

        private final boolean isInventoryHolderOnly;

        AccessType(boolean isInventoryHolderOnly) {
            this.isInventoryHolderOnly = isInventoryHolderOnly;
        }

        public boolean doesQualifyAs(Block block) {
            return !isInventoryHolderOnly || block.getState() instanceof InventoryHolder;
        }
    }
}
