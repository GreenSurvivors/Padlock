package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.EveryoneSign;
import de.greensurvivors.padlock.impl.signdata.SignExpiration;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignTimer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

/**
 * get all important information about a lock sign.
 * members, owners, timer, expiration, etc.
 */
public class Info extends SubCommand {
    protected Info(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_INFO.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("info");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_INFO);
    }


    /**
     * Try to get a list of names from the set of string versions of uuids given back by the lock.
     * Will ignore any entry without a valid uuid.
     */
    public static @NotNull Set<String> getNamesFromUUIDStrSet(final @NotNull Set<String> stringUUIDs) {
        Set<String> players = new HashSet<>(stringUUIDs.size());

        for (String uuidStr : stringUUIDs) {
            try {
                players.add(Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName());
            } catch (IllegalArgumentException e) {
                Padlock.getPlugin().getLogger().log(Level.WARNING, "couldn't get UUID from String \"" + uuidStr + "\"", e);
            }
        }

        return players;
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) { //todo this needs formatting and general glow up
        if (this.checkPermission(sender)) {
            if (sender instanceof Player player) {
                Sign sign = SignSelection.getSelectedSign(player);
                if (sign != null) {
                    //check for old Lockett(Pro) signs and try to update them
                    sign = Command.checkAndUpdateLegacySign(sign, player);
                    if (sign == null) {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        return true;
                    }

                    // only admins, owners and members
                    if (player.hasPermission(PermissionManager.ADMIN_USE.getPerm()) ||
                            SignLock.isOwner(sign, player.getUniqueId()) ||
                            SignLock.isMember(sign, player.getUniqueId())) {

                        // owners
                        Component component = plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_OWNERS);
                        for (String name : getNamesFromUUIDStrSet(SignLock.getUUIDs(sign, true))) {
                            component = component.append(Component.text(name));
                            component = component.append(Component.text(", "));
                        }

                        // members
                        component = component.append(Component.newline());
                        component = component.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_MEMBERS));
                        if (EveryoneSign.getAccessEveryone(sign)) {
                            component = component.append(plugin.getMessageManager().getLang(MessageManager.LangPath.EVERYONE_SIGN));
                        } else {
                            for (String name : getNamesFromUUIDStrSet(SignLock.getUUIDs(sign, false))) {
                                component = component.append(Component.text(name));
                                component = component.append(Component.text(", "));
                            }
                        }

                        // timer
                        Long timer = SignTimer.getTimer(sign);
                        if (timer != null) {
                            component = component.append(Component.newline());
                            component = component.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_TIMER));
                            component = component.append(Component.text(timer));
                        }

                        // expiration
                        component = component.append(Component.newline());
                        component = component.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_EXPIRED));
                        component = component.append(Component.text(SignExpiration.isSignExpired(sign)));

                        plugin.getMessageManager().sendMessageWithPrefix(sender, component);
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
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