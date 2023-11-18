package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignAccessType;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SetAccessType extends SubCommand {
    protected SetAccessType(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_SET_ACCESS_TYPE.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("setaccesstype", "settype", "setaccess", "seta", "sa", "sat");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SET_ACCESS_TYPE);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (this.checkPermission(sender)) {
            if (sender instanceof Player player) {
                if (args.length >= 2) {
                    Sign sign = SignSelection.getSelectedSign(player);

                    if (sign != null) {
                        //check for old Lockett(Pro) signs and try to update them
                        sign = Command.checkAndUpdateLegacySign(sign, player);
                        if (sign == null) {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                            return true;
                        }

                        // only admins and owners can change a signs properties
                        if (SignLock.isOwner(sign, player.getUniqueId()) ||
                                player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm())) {
                            // get and check type from arg
                            //todo lang mapping
                            SignAccessType.AccessType accessType = MiscUtils.getEnum(SignAccessType.AccessType.class, args[1]);

                            if (accessType != null) {
                                // success!
                                SignAccessType.setAccessType(sign, accessType, true);
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SET_ACCESS_TYPE_SUCCESS,
                                        Placeholder.component(MessageManager.PlaceHolder.ARGUMENT.getPlaceholder(), Component.text(accessType.name().toLowerCase(Locale.ENGLISH))));
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_ACCESS_TYPE,
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
            //todo lang mapping
            return Arrays.stream(SignAccessType.AccessType.values()).map(type -> type.name().toLowerCase()).toList();
        } else {
            return null;
        }
    }
}
