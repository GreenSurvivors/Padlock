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
     */
    public static void setEveryone(@NotNull Sign sign, boolean everyoneHasAccess) {
        sign.getPersistentDataContainer().set(everyoneKey, PersistentDataType.BOOLEAN, everyoneHasAccess);
        sign.update();

        SignDisplay.updateDisplay(sign);
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
     * update a legacy lockette sign with potential an everyone line on it.
     */
    @Deprecated(forRemoval = true)
    public static void updateLegacy(@NotNull Sign sign) {
        setEveryone(sign, getLegacySetting(sign));
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
}
