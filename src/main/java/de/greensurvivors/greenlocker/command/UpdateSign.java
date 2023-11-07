package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import de.greensurvivors.greenlocker.impl.SignSelection;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class UpdateSign extends SubCommand {
    protected UpdateSign(@NotNull GreenLocker plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission(PermissionManager.CMD_UPDATE_SIGN.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("updatesign");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_UPDATE_SIGN);
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
            if (this.checkPermission(sender)) {
                Block block = SignSelection.getSelectedSign(player);
                if (block != null) {
                    if (block instanceof Sign sign) {
                        if (GreenLockerAPI.updateLegacySign(sign) != null) {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UPDATE_SIGN_SUCCESS);
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        }
                    } else if (GreenLockerAPI.isLocked(block)) { //something went wrong, try to recover
                        Sign sign = GreenLockerAPI.getLockSign(block);

                        if (sign != null) {
                            if (GreenLockerAPI.updateLegacySign(sign) != null) {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UPDATE_SIGN_SUCCESS);
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
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
        return null;
    }
}
