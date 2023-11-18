package de.greensurvivors.padlock.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.RemovalListener;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.impl.dataTypes.LazySignPropertys;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    Map<Location, LazySignPropertys> lockLazyProps = new HashMap<>();
    private final @NotNull Cache<@NotNull Location, @NotNull LockWrapper> lockStateCache = Caffeine.newBuilder().
            evictionListener((RemovalListener<Location, LockWrapper>) (loc, lockWrapper, cause) -> {
                if (lockWrapper != null) {
                    lockLazyProps.remove(lockWrapper.lockLoc());
                }
            }).expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(500).build();

    /**
     * set a new time after witch the cache should expire; invalidates every already cached value.
     */
    public void setExpirationTime(@NonNegative long duration, @NotNull TimeUnit unit) {
        lockStateCache.invalidateAll();
        Optional<Policy.FixedExpiration<@NotNull Location, @NotNull LockWrapper>> policy = lockStateCache.policy().expireAfterAccess();
        policy.ifPresent(expiration -> expiration.setExpiresAfter(duration, unit));
    }

    public @NotNull LazySignPropertys getProtectedFromCache(@NotNull Location location) {
        LockWrapper lockWrapper = lockStateCache.getIfPresent(location);

        if (lockWrapper != null) {
            if (lockWrapper.lockLoc() != null) {
                LazySignPropertys lazySignPropertys = lockLazyProps.get(lockWrapper.lockLoc());

                if (lazySignPropertys == null) {
                    lazySignPropertys = new LazySignPropertys(PadlockAPI.getLock(lockWrapper.lockLoc().getBlock(), true));

                    lockLazyProps.put(lockWrapper.lockLoc(), lazySignPropertys);
                }
                return lazySignPropertys;
            }
        } else {
            @Nullable Sign lock = PadlockAPI.getLock(location.getBlock(), true);

            if (lock != null) {
                lockStateCache.put(location, new LockWrapper(lock.getLocation()));

                LazySignPropertys lazySignPropertys = lockLazyProps.get(lock);

                if (lazySignPropertys == null) {
                    lazySignPropertys = new LazySignPropertys(lock);
                    lockLazyProps.put(lock.getLocation(), lazySignPropertys);
                }

                return lazySignPropertys;
            } else {
                lockStateCache.put(location, new LockWrapper(null));
            }
        }

        return new LazySignPropertys(null);
    }

    /**
     * reset the cache of a block and the ones next to it as well.
     */
    public void removeFromCache(@NotNull Location location) {
        final LockWrapper signWrapper = lockStateCache.getIfPresent(location);

        if (signWrapper != null && signWrapper.lockLoc() != null) {
            lockStateCache.asMap().entrySet().removeIf(entry -> signWrapper.lockLoc().equals(entry.getValue().lockLoc()));
        }
    }

    public void removeFromCache(@NotNull Sign changedLock) {
        lockStateCache.asMap().entrySet().removeIf(entry -> entry.getValue().lockLoc() != null && changedLock.getLocation().equals(entry.getValue().lockLoc()));
    }

    /**
     * we need a value that represents "cached but still null" in contrast to "not cached yet aka null.
     */
    private record LockWrapper(@Nullable Location lockLoc) {
    }
}