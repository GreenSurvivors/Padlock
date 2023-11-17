package de.greensurvivors.padlock.impl.signdata;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.dataTypes.CacheSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class SignPasswords {
    private final static NamespacedKey passwordHashKey = new NamespacedKey(Padlock.getPlugin(), "passwordHash");
    private final static Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    private final static @NotNull CacheSet<@NotNull UUID> waitForCmdGettingProcessed = new CacheSet<>(Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build());
    private final static @NotNull Map<@NotNull UUID, @NotNull Cache<@NotNull Location, @NotNull Integer>> triesLast3Minutes = new HashMap<>();
    private final static @NotNull Map<@NotNull UUID, CacheSet<@NotNull Location>> accessMap = new HashMap<>();

    public static void setWaitForCmdGettingProcessed(@NotNull UUID uuid) {
        waitForCmdGettingProcessed.add(uuid);
    }

    private static void stopWaiting(@NotNull UUID uuid, @NotNull Location location) {
        Cache<@NotNull Location, @NotNull Integer> triesAtLocations = triesLast3Minutes.get(uuid);

        if (triesAtLocations != null) {
            triesAtLocations.invalidate(location);
            triesAtLocations.cleanUp();

            if (triesAtLocations.estimatedSize() <= 0) {
                triesLast3Minutes.remove(uuid);
            }
        }

        waitForCmdGettingProcessed.remove(uuid);
    }

    public static boolean isOnCooldown(@NotNull UUID uuid, @NotNull Location location) {
        if (waitForCmdGettingProcessed.contains(uuid)) {
            Cache<@NotNull Location, @NotNull Integer> triesAtLocations = triesLast3Minutes.get(uuid);

            if (triesAtLocations != null) {
                Integer tries = triesAtLocations.getIfPresent(location);

                if (tries != null) {
                    return tries > 10;
                }
            }
        }
        return false;
    }

    public static void countTriesUp(@NotNull UUID uuid, @NotNull Location location) {
        Cache<@NotNull Location, @NotNull Integer> triesAtLocations = triesLast3Minutes.get(uuid);

        if (triesAtLocations != null) {
            Integer tries = triesAtLocations.getIfPresent(location);

            if (tries != null) {
                tries++;

                triesAtLocations.put(location, tries);
            } else {
                triesAtLocations.put(location, 1);
            }
        } else {
            Cache<Location, Integer> newTries = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();
            triesLast3Minutes.put(uuid, newTries);
        }
    }

    private static void cacheAccess(UUID uuid, Location location) {
        accessMap.computeIfAbsent(uuid, ignored -> new CacheSet<>(Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(20).build()));
        accessMap.get(uuid).add(location);
    }

    public static boolean hasStillAccess(UUID uuid, Location location) {
        CacheSet<Location> cache = accessMap.get(uuid);

        if (cache != null) {
            if (cache.isEmpty()) {
                accessMap.remove(uuid);
            } else {
                return cache.contains(location);
            }
        }

        return false;
    }

    public static boolean needsPasswordAccess(Sign sign) {
        final String hash = sign.getPersistentDataContainer().get(passwordHashKey, PersistentDataType.STRING);

        return (hash != null && !hash.isEmpty());
    }

    public static void checkPasswordAndGrandAccess(@NotNull Sign sign, @NotNull Player player, char @NotNull [] password) {
        final String hash = sign.getPersistentDataContainer().get(passwordHashKey, PersistentDataType.STRING);

        Bukkit.getScheduler().runTaskAsynchronously(Padlock.getPlugin(), () -> {
            boolean doesMatch = encoder.matches(String.valueOf(password), hash);

            if (doesMatch) {
                cacheAccess(player.getUniqueId(), sign.getLocation());
                Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_ACCESS_GRANTED);
            } else {
                SignPasswords.countTriesUp(player.getUniqueId(), sign.getLocation());
                Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_WRONG_PASSWORD);
            }
        });
        Arrays.fill(password, '*');
    }

    private static void clearArray(char @Nullable [] newPassword) {
        if (newPassword != null) {
            Arrays.fill(newPassword, '*');
        }
    }

    public static void removeAccessOfLoc(@NotNull Location location) {
        for (CacheSet<Location> cache : accessMap.values()) {
            cache.remove(location);
        }
    }

    public static void setPassword(@NotNull Sign sign, @NotNull Player player, char @Nullable [] newPassword) {
        PersistentDataContainer dataContainer = sign.getPersistentDataContainer();

        if (newPassword == null) {
            dataContainer.remove(passwordHashKey);
            sign.update();
            removeAccessOfLoc(sign.getLocation());
            Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SET_PASSWORD_REMOVE_SUCCESS);
            SignPasswords.stopWaiting(player.getUniqueId(), sign.getLocation());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(Padlock.getPlugin(), () -> {
                // encoding may take a while and slow down the cpu, it's better if we do it async
                String newHash = encoder.encode(new String(newPassword));

                Bukkit.getScheduler().runTask(Padlock.getPlugin(), () -> {
                    dataContainer.set(passwordHashKey, PersistentDataType.STRING, newHash);
                    sign.update();
                    removeAccessOfLoc(sign.getLocation());
                    Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SET_PASSWORD_SUCCESS);

                    cacheAccess(player.getUniqueId(), sign.getLocation());
                    SignPasswords.stopWaiting(player.getUniqueId(), sign.getLocation());

                    clearArray(newPassword);
                });
            });
        }
    }
}
