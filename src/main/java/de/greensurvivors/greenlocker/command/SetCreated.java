package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import de.greensurvivors.greenlocker.impl.SignSelection;
import de.greensurvivors.greenlocker.impl.signdata.SignExpiration;
import de.greensurvivors.greenlocker.impl.signdata.SignLock;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.List;
import java.util.Set;

public class SetCreated extends SubCommand {
    private final static DateTimeFormatter[] supportedFormats = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_TIME,
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("dd.MM.yyyy['T'HH:mm[:ss]][zzzz]")
    };

    protected SetCreated(@NotNull GreenLocker plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission(PermissionManager.CMD_SET_CREATED.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("setCreated", "created");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SETCREATED);
    }

    private @Nullable Long getEpochMillisFromString(@NotNull String string) {
        for (DateTimeFormatter formatter : supportedFormats) {
            try {
                TemporalAccessor temporalAccessor = formatter.parse(string);

                ZoneId zoneId = null;
                try {
                    zoneId = temporalAccessor.query(TemporalQueries.zone());
                } catch (DateTimeException ignored) {
                }

                if (zoneId == null) {
                    zoneId = ZoneId.systemDefault();
                }

                LocalDateTime localDateTime = formatter.parse("", LocalDateTime::from);
                return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
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
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
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

                            Long millisEpoch = getEpochMillisFromString(args[1]);

                            if (millisEpoch != null) {
                                SignExpiration.updateWithTime(sign, millisEpoch);

                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_CREATED_SUCCESS);
                                return true;
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_CREATED_ERROR);
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
        return null; //todo
    }
}
