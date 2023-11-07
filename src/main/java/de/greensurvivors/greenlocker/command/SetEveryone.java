package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import de.greensurvivors.greenlocker.impl.SignSelection;
import de.greensurvivors.greenlocker.impl.signdata.EveryoneSign;
import de.greensurvivors.greenlocker.impl.signdata.SignLock;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.BooleanUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class SetEveryone extends SubCommand {
    protected SetEveryone(@NotNull GreenLocker plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission(PermissionManager.CMD_SET_EVERYONE.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("seteveryone", "everyone");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SETEVERYONE);
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

                            Boolean setting = BooleanUtils.toBooleanObject(args[1]);

                            if (setting != null) {
                                EveryoneSign.setEveryone(sign, setting);
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_EVERYONE_SUCCESS);
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_EVERYONE_ERROR);
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
        if (args.length == 2) {
            return List.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());
        } else {
            return null;
        }
    }
}
