package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignDisplay;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * forces the display (sign text) to update.
 * This way changed usernames can get updated without changing anything on the lock.
 */
public class UpdateDisplay extends SubCommand {

    protected UpdateDisplay(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_UPDATE_DISPLAY.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("updatedisplay");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_UPDATE_DISPLAY);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (checkPermission(sender)) {
                Sign sign = SignSelection.getSelectedSign(player);

                if (sign != null) {
                    //check for old Lockett(Pro) signs and try to update them
                    sign = MainCommand.checkAndUpdateLegacySign(sign, player);
                    if (sign == null) {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        return true;
                    }

                    if (SignLock.isOwner(sign, player.getUniqueId()) || SignLock.isMember(sign, player.getUniqueId()) ||
                        player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm())) {
                        SignDisplay.updateDisplay(sign);
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UPDATE_DISPLAY_SUCCESS);
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_OWNER);
                    }
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
        return null;
    }
}
