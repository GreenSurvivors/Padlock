package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import de.greensurvivors.greenlocker.impl.SignSelection;
import de.greensurvivors.greenlocker.impl.signdata.SignLock;
import de.greensurvivors.greenlocker.impl.signdata.SignTimer;
import net.kyori.adventure.text.Component;
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

public class SetTimer extends SubCommand {
    private static final @NotNull Pattern periodPattern = Pattern.compile("(-?[0-9]+)([sSmhHdDwWM])");
    private static final @NotNull Pattern numberEndPattern = Pattern.compile("-?[0-9]+$");

    protected SetTimer(@NotNull GreenLocker plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission(PermissionManager.CMD_SET_TIMER.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("settimer", "timer");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SETTIMER);
    }

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
                millis += switch (typ) {
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

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender Source of the command
     * @param args   Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) { //todo check if owner or admin
        if (sender instanceof Player player) {
            if (sender.hasPermission(PermissionManager.CMD_SET_CREATED.getPerm())) {
                if (args.length >= 2) {
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block.getState() instanceof Sign sign) {
                            if (GreenLockerAPI.isAdditionalSign(sign) || SignLock.isLegacySign(sign)) {
                                Sign otherSign = GreenLockerAPI.updateLegacySign(sign); //get main sign

                                if (otherSign == null) {
                                    GreenLockerAPI.setInvalid(sign);
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                                    return true;
                                } else {
                                    sign = otherSign;
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UPDATE_SIGN_SUCCESS);
                                }
                            }

                            long timerDuration = 0;
                            for (int i = 1; i < args.length; i++) {
                                timerDuration += parsePeriod(args[i]);
                            }

                            if (timerDuration != 0) {
                                SignTimer.setTimer(sign, timerDuration);

                                if (timerDuration > 0) {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_SUCCESS_ON);
                                } else {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_SUCCESS_OFF);
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_ERROR);
                                return false;
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_ENOUGH_ARGS);
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

    /**
     * Requests a list of possible completions for a command argument.
     * Please Note: The subcommand will ALWAYS be the first argument aka arg[0].
     *
     * @param sender Source of the command.  For players tab-completing a
     *               command inside of a command block, this will be the player, not
     *               the command block.
     * @param args   The arguments passed to the command, including final
     *               partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        String arg = args[args.length - 1];
        List<String> result = new ArrayList<>();

        if (periodPattern.matcher(arg).matches()) {
            if (numberEndPattern.matcher(arg).matches()) {
                result.addAll(List.of("s", "m", "h", "d", "w", "M"));
            }

            for (int i = 0; i <= 9; i++) {
                result.add(String.valueOf(i));
            }

            return result;
        }

        return null;
    }
}
