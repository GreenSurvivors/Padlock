package de.greensurvivors.padlock.listener;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.command.Password;
import de.greensurvivors.padlock.command.SetPassword;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public final class ChatPlayerListener implements Listener {
    private static final Set<String> mainCmdStrings = new HashSet<>();
    private static final Set<String> pwCmdStrings = new HashSet<>();
    private final Padlock plugin;

    public ChatPlayerListener(Padlock plugin) {
        this.plugin = plugin;

        // get all possible combinations of all command aliases with all subcommand aliases
        final Set<String> cmdAliases = new HashSet<>();
        for (Map.Entry<String, Map<String, Object>> cmdEntry : plugin.getDescription().getCommands().entrySet()) {
            cmdAliases.add(cmdEntry.getKey().toLowerCase(Locale.ENGLISH));
            Object objectAliases = cmdEntry.getValue().get("aliases");

            if (Objects.requireNonNull(objectAliases) instanceof String string) {
                cmdAliases.add(string);
            } else if (objectAliases instanceof List<?> list) {
                for (Object listEntry : list) {
                    if (listEntry instanceof String string) {
                        cmdAliases.add(string);
                    } else {
                        plugin.getLogger().warning("Couldn't get alias " + listEntry + " as String. Ignoring. Might risk posting passwords to chat tho");
                    }
                }
            } else {
                plugin.getLogger().warning("Couldn't get aliases " + objectAliases + ". Ignoring. Might risk posting passwords to chat tho");
            }
        }
        for (String cmdStr : cmdAliases) {
            for (String subCmdStr : SetPassword.getAliasesStatic()) {
                mainCmdStrings.add(cmdStr + " " + subCmdStr + " ");
            }
        }
        for (String cmdStr : cmdAliases) {
            for (String subCmdStr : Password.getAliasesStatic()) {
                pwCmdStrings.add(cmdStr + " " + subCmdStr + " ");
            }
        }
        for (String cmdStr : Password.getAliasesStatic()) {
            pwCmdStrings.add(cmdStr + " ");
        }

        // set our custom filter
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addFilter(new CmdFilter());
    }

    /**
     * We want to be there if possible as the first, so we can cancel the event as fast as possible,
     * if it could contain a password
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    private void onChat(@NotNull PlayerCommandPreprocessEvent event) {
        String text = event.getMessage();
        //Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        for (String cmdToCheck : mainCmdStrings) {
            if (text.regionMatches(true, 1, cmdToCheck, 0, cmdToCheck.length())) {
                // make room so chat event can roll through, we can wait until next circle
                Bukkit.getScheduler().runTask(plugin, () -> {
                    final char[] newPassword = text.substring(cmdToCheck.length() + 1).toCharArray();
                    SetPassword.onExternalCommand(newPassword, event.getPlayer());

                    //invalidate char array
                    Arrays.fill(newPassword, '*');
                });

                event.setMessage(text.substring(0, cmdToCheck.length() + 1) + "**********");
                plugin.getLogger().info(event.getPlayer().getName() + " issued sub command: setpassword.");
                break;
            }
        }

        for (String cmdToCheck : pwCmdStrings) {
            if (text.regionMatches(true, 1, cmdToCheck, 0, cmdToCheck.length())) {
                // make room so chat event can roll through, we can wait until next circle
                Bukkit.getScheduler().runTask(plugin, () -> {
                    final char[] password = text.substring(cmdToCheck.length() + 1).toCharArray();
                    Password.onExternalCommand(password, event.getPlayer());

                    //invalidate char array
                    Arrays.fill(password, '*');
                });

                event.setMessage(text.substring(0, cmdToCheck.length() + 1) + "**********");
                plugin.getLogger().info(event.getPlayer().getName() + " issued sub command: password.");
                break;
            }
        }
    }

    /**
     * This filters the log of the Server to not write any passwords and accidentally keep them in the logs.
     */
    private static class CmdFilter extends AbstractFilter {
        private CmdFilter() {
        }

        @Override
        public Result filter(final LogEvent event) {
            return event == null ? Result.NEUTRAL : logResult(event.getMessage().getFormattedMessage());
        }

        @Override
        public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                             final Throwable t) {
            Result result = t != null ? logResult(t.getMessage()) : Result.NEUTRAL;
            if (msg != null) {
                if (result == Result.DENY) return result;
                return logResult(msg.getFormattedMessage());
            }
            return Result.NEUTRAL;
        }

        public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                             final Throwable t) {
            Result result = t != null ? logResult(t.getMessage()) : Result.NEUTRAL;
            if (msg != null) {
                if (result == Result.DENY) return result;
                return logResult(msg.toString());
            }
            return Result.NEUTRAL;
        }

        public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                             final Object... params) {
            return logResult(msg);
        }


        public Result logResult(String text) {
            for (String cmdToCheck : mainCmdStrings) {
                int index = StringUtils.indexOfIgnoreCase(text, cmdToCheck);

                if (index > 0) {
                    return Result.DENY;
                }
            }

            for (String cmdToCheck : pwCmdStrings) {
                int index = StringUtils.indexOfIgnoreCase(text, cmdToCheck);

                if (index > 0) {
                    return Result.DENY;
                }
            }

            return Result.NEUTRAL;
        }
    }
}
