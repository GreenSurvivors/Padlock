package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.impl.MiscUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * This handles the time a sign was created at, and therefore,
 * if enabled in the config, if the lock already expired.
 */
public class SignExpiration {
    /**
     * get the time, in millis since Epoch, a signs owner was seen last
     * or -1 if this isn't a lock sign.
     */
    public static long getLastUsed(@NotNull Sign sign) {
        if (PadlockAPI.isLockSign(sign)) {
            long lastUsed = -1;

            for (OfflinePlayer offlinePlayer : MiscUtils.getPlayersFromUUIDStrings(SignLock.getUUIDs(sign, true, true))) {
                // removed check for no expire permission, since it could slow down main thread.

                lastUsed = Math.max(offlinePlayer.getLastSeen(), lastUsed);
            }

            if (lastUsed <= 0) {
                // also didn't work. Use default.
                lastUsed = System.currentTimeMillis();
            }

            return lastUsed;
        } else {
            return -1;
        }
    }

    /**
     * get if a sign is already expired. If no data was found check if this might be a legacy lockette sign
     * and start the update process. If this also isn't true, get the default time from config.
     */
    public static boolean isSignExpired(@NotNull Sign sign) {
        long lasUsed = getLastUsed(sign);

        return (System.currentTimeMillis() > lasUsed + Duration.ofDays(Padlock.getPlugin().getConfigManager().getLockExpireDays()).toMillis());
    }
}
