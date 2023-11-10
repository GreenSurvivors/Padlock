package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
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

/**
 * updates a selected legacy or additional sign.
 */
@Deprecated(forRemoval = true)
public class UpdateSign extends SubCommand {
    protected UpdateSign(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_UPDATE_SIGN.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("updatesign");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_UPDATE_SIGN);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (this.checkPermission(sender)) {
            if (sender instanceof Player player) {
                Sign sign = SignSelection.getSelectedSign(player);
                if (sign != null) {
                    if (PadlockAPI.updateLegacySign(sign) != null) {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UPDATE_SIGN_SUCCESS);
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
                return false;
            }
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
        }

        return true;
    }

    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return null;
    }
}
