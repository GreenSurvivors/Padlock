package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A timer on a lock determines how long it takes until the openable,
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
    @Deprecated(forRemoval = true)
    private final static Set<Pattern> legacyPatterns = Padlock.getPlugin().getMessageManager().
        getNakedLegacyText(MessageManager.LangPath.LEGACY_TIMER_SIGN).stream().
        map(s -> Pattern.compile(s.replace("[", "\\[(?i)").
            replace("<time>", "(-?[0-9]+)"))).collect(Collectors.toSet());
    // pretty complex stuff [timer:<timer>] and timer can be any number of digits with their timeunit (t, s, h, d, w or M)
    // optionally delimited by whitespace and commas, all ignoring case.
    // valid lines would be "[timer:2h]", "[TIMER:2D, 3w2t 555s]", "[tiMeR: 100W,,,  -8t]", "[timer:2h5h99h]"
    // invalid lines would be "timer:3d]", "[timer24w]", "[time:0x77Q]", "[banana]", "[timer:34ttt]"
    private final static Pattern modernPattern = Pattern.compile(Padlock.getPlugin().getMessageManager().
        getNakedSignText(MessageManager.LangPath.SIGN_LINE_TIMER_SIGN).replace("[", "\\[(?i)").
        replace("<" + MessageManager.PlaceHolder.TIME.getPlaceholder() + ">",
            "\\s?((" + MiscUtils.getPeriodPattern().pattern() + "[\\s,]*?)+)"));
    private final static NamespacedKey timerKey = new NamespacedKey(Padlock.getPlugin(), "timer");

    /**
     * As for the legacy pattern, we can't just pull the pattern to
     * display from the config, but we have to replace the placeholder
     * with our milliseconds inserted first.
     * Used to display die timer by {@link SignDisplay}
     *
     * @return might be null if no timer was configured
     */
    public static @Nullable Component getTimerComponent(@NotNull Sign sign) {
        Duration timerDuration = getTimer(sign, false);

        if (timerDuration != null && timerDuration.toMillis() > 0) {
            final String timeStr = MiscUtils.formatTimeString(timerDuration);

            return Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_TIMER_SIGN,
                Placeholder.unparsed(MessageManager.PlaceHolder.TIME.getPlaceholder(), timeStr));
        } else {
            return null;
        }
    }

    /**
     * set the timer to toggle
     *
     * @param timerDuration       time duration
     * @param shouldUpdateDisplay if the display should get updated. is important to set to false for updating a lock sign from legacy
     */
    public static void setTimer(@NotNull Sign sign, final @NotNull Duration timerDuration, boolean shouldUpdateDisplay) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            Padlock.getPlugin().getLockCacheManager().removeFromCache(sign);
        }

        sign.getPersistentDataContainer().set(timerKey, PersistentDataType.LONG, timerDuration.toMillis());
        sign.update();

        if (shouldUpdateDisplay) {
            SignDisplay.updateDisplay(sign);
        }
    }

    /**
     * get how long it should take for an openable to toggle.
     * Will start the update process if the sign was a legacy sign.
     *
     * @return might be null if no timer was configured
     */
    public static @Nullable Duration getTimer(@NotNull Sign sign, boolean ignoreCache) {
        if (!ignoreCache && Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            return Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(sign.getLocation()).getTimer();
        } else {
            final @Nullable Long persistentMillis = sign.getPersistentDataContainer().get(timerKey, PersistentDataType.LONG);

            if (persistentMillis != null) {
                return Duration.ofMillis(persistentMillis);
            } else {
                Duration timerDuration = getLegacyTimer(sign);

                if (timerDuration != null) {
                    PadlockAPI.updateLegacySign(sign);
                    return timerDuration;
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Read a timer duration from a component line
     * used to parse timer when locking manuel, should never get used for legacy signs!
     *
     * @return might be null, if it does not fit the legacy pattern.
     */
    public static @Nullable Duration getTimerFromComp(@NotNull Component line) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(line).trim();
        Matcher matcher;

        for (Pattern legacyPattern : legacyPatterns) {
            matcher = legacyPattern.matcher(strToTest);

            if (matcher.matches()) {
                return Duration.ofSeconds(Long.parseLong(matcher.group(1)));
            }
        }

        matcher = modernPattern.matcher(strToTest);
        if (matcher.matches()) {

            return MiscUtils.parsePeriod(matcher.group(1));
        } else {
            return null;
        }
    }

    /**
     * update from a legacy timer, purely written on the sign to one in the getPersistentDataContainer.
     * Will not update the Display of the sign afterwarts to not overwrite other unimported data like timers
     */
    @Deprecated(forRemoval = true)
    public static void updateLegacyTimer(@NotNull Sign sign) {
        @Nullable Duration legacyTimer = getLegacyTimer(sign);

        if (legacyTimer != null) {
            setTimer(sign, legacyTimer, false);
        }
    }

    /**
     * get the fist legacy timer duration found on a sign.
     *
     * @return might be null if no fitting timer was found.
     */
    @Deprecated(forRemoval = true)
    private static @Nullable Duration getLegacyTimer(@NotNull Sign sign) {
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            Duration timer = getTimerFromComp(line);

            if (timer != null) {
                return timer;
            }
        }

        return null;
    }

    /**
     * update from a legacy timer, purely written on the lock sign to one in the getPersistentDataContainer.
     */
    public static void updateLegacyTimerFromAdditional(@NotNull Sign lockSign, @NotNull Sign additional) {
        Duration legacyTimer = getLegacyTimer(additional);

        if (legacyTimer != null) {
            setTimer(lockSign, legacyTimer, true);
        }
    }
}
