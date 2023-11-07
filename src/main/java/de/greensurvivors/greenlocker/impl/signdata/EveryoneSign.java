package de.greensurvivors.greenlocker.impl.signdata;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class EveryoneSign {
    private final static NamespacedKey everyoneKey = new NamespacedKey(GreenLocker.getPlugin(), "Everyone");
    private final static Pattern legacyPattern = Pattern.compile("^(i?)" + GreenLocker.getPlugin().getMessageManager().getNakedSignText(MessageManager.LangPath.EVERYONE_SIGN) + "$");

    public static void setEveryone(@NotNull Sign sign, boolean everyoneHasAccess) {
        sign.getPersistentDataContainer().set(everyoneKey, PersistentDataType.BOOLEAN, everyoneHasAccess);
    }

    public static @Nullable Boolean getAccessEveryone(@NotNull Sign sign) {
        Boolean hasEveryOneAccess = sign.getPersistentDataContainer().get(everyoneKey, PersistentDataType.BOOLEAN);

        if (hasEveryOneAccess == null && getLegacySetting(sign)) {
            GreenLockerAPI.updateLegacySign(sign);
            return true;
        }

        return hasEveryOneAccess;
    }

    @Deprecated(forRemoval = true)
    public static void updateLegacy(@NotNull Sign sign) {
        setEveryone(sign, getLegacySetting(sign));
    }

    @Deprecated(forRemoval = true)
    private static boolean getLegacySetting(@NotNull Sign sign) {
        PlainTextComponentSerializer plainTextComponentSerializer = PlainTextComponentSerializer.plainText();
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            if (legacyPattern.matcher(plainTextComponentSerializer.serialize(line).trim()).matches()) {
                return true;
            }
        }

        return false;
    }
}
