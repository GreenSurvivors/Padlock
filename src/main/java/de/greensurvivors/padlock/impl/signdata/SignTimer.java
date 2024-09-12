package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.language.LangPath;
import de.greensurvivors.padlock.language.PlaceHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // pretty complex stuff [timer:<timer>] and timer can be any number of digits with their timeunit (t, s, h, d, w or M)
    // optionally delimited by whitespace and commas, all ignoring case.
    // valid lines would be "[timer:2h]", "[TIMER:2D, 3w2t 555s]", "[tiMeR: 100W,,,  -8t]", "[timer:2h5h99h]"
    // invalid lines would be "timer:3d]", "[timer24w]", "[time:0x77Q]", "[banana]", "[timer:34ttt]"
    private final static Pattern modernPattern = Pattern.compile(Padlock.getPlugin().getMessageManager().
        getNakedSignText(LangPath.SIGN_LINE_TIMER_SIGN).replace("[", "\\[(?i)").
        replace("<" + PlaceHolder.TIME.getPlaceholder() + ">",
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

            return Padlock.getPlugin().getMessageManager().getLang(LangPath.SIGN_LINE_TIMER_SIGN,
                Placeholder.unparsed(PlaceHolder.TIME.getPlaceholder(), timeStr));
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
                return null;
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
        matcher = modernPattern.matcher(strToTest);
        if (matcher.matches()) {

            return MiscUtils.parsePeriod(matcher.group(1));
        } else {
            return null;
        }
    }
}
