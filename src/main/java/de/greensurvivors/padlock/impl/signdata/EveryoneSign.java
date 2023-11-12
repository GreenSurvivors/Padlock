package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * A sign with the everyone setting act like everyone is a member on it.
 */
public class EveryoneSign {
    private final static NamespacedKey everyoneKey = new NamespacedKey(Padlock.getPlugin(), "Everyone");

    /**
     * Set if everyone should have member access or not.
     * @param shouldUpdateDisplay if the display should get updated. is important to set to false for updating a lock sign from legacy
     */
    public static void setEveryone(@NotNull Sign sign, boolean everyoneHasAccess, boolean shouldUpdateDisplay) {
        sign.getPersistentDataContainer().set(everyoneKey, PersistentDataType.BOOLEAN, everyoneHasAccess);
        sign.update();

        if (shouldUpdateDisplay) {
            SignDisplay.updateDisplay(sign);
        }
    }

    /**
     * get if everyone has access or not,
     * will start an update process, if the sign is a legacy sign
     */
    public static boolean getAccessEveryone(@NotNull Sign sign) {
        Boolean hasEveryOneAccess = sign.getPersistentDataContainer().get(everyoneKey, PersistentDataType.BOOLEAN);

        if (hasEveryOneAccess == null && getLegacySetting(sign)) {
            PadlockAPI.updateLegacySign(sign);
            return true;
        }

        return hasEveryOneAccess != null && hasEveryOneAccess;
    }

    /**
     * update a legacy lockette lock sign with potential an everyone line on it.
     * Will not update the Display of the sign afterwarts to not overwrite other unimported data like timers
     */
    @Deprecated(forRemoval = true)
    public static void updateLegacy(@NotNull Sign sign) {
        setEveryone(sign, getLegacySetting(sign), false);
    }

    /**
     * checks if a line would be a line of an everyone sign.
     * This is only available to make sure the line can safely interpreted as a username.
     * Please don't use this to get data of a legacy sign.
     * Use {@link #updateLegacy(Sign)} and then {@link #getAccessEveryone(Sign)}
     */
    @Deprecated(forRemoval = true)
    protected static boolean isLegacyComp(@NotNull Component component) {
        return Padlock.getPlugin().getMessageManager().isSignComp(component, MessageManager.LangPath.EVERYONE_SIGN);
    }

    /**
     * returns true if at least one sign is a legacy lockette everyone line.
     */
    @Deprecated(forRemoval = true)
    private static boolean getLegacySetting(@NotNull Sign sign) {
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            if (Padlock.getPlugin().getMessageManager().isSignComp(line, MessageManager.LangPath.EVERYONE_SIGN)) {
                return true;
            }
        }

        return false;
    }

    /**
     * update a legacy lockette additional sign with potential an everyone line on it.
     */
    public static void updateLegacyFromAdditional(Sign lockSign, Sign additional) {
        setEveryone(lockSign, getLegacySetting(additional), true);
    }
}
