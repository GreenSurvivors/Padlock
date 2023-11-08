package de.greensurvivors.greenlocker.impl.signdata;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class EveryoneSign {
    private final static NamespacedKey everyoneKey = new NamespacedKey(GreenLocker.getPlugin(), "Everyone");

    public static void setEveryone(@NotNull Sign sign, boolean everyoneHasAccess) {
        sign.getPersistentDataContainer().set(everyoneKey, PersistentDataType.BOOLEAN, everyoneHasAccess);
        sign.update();

        SignDisplay.updateDisplay(sign);
    }

    public static boolean getAccessEveryone(@NotNull Sign sign) {
        Boolean hasEveryOneAccess = sign.getPersistentDataContainer().get(everyoneKey, PersistentDataType.BOOLEAN);

        if (hasEveryOneAccess == null && getLegacySetting(sign)) {
            GreenLockerAPI.updateLegacySign(sign);
            return true;
        }

        return hasEveryOneAccess != null && hasEveryOneAccess;
    }

    @Deprecated(forRemoval = true)
    public static void updateLegacy(@NotNull Sign sign) {
        setEveryone(sign, getLegacySetting(sign));
    }

    @Deprecated(forRemoval = true)
    private static boolean getLegacySetting(@NotNull Sign sign) {
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            if (GreenLocker.getPlugin().getMessageManager().isSignComp(line, MessageManager.LangPath.EVERYONE_SIGN)) {
                return true;
            }
        }

        return false;
    }
}
