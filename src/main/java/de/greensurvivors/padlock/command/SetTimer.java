package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * set the timer a door/gate/trapdoor should toggle after used
 */
public class SetTimer extends SubCommand {
    private static final @NotNull Pattern numberEndPattern = Pattern.compile("^*?\\d$");

    protected SetTimer(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_SET_TIMER.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("settimer", "timer");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SET_TIMER);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (checkPermission(sender)) {
                if (args.length >= 2) {
                    Sign sign = SignSelection.getSelectedSign(player);

                    if (sign != null) {
                        //check for old Lockett(Pro) signs and try to update them
                        sign = MainCommand.checkAndUpdateLegacySign(sign, player);
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
                            Long timerDuration = null;

                            if (args.length == 2) {
                                try {
                                    if (Integer.parseInt(args[1]) <= 0) {
                                        timerDuration = -1L;
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }

                            if (timerDuration == null) {
                                for (int i = 1; i < args.length; i++) {
                                    Long period = MiscUtils.parsePeriod(args[i]);

                                    if (period != null) {
                                        if (timerDuration == null) {
                                            timerDuration = 0L;
                                        }

                                        timerDuration += period;
                                    } else {
                                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_ERROR);
                                        return false;
                                    }
                                }
                            }

                            // success
                            SignTimer.setTimer(sign, timerDuration, true);

                            if (timerDuration > 0) {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_SUCCESS_ON,
                                        Placeholder.component(MessageManager.PlaceHolder.TIME.getPlaceholder(), Component.text(timerDuration)));
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_TIMER_SUCCESS_OFF);
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
