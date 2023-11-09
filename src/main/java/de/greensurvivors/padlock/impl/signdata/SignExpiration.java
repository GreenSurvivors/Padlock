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

public class SignExpiration {
    @Deprecated(forRemoval = true)
    private final static Pattern legacyPattern = Pattern.compile("((?i)#created:)(-?[0-9]+)");
    private final static NamespacedKey createdKey = new NamespacedKey(Padlock.getPlugin(), "timeCreated");

    public static @Nullable Long getCreatedTime(Sign sign) {
        return sign.getPersistentDataContainer().get(createdKey, PersistentDataType.LONG);
    }

    public static void updateWithTime(Sign sign, long epochMilli) {
        sign.getPersistentDataContainer().set(createdKey, PersistentDataType.LONG, epochMilli);
        sign.update();
    }

    public static void updateWithTimeNow(Sign sign, boolean noexpire) {
        sign.getPersistentDataContainer().set(createdKey, PersistentDataType.LONG, noexpire ? -1L : System.currentTimeMillis());
        sign.update();
    }

    @Deprecated(forRemoval = true)
    public static boolean isPrivateTimeLine(@NotNull Component component) {
        String text = PlainTextComponentSerializer.plainText().serialize(component);

        return legacyPattern.matcher(text).matches();
    }

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

    @Deprecated(forRemoval = true)
    public static void updateLegacyTime(@NotNull Sign sign) {
        String text = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(0));
        Matcher matcher = legacyPattern.matcher(text);

        if (matcher.matches()) {
            // note that we already test if it could be a number by matching against our pattern.
            // While the number still could be out of bounds, I'm willing to risk it here. Nothing should break anyway.
            updateWithTime(sign, Long.parseLong(matcher.group(2)));
        }
    }
}
