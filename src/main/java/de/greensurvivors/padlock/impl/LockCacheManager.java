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
public class LockCacheManager { //todo use
    // default cache should never get used, but if it does, it has at least a maximum size to not grow unlimited
    Map<Sign, LazySignPropertys> lockLazyProps = new HashMap<>();
    private final @NotNull Cache<@NotNull Location, @NotNull SignWrapper> lockStateCache = Caffeine.newBuilder().
            evictionListener((RemovalListener<Location, SignWrapper>) (loc, lockWrapper, cause) -> {
                if (lockWrapper != null) {
                    lockLazyProps.remove(lockWrapper.lock());
                }
            }).expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(200).build();

    /**
     * set a new time after witch the cache should expire; invalidates every already cached value.
     */
    public void setExpirationTime(@NonNegative long duration, @NotNull TimeUnit unit) {
        lockStateCache.invalidateAll();
        Optional<Policy.FixedExpiration<@NotNull Location, @NotNull SignWrapper>> policy = lockStateCache.policy().expireAfterAccess();
        policy.ifPresent(expiration -> expiration.setExpiresAfter(duration, unit));
    }

    public @NotNull LazySignPropertys getProtectedFromCache(@NotNull Location location) {
        SignWrapper lockWrapper = lockStateCache.getIfPresent(location);

        if (lockWrapper != null) {
            if (lockWrapper.lock() != null) {
                LazySignPropertys lazySignPropertys = lockLazyProps.get(lockWrapper.lock());

                if (lazySignPropertys == null) {
                    lazySignPropertys = new LazySignPropertys(lockWrapper.lock());

                    lockLazyProps.put(lockWrapper.lock(), lazySignPropertys);
                }
                return lazySignPropertys;
            }
        } else {
            @Nullable Sign lock = PadlockAPI.getLock(location.getBlock());
            lockStateCache.put(location, new SignWrapper(lock));

            if (lock != null) {
                LazySignPropertys lazySignPropertys = lockLazyProps.get(lock);

                if (lazySignPropertys != null) {
                    return lazySignPropertys;
                }
            }
        }

        return new LazySignPropertys(null);
    }

    /**
     * reset the cache of a block and the ones next to it as well.
     */
    public void removeFromCache(@NotNull Location location) {
        final SignWrapper signWrapper = lockStateCache.getIfPresent(location);

        if (signWrapper != null && signWrapper.lock() != null) {
            lockStateCache.asMap().entrySet().removeIf(entry -> signWrapper.lock() == entry.getValue().lock());
        }
    }

    public void removeFromCache(@NotNull Sign changedLock) {
        lockStateCache.asMap().entrySet().removeIf(entry -> changedLock == entry.getValue().lock());
    }

    /**
     * we need a value that represents "cached but still null" in contrast to "not cached yet aka null.
     */
    private record SignWrapper(@Nullable Sign lock) {
    }
}