package de.greensurvivors.padlock.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.greensurvivors.padlock.PadlockAPI;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * dangerous but fast stuff.
 * This stores cache for faster access than searching for a lock sign.
 * But at the same time this means changes of the lock sign might not get recognized as fast.
 * Why is any of this important? Does the plugin slow the server this much down?
 * Normally not, no. But with many Inventory movement attempts like big hopper contraptions or
 * heavy redstone wire use might be.
 */
public class LockCacheManager {
    // default cache should never get used, but if it does, it has at least a maximum size to not grow unlimited
    private @NotNull Cache<@NotNull Location, @NotNull Boolean> lockStateCache = Caffeine.newBuilder().maximumSize(100).build();

    /**
     * set a new time after witch the cache should expire; invalidates every already cached value.
     */
    public void setExpirationTime(@NonNegative long duration, @NotNull TimeUnit unit) {
        lockStateCache.invalidateAll();
        lockStateCache = Caffeine.newBuilder().expireAfterAccess(duration, unit).build();
    }

    /**
     * tries to get locked status from cache, if it fails,
     * it takes the value from the API and caches it.
     */
    public boolean tryGetProtectedFromCache(@NotNull Block block) {
        Boolean cachedLockState = lockStateCache.getIfPresent(block.getLocation());

        if (cachedLockState == null) {
            boolean newState = PadlockAPI.isProtected(block);
            lockStateCache.put(block.getLocation(), newState);
            return newState;
        } else {
            return cachedLockState;
        }
    }

    /**
     * reset the cache of a block and the ones next to it as well.
     */
    public void resetCache(@NotNull Block block) {
        lockStateCache.invalidate(block.getLocation());

        // if one block changes next to another the chances are high, that the last cache entry might get out of date.
        for (BlockFace blockface : PadlockAPI.cardinalFaces) {
            Block relative = block.getRelative(blockface);

            if (relative.getType() == block.getType()) {
                lockStateCache.invalidate(relative.getLocation());
            }
        }
    }
}