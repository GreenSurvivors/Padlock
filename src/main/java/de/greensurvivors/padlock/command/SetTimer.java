package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * set the timer a door/gate/trapdoor should toggle after used
 */
public class SetTimer extends SubCommand {
    private static final @NotNull Pattern periodPattern = Pattern.compile("(-?[0-9]+)([tTsSmhHdDwWM])");
    private static final @NotNull Pattern numberEndPattern = Pattern.compile("^*?\\d$");

    protected SetTimer(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_SET_TIMER.getPerm());
    }

    /**
     * Try to get a time period of a string.
     * First try ISO-8601 duration, and afterward our own implementation
     * using the same time unit more than once is permitted.
     *
     * @return the duration in milliseconds, or zero if not possible
     */
    private long parsePeriod(@NotNull String period) {
        try { //try Iso
            return Duration.parse(period).toMillis();
        } catch (DateTimeParseException e) {
            plugin.getLogger().log(Level.FINE, "Couldn't get time period \"" + period + "\" as duration. Trying to parse manual next.", e);
        }

        Matcher matcher = periodPattern.matcher(period);
        long millis = 0;

        while (matcher.find()) {
            try {
                long num = Long.parseLong(matcher.group(1));
                String typ = matcher.group(2);
                millis += switch (typ) { // from periodPattern
                    case "t", "T" -> Math.round(50D * num); // ticks
                    case "s" -> TimeUnit.SECONDS.toMillis(num);
                    case "m" -> TimeUnit.MINUTES.toMillis(num);
                    case "h", "H" -> TimeUnit.HOURS.toMillis(num);
                    case "d", "D" -> TimeUnit.DAYS.toMillis(num);
                    case "w", "W" -> TimeUnit.DAYS.toMillis(Period.ofWeeks((int) num).getDays());
                    case "M" -> TimeUnit.DAYS.toMillis(Period.ofMonths((int) num).getDays());
                    default -> 0;
                };
            } catch (NumberFormatException e) {
                plugin.getLogger().log(Level.WARNING, "Couldn't get time period for " + period, e);
            }
        }
        return millis;
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("settimer", "timer");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SETTIMER);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (sender.hasPermission(PermissionManager.CMD_SET_CREATED.getPerm())) {
                if (args.length >= 2) {
                    Sign sign = SignSelection.getSelectedSign(player);

                    if (sign != null) {
                        //check for old Lockett(Pro) signs and try to update them
                        sign = Command.checkAndUpdateLegacySign(sign, player);
                        if (sign == null) {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                            return true;
                        }

                        // only owners and admins can change a signs properties
                        if (SignLock.isOwner(sign, player.getUniqueId()) ||
                                player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm())) {
                            // get all durations of all arguments together
                            // note: writing every time element in one argument,
                            // would have the same effect as spreading them across multiple arguments.
                            // using the same time unit more than once is permitted.
                            long timerDuration = 0;
                            for (int i = 1; i < args.length; i++) {
                                timerDuration += parsePeriod(args[i]);
                            }

                            if (timerDuration != 0) {
                                // success
                                SignTimer.setTimer(sign, timerDuration);

                                if (timerDuration > 0) {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_SUCCESS_ON,
                                            Placeholder.component(MessageManager.PlaceHolder.TIME.getPlaceholder(), Component.text(timerDuration)));
                                } else {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_SUCCESS_OFF);
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_ERROR);
                                return false;
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_OWNER);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_ENOUGH_ARGS);
                    return false;
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
            }

            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
            return false;
        }
    }

    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (this.checkPermission(sender)) {
            String arg = args[args.length - 1];
            List<String> result = new ArrayList<>();

            if (numberEndPattern.matcher(arg).matches()) {
                // add all distinct options from periodPattern
                result.addAll(List.of("t", "s", "m", "h", "d", "w", "M"));
            }

            for (int i = 0; i <= 9; i++) {
                result.add(String.valueOf(i));
            }

            return result.stream().map(str -> arg + str).toList();
        } else {
            return null;
        }
    }
}
