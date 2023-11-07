package de.greensurvivors.greenlocker.impl.signdata;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignTimer {
    private final static Pattern legacyPattern = Pattern.compile(GreenLocker.getPlugin().getMessageManager().getNakedSignText(MessageManager.LangPath.TIMER_SIGN).replace("[", "\\[(i?)").replace("<time>", "(-?[0-9]+)"));
    private final static NamespacedKey timerKey = new NamespacedKey(GreenLocker.getPlugin(), "timer");

    protected static @Nullable Component getTimerComponent(@NotNull Sign sign) {
        Long timerDuration = getTimer(sign);

        if (timerDuration != null && timerDuration > 0) {
            return GreenLocker.getPlugin().getMessageManager().getLang(MessageManager.LangPath.TIMER_SIGN,
                    Placeholder.unparsed(MessageManager.PlaceHolder.TIME.getPlaceholder(), String.valueOf(timerDuration)));
        } else {
            return null;
        }
    }

    public static void setTimer(@NotNull Sign sign, long timerDuration) {
        sign.getPersistentDataContainer().set(timerKey, PersistentDataType.LONG, timerDuration);
        sign.update();

        SignDisplay.updateDisplay(sign);
    }

    public static @Nullable Long getTimer(Sign sign) {
        Long timerDuration = sign.getPersistentDataContainer().get(timerKey, PersistentDataType.LONG);

        if (timerDuration != null) {
            return timerDuration;
        } else {
            timerDuration = getLegacyTimer(sign);

            if (timerDuration != null) {
                GreenLockerAPI.updateLegacySign(sign);
                return timerDuration;
            } else {
                return null;
            }
        }
    }

    @Deprecated(forRemoval = true)
    public static void updateLegacyTimer(Sign sign) {
        Long legacyTimer = getLegacyTimer(sign);

        if (legacyTimer != null) {
            setTimer(sign, legacyTimer);
        }
    }

    @Deprecated(forRemoval = true)
    private static @Nullable Long getLegacyTimer(Sign sign) {
        for (Component compToTest : sign.getSide(Side.FRONT).lines()) {
            String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();
            Matcher matcher = legacyPattern.matcher(strToTest);

            if (matcher.matches()) {
                return Math.min(Long.parseLong(matcher.group(1)), 20);
            }
        }

        return null;
    }
}
