package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignConnectedOpenable;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.BooleanUtils;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class SetConnected extends SubCommand {

    protected SetConnected(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_SET_CONNECTED.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("setconnected", "setc");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SET_CONNECTED);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (this.checkPermission(sender)) {
            if (sender instanceof Player player) {
                if (args.length >= 2) {
                    Sign sign = SignSelection.getSelectedSign(player);

                    if (sign != null) {
                        //check for old Lockett(Pro) signs and try to update them
                        sign = MainCommand.checkAndUpdateLegacySign(sign, player);
                        if (sign == null) {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                            return true;
                        }

                        // only admins and owners can change a signs properties
                        if (SignLock.isOwner(sign, player.getUniqueId()) ||
                                player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm())) {
                            // get and check type from arg

                            Boolean shouldConnected = BooleanUtils.toBooleanObject(args[1]);

                            if (shouldConnected != null) {
                                // success!
                                SignConnectedOpenable.setConnected(sign, shouldConnected);
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_CONNECTED_SUCCESS,
                                        Placeholder.component(MessageManager.PlaceHolder.ARGUMENT.getPlaceholder(), Component.text(shouldConnected)));
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_BOOL,
                                        Placeholder.unparsed(MessageManager.PlaceHolder.ARGUMENT.getPlaceholder(), args[1]));
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
        if (this.checkPermission(sender) && args.length == 2) {
            return List.of(Boolean.toString(true), Boolean.toString(false));
        } else {
            return null;
        }
    }
}
