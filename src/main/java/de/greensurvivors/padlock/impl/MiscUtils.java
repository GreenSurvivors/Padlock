package de.greensurvivors.padlock.impl;

import de.greensurvivors.padlock.Padlock;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitys that don't fit anywhere else
 */
public class MiscUtils {
    private static final @NotNull Pattern periodPattern = Pattern.compile("(-?[0-9]+)([tTsSmhHdDwWM])");
    /**
     * Set containing all players that have been notified about being able to lock things
     */
    private static final Set<UUID> notified = new HashSet<>();
    // the point at the beginning is for bedrock player if the proxy supports them.
    // please note, even though very few but there are valid accounts that doesn't match, from very early days and exploits.
    // If it comes down to it, we have to try UUID from string / or check Bukkit.getOfflinePlayer(<str>).hasPlayedBefore()
    // Since this is just a brute force methode I will it only change it, if a problem ever gets reported.
    private static Pattern usernamePattern = Pattern.compile("^.?[a-zA-Z0-9_]{3,16}$");

    public static @NotNull Pattern getPeriodPattern() {
        return periodPattern;
    }

    /**
     * removes one item of the players main hand.
     */
    public static void removeAItemMainHand(@NotNull Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (player.getInventory().getItemInMainHand().getAmount() == 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        }
    }

    /**
     * checks if a player has not been notified before and adds them to the notified list if not
     */
    public static boolean shouldNotify(@NotNull Player player) {
        return notified.add(player.getUniqueId());
    }

    /**
     * check if a string is a valid username
     */
    public static boolean isUserName(@NotNull String text) {
        return usernamePattern.matcher(text).matches();
    }

    /**
     * get the direction a block is facing or null if not directional
     */
    private static @Nullable BlockFace getFacing(@NotNull Block block) {
        if (block.getBlockData() instanceof Directional directional) {
            return directional.getFacing();
        } else {
            return null;
        }
    }

    /**
     * get a sign relative in direction of the blockface facing the same direction.
     * or null if not found / wrong state
     */
    public static @Nullable Sign getFacingSign(@NotNull Block block, @NotNull BlockFace blockface) {
        Block relativeblock = block.getRelative(blockface);

        if (relativeblock.getState() instanceof Sign sign && getFacing(relativeblock) == blockface) {
            return sign;
        } else {
            return null;
        }
    }

    /**
     * Set the new userNamePattern, to account for other settings in the proxy for bedrock players
     */
    public static void setBedrockPrefix(@NotNull String bedrockPrefix) {
        usernamePattern = Pattern.compile("^" + bedrockPrefix + "?[a-zA-Z0-9_]{3,16}$");
    }

    public static @NotNull Set<@NotNull OfflinePlayer> getPlayersFromUUIDStrings(@NotNull Set<@NotNull String> uuidStrs) {
        Set<OfflinePlayer> result = new HashSet<>();

        for (String uuidStr : uuidStrs) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                result.add(Bukkit.getOfflinePlayer(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }

    /**
     * Try to get a member of the enum given as an argument by the name
     *
     * @param enumName  name of the enum to find
     * @param enumClass the enum to check
     * @param <E>       the type of the enum to check
     * @return the member of the enum to check
     */
    public static @Nullable <E extends Enum<E>> E getEnum(final @NotNull Class<E> enumClass, final @NotNull String enumName) {
        try {
            return Enum.valueOf(enumClass, enumName.toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Try to get a time period of a string.
     * First try ISO-8601 duration, and afterward our own implementation
     * using the same time unit more than once is permitted.
     *
     * @return the duration in milliseconds, or null if not possible
     */
    public static @Nullable Long parsePeriod(@NotNull String period) {

        try { //try Iso
            return Duration.parse(period).toMillis();
        } catch (DateTimeParseException e) {
            Padlock.getPlugin().getLogger().log(Level.FINE, "Couldn't get time period \"" + period + "\" as duration. Trying to parse manual next.", e);
        }

        Matcher matcher = periodPattern.matcher(period);
        Long millis = null;

        while (matcher.find()) {
            // we got a match.
            if (millis == null) {
                millis = 0L;
            }

            try {
                long num = Long.parseLong(matcher.group(1));
                String typ = matcher.group(2);
                millis += switch (typ) { // from periodPattern
                    case "t", "T" -> Math.round(50D * num); // ticks
                    case "s", "S" -> TimeUnit.SECONDS.toMillis(num);
                    case "m" -> TimeUnit.MINUTES.toMillis(num);
                    case "h", "H" -> TimeUnit.HOURS.toMillis(num);
                    case "d", "D" -> TimeUnit.DAYS.toMillis(num);
                    case "w", "W" -> TimeUnit.DAYS.toMillis(Period.ofWeeks((int) num).getDays());
                    case "M" -> TimeUnit.DAYS.toMillis(Period.ofMonths((int) num).getDays());
                    default -> 0;
                };
            } catch (NumberFormatException e) {
                Padlock.getPlugin().getLogger().log(Level.WARNING, "Couldn't get time period for " + period, e);
            }
        }
        return millis;
    }

    public static @NotNull String formatTimeString(final @NotNull Duration duration) {
        String timeStr = "";

        final long days = duration.toDaysPart();
        if (days != 0) {
            timeStr += days + "d";
        }

        final int hours = duration.toHoursPart();
        if (hours != 0) {
            timeStr += hours + "h";
        }

        final int minutes = duration.toMinutesPart();
        if (minutes != 0) {
            timeStr += minutes + "m";
        }

        final int seconds = duration.toSecondsPart();
        if (seconds != 0) {
            timeStr += seconds + "s";
        }

        final int ticks = duration.toMillisPart() / 50;
        if (ticks != 0) {
            timeStr += ticks + "t";
        }
        return timeStr;
    }
}
