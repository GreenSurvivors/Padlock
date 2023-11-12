package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
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

/**
 * A timer on a lock determines how many milliseconds it takes until the openable,
 * the sign is attached to, toggles.
 * With other words, setting a timer on a sign of a chest does nothing but display a
 * somewhat pretty number.
 */
public class SignTimer {
    /**
     * since the timer has a replaceable part we can't just
     * pull the component from the language file, but we have to
     * build the regexPattern to filter it ourselves.
     * <p>
     * "[" has to get marked as to get used as a character,
     * casing should not matter and the placeholder should be a
     * group of any number to receive later
     */
    private final static Pattern legacyPattern = Pattern.compile(Padlock.getPlugin().getMessageManager().getNakedSignText(MessageManager.LangPath.TIMER_SIGN).replace("[", "\\[(?i)").replace("<time>", "(-?[0-9]+)"));
    private final static NamespacedKey timerKey = new NamespacedKey(Padlock.getPlugin(), "timer");

    /**
     * As for the legacy pattern, we can't just pull the pattern to
     * display from the config, but we have to replace the placeholder
     * with our milliseconds inserted first.
     * Used to display die timer by {@link SignDisplay}
     *
     * @return might be null if no timer was configured
     */
    protected static @Nullable Component getTimerComponent(@NotNull Sign sign) { //todo maybe build a better display by calculating seconds, minutes, etc?
        Long timerDuration = getTimer(sign);

        if (timerDuration != null && timerDuration > 0) {
            return Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.TIMER_SIGN,
                    Placeholder.unparsed(MessageManager.PlaceHolder.TIME.getPlaceholder(), String.valueOf(timerDuration)));
        } else {
            return null;
        }
    }

    /**
     * set the timer to toggle
     *
     * @param timerDuration time duration in milliseconds
     */
    public static void setTimer(@NotNull Sign sign, long timerDuration) {
        sign.getPersistentDataContainer().set(timerKey, PersistentDataType.LONG, timerDuration);
        sign.update();

        SignDisplay.updateDisplay(sign);
    }

    /**
     * get how many milliseconds it should take for an openable to toggle.
     * Will start the update process if the sign was a legacy sign.
     *
     * @return might be null if no timer was configured
     */
    public static @Nullable Long getTimer(@NotNull Sign sign) {
        Long timerDuration = sign.getPersistentDataContainer().get(timerKey, PersistentDataType.LONG);

        if (timerDuration != null) {
            return timerDuration;
        } else {
            timerDuration = getLegacyTimer(sign);

            if (timerDuration != null) {
                PadlockAPI.updateLegacySign(sign);
                return timerDuration;
            } else {
                return null;
            }
        }
    }

    /**
     * Read a timer duration in milliseconds from a component line
     * used to parse timer when locking manuel, should never get used for legacy signs!
     *
     * @return might be null, if it does not fit the legacy pattern.
     */
    public static @Nullable Long getTimerFromComp(@NotNull Component line) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(line).trim();
        Matcher matcher = legacyPattern.matcher(strToTest);

        if (matcher.matches()) {
            return Long.parseLong(matcher.group(1));
        } else {
            return null;
        }
    }

    /**
     * update from a legacy timer, purely written on the sign to one in the getPersistentDataContainer.
     */
    @Deprecated(forRemoval = true)
    public static void updateLegacyTimer(@NotNull Sign sign) {
        Long legacyTimer = getLegacyTimer(sign);

        if (legacyTimer != null) {
            setTimer(sign, legacyTimer);
        }
    }

    /**
     * get the fist legacy timer duration in milliseconds found on a sign.
     *
     * @return might be null if no fitting timer was found.
     */
    @Deprecated(forRemoval = true)
    private static @Nullable Long getLegacyTimer(@NotNull Sign sign) {
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            Long timer = getTimerFromComp(line);

            if (timer != null) {
                return timer;
            }
        }

        return null;
    }
}
