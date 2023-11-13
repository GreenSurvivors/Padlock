package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This handles the time a sign was created at, and therefore,
 * if enabled in the config, if the lock already expired.
 */
public class SignExpiration {
    @Deprecated(forRemoval = true)
    private final static Pattern legacyPattern = Pattern.compile("((?i)#created:)(-?[0-9]+)");
    private final static NamespacedKey createdKey = new NamespacedKey(Padlock.getPlugin(), "timeCreated");

    /**
     * get the time, in millis since Epoch, a sign was created at
     * or null if expiration wasn't enabled at time of its creation
     */
    public static @Nullable Long getCreatedTime(Sign sign) {
        return sign.getPersistentDataContainer().get(createdKey, PersistentDataType.LONG);
    }

    /**
     * update the time this sign was "created" with a new time
     *
     * @param sign                to update
     * @param epochMilli          milliseconds since Epoch
     * @param shouldUpdateDisplay if the display should get updated. is important to set to false for updating a lock sign from legacy
     */
    public static void updateWithTime(Sign sign, long epochMilli, boolean shouldUpdateDisplay) {
        sign.getPersistentDataContainer().set(createdKey, PersistentDataType.LONG, epochMilli);
        sign.update();

        if (shouldUpdateDisplay) {
            SignDisplay.updateDisplay(sign);
        }
    }

    /**
     * update the time this sign was created with the time now or disables expiation for this sign
     *
     * @param sign     the sign to update
     * @param noexpire if this sign should never expire
     */
    public static void updateWithTimeNow(Sign sign, boolean noexpire) {
        sign.getPersistentDataContainer().set(createdKey, PersistentDataType.LONG, noexpire ? -1L : System.currentTimeMillis());
        sign.update();
    }

    /**
     * Lockette saved this information behind the [private] line. Checks if this is one of them
     */
    @Deprecated(forRemoval = true)
    private static boolean isPrivateTimeLine(@NotNull Component component) {
        String text = PlainTextComponentSerializer.plainText().serialize(component);

        return legacyPattern.matcher(text).matches();
    }

    /**
     * get if a sign is already expired. If no data was found check if this might be a legacy lockette sign
     * and start the update process. If this also isn't true, get the default time from config.
     */
    public static boolean isSignExpired(Sign sign) {
        if (PadlockAPI.isLockSign(sign)) {
            Long createdTime = sign.getPersistentDataContainer().get(createdKey, PersistentDataType.LONG);

            if (createdTime == null) {
                // couldn't get created time stamp. is it legacy?
                if (isPrivateTimeLine(sign.getSide(Side.FRONT).line(0))) {
                    PadlockAPI.updateLegacySign(sign);
                    return isSignExpired(sign);
                } else { // nope, it's just missing. Use default
                    //todo maybe work with coreProtect together if dependency was enabled
                    createdTime = Padlock.getPlugin().getConfigManager().getLockDefaultCreateTimeEpoch();
                }
            }

            return (createdTime > 0 && System.currentTimeMillis() > createdTime + Duration.ofDays(Padlock.getPlugin().getConfigManager().getLockExpireDays()).toMillis());
        } else {
            return false;
        }
    }

    /**
     * update a lock sign from lockette times to modern padlock data storage
     * Will not update the Display of the sign afterwarts to not overwrite other unimported data like everyone
     */
    @Deprecated(forRemoval = true)
    public static void updateLegacyTime(@NotNull Sign sign) {
        String text = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(0));
        Matcher matcher = legacyPattern.matcher(text);

        if (matcher.matches()) {
            // note that we already test if it could be a number by matching against our pattern.
            // While the number still could be out of bounds, I'm willing to risk it here. Nothing should break anyway.
            updateWithTime(sign, Long.parseLong(matcher.group(2)), false);
        }
    }

    /**
     * update a additional sign from lockette times to modern padlock data storage
     */
    public static void updateLegacyTimeFromAdditional(Sign lockSign, Sign additional) {
        String text = PlainTextComponentSerializer.plainText().serialize(lockSign.getSide(Side.FRONT).line(0));
        Matcher matcher = legacyPattern.matcher(text);

        if (matcher.matches()) {
            // note that we already test if it could be a number by matching against our pattern.
            // While the number still could be out of bounds, I'm willing to risk it here. Nothing should break anyway.
            updateWithTime(additional, Long.parseLong(matcher.group(2)), true);
        }
    }
}
