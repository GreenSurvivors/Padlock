package de.greensurvivors.padlock.impl.signdata;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.dataTypes.CacheSet;
import de.mkammerer.argon2.Argon2Advanced;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Version;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class SignPasswords {
    private final static NamespacedKey passwordHashKey = new NamespacedKey(Padlock.getPlugin(), "passwordHash");

    private final static @NotNull CacheSet<@NotNull UUID> waitForCmdGettingProcessed = new CacheSet<>(Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build());
    private final static @NotNull Map<@NotNull UUID, @NotNull Cache<@NotNull Location, @NotNull Integer>> triesLast3Minutes = new HashMap<>();
    private final static @NotNull Map<@NotNull UUID, CacheSet<@NotNull Location>> accessMap = new HashMap<>();

    private final static int SALT_LENGTH = 16;
    private final static int PARALLELISM = 1;
    private final static int MEMORY = 1 << 14;
    private final static int ITERATIONS = 2;
    private static final Base64.Encoder b64encoder = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder b64decoder = Base64.getDecoder();

    /*
     * Encodes a raw Argon2-hash and its parameters into the standard Argon2-hash-string
     * as specified in the reference implementation
     * (https://github.com/P-H-C/phc-winner-argon2/blob/master/src/encoding.c#L244):
     * <p>
     * {@code $argon2<T>[$v=<num>]$m=<num>,t=<num>,p=<num>$<bin>$<bin>}
     * <p>
     * where {@code <T>} is either 'd', 'id', or 'i', {@code <num>} is a decimal integer
     * (positive, fits in an 'unsigned long'), and {@code <bin>} is Base64-encoded data
     * (no '=' padding characters, no newline or whitespace).
     * <p>
     * The last two binary chunks (encoded in Base64) are, in that order, the salt and the
     * output. If no salt has been used, the salt will be omitted.
     *
     * @param hash       the raw Argon2 hash in binary format
     * @param parameters the Argon2 parameters that were used to create the hash
     * @return the encoded Argon2-hash-string as described above
     * @throws IllegalArgumentException if the Argon2Parameters are invalid
     */
    private static boolean matches(char[] rawPassword, String encodedOtherPassword) {
        Logger logger = Padlock.getPlugin().getLogger();

        String[] parts = encodedOtherPassword.split("\\$");
        if (parts.length >= 4) {
            int currentIndex = 1;
            final Argon2Advanced argon2 = switch (parts[currentIndex++]) {
                case "argon2d" -> Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2d);
                case "argon2i" -> Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2i);
                case "argon2id" -> Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);
                default -> null;
            };
            if (argon2 != null) {
                String currentPart = parts[currentIndex++];
                Argon2Version argon2Version = null;
                if (currentPart.startsWith("v=")) {
                    int expectedVersion = Integer.parseInt(currentPart.substring(2));
                    for (Argon2Version possibleVersion : Argon2Version.values()) {
                        if (expectedVersion == possibleVersion.getVersion()) {
                            argon2Version = possibleVersion;
                        }
                    }

                    if (argon2Version != null) {
                        currentPart = parts[currentIndex++];
                        String[] performanceParams = currentPart.split(",");
                        if (performanceParams.length == 3) {
                            if (performanceParams[0].startsWith("m=")) {
                                final int memory = Integer.parseInt(performanceParams[0].substring(2));

                                if (performanceParams[1].startsWith("t=")) {
                                    final int iterations = Integer.parseInt(performanceParams[1].substring(2));

                                    if (performanceParams[2].startsWith("p=")) {
                                        final int parallelism = Integer.parseInt(performanceParams[2].substring(2));

                                        String one = parts[currentIndex++];
                                        String two = parts[currentIndex];
                                        final byte[] salt = b64decoder.decode(one);
                                        byte[] expectedHash = b64decoder.decode(two);

                                        ByteBuffer buf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(rawPassword));
                                        byte[] encodedPassword = new byte[buf.limit()];
                                        buf.get(encodedPassword);

                                        buf.clear();
                                        clearArray(rawPassword);

                                        return argon2.verifyAdvanced(iterations, memory, parallelism, encodedPassword, salt, null, null, expectedHash.length, argon2Version, expectedHash);
                                    } else {
                                        logger.warning("Invalid parallelity parameter: " + performanceParams[1]);
                                    }
                                } else {
                                    logger.warning("Invalid iterations parameter: " + performanceParams[1]);
                                }
                            } else {
                                logger.warning("Invalid memory parameter: " + performanceParams[0]);
                            }
                        } else {
                            logger.warning("Amount of performance parameters invalid: " + currentPart);
                        }
                    } else {
                        logger.warning("invalid argon2 Version: " + currentPart);
                    }
                } else {
                    logger.warning("invalid argon2 Version: " + currentPart);
                }
            } else {
                logger.warning("Invalid algorithm type: " + parts[1]);
            }
        } else {
            logger.warning("Invalid encoded Argon2-hash: " + encodedOtherPassword);
        }
        return false;
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

    private static void cacheAccess(@NotNull UUID uuid, @NotNull Location location) {
        accessMap.computeIfAbsent(uuid, ignored -> new CacheSet<>(Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(20).build()));
        accessMap.get(uuid).add(location);
    }

    public static boolean hasStillAccess(@NotNull UUID uuid, @NotNull Location location) {
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

    public static boolean needsPasswordAccess(@NotNull Sign sign) {
        final String hash = sign.getPersistentDataContainer().get(passwordHashKey, PersistentDataType.STRING);

        return (hash != null && !hash.isEmpty());
    }

    public static void checkPasswordAndGrandAccess(@NotNull Sign sign, @NotNull Player player, char @NotNull [] password) {
        final String hash = sign.getPersistentDataContainer().get(passwordHashKey, PersistentDataType.STRING);

        if (hash != null) {
            Bukkit.getScheduler().runTaskAsynchronously(Padlock.getPlugin(), () -> {
                boolean doesMatch = matches(password, hash);

                if (doesMatch) {
                    cacheAccess(player.getUniqueId(), sign.getLocation());
                    Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_ACCESS_GRANTED);
                } else {
                    SignPasswords.countTriesUp(player.getUniqueId(), sign.getLocation());
                    Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_WRONG_PASSWORD);
                }
            });
        } else { // no password was set
            Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_ACCESS_GRANTED);
        }

        // yes I know I invalidate the arrays at multiple places, but in terms of password safety it's better to be double and tripple safe then sorry.
        Arrays.fill(password, '*');
    }

    // yes I know I invalidate the arrays at multiple places, but in terms of password safety it's better to be double and tripple safe then sorry.
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

    public static void setPassword(final @NotNull Sign sign, final @NotNull Player player, final char @Nullable [] newPassword) {
        PersistentDataContainer dataContainer = sign.getPersistentDataContainer();

        if (newPassword == null) {
            dataContainer.remove(passwordHashKey);
            sign.update();
            removeAccessOfLoc(sign.getLocation());
            Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SET_PASSWORD_REMOVE_SUCCESS);
            SignPasswords.stopWaiting(player.getUniqueId(), sign.getLocation());

            SignDisplay.updateDisplay(sign);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(Padlock.getPlugin(), () -> {
                // encoding may take a while and slow down the cpu, it's better if we do it async
                String newHash = encode(newPassword);

                Bukkit.getScheduler().runTask(Padlock.getPlugin(), () -> {
                    dataContainer.set(passwordHashKey, PersistentDataType.STRING, newHash);
                    sign.update();
                    removeAccessOfLoc(sign.getLocation());
                    Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SET_PASSWORD_SUCCESS);

                    cacheAccess(player.getUniqueId(), sign.getLocation());
                    SignPasswords.stopWaiting(player.getUniqueId(), sign.getLocation());

                    clearArray(newPassword);

                    SignDisplay.updateDisplay(sign);
                });
            });
        }
    }

    /*
     * Decodes an Argon2 hash string as specified in the reference implementation
     * (https://github.com/P-H-C/phc-winner-argon2/blob/master/src/encoding.c#L244) into
     * the raw hash and the used parameters.
     * <p>
     * The hash has to be formatted as follows:
     * {@code $argon2<T>[$v=<num>]$m=<num>,t=<num>,p=<num>$<bin>$<bin>}
     * <p>
     * where {@code <T>} is either 'd', 'id', or 'i', {@code <num>} is a decimal integer
     * (positive, fits in an 'unsigned long'), and {@code <bin>} is Base64-encoded data
     * (no '=' padding characters, no newline or whitespace).
     * <p>
     * The last two binary chunks (encoded in Base64) are, in that order, the salt and the
     * output. Both are required. The binary salt length and the output length must be in
     * the allowed ranges defined in argon2.h.
     *
     * @param encodedPassword the Argon2 hash string as described above
     *                        {@link Argon2Parameters}.
     * @throws IllegalArgumentException if the encoded hash is malformed
     */
    private static String encode(final char[] password) {
        final Argon2Advanced argon2 = Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);
        final byte[] salt = argon2.generateSalt(SALT_LENGTH);
        final byte[] hash = argon2.rawHash(ITERATIONS, MEMORY, PARALLELISM, password, StandardCharsets.UTF_8, salt);

        clearArray(password);

        return "$argon2id" +
            "$v=" +
            Argon2Version.DEFAULT_VERSION.getVersion() +
            "$m=" +
            MEMORY +
            ",t=" +
            ITERATIONS +
            ",p=" +
            PARALLELISM +
            "$" + b64encoder.encodeToString(salt) +
            "$" + b64encoder.encodeToString(hash); //version
    }
}
