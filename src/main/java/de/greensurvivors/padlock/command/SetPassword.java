package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignPasswords;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * But since we are handling passwords, we want to leak them as less as possible,
 * and catch them even before the server checks for the right command in a {@link PlayerCommandPreprocessEvent} in {@link de.greensurvivors.padlock.listener.ChatPlayerListener}
 */
public final class SetPassword extends SubCommand {

    SetPassword(@NotNull Padlock plugin) {
        super(plugin);
    }

    /**
     * Because fuck Java not allowing an abstract function to also be static.
     * I really wish I could implement this cleaner, but nether can an abstract methode static,
     * nor is there a way to have a static Set in the mother class without all subclasses share the same... -.-
     */
    public static @NotNull Set<String> getAliasesStatic() {
        return Set.of("setpassword", "setpw");
    }

    public static void onExternalCommand(char @Nullable [] newPassword, @NotNull Player player) {
        // check permission to edit
        if (player.hasPermission(PermissionManager.CMD_SET_PASSWORD.getPerm())) {
            //get and check selected sign
            Sign sign = SignSelection.getSelectedSign(player);
            if (sign != null) {
                if (PadlockAPI.isLockSign(sign)) {
                    // check sign owner, even admins can't change a password of something they don't own.
                    if (SignLock.isOwner(sign, player.getUniqueId()) || player.hasPermission(PermissionManager.ADMIN_PASSWORD.getPerm())) {
                        // this will communicate if password was set or removed
                        SignPasswords.setPassword(sign, player, newPassword);
                    } else {
                        Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.NO_PERMISSION);
                    }
                } else {
                    Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SIGN_NEED_RESELECT);
                }
            } else {
                Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SIGN_NOT_SELECTED);
            }
        } else {
            Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.NO_PERMISSION);
        }

        // yes I know I invalidate the arrays at multiple places, but in terms of password safety it's better to be double and tripple safe then sorry.
        if (newPassword != null) {
            Arrays.fill(newPassword, '*');
        }
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_SET_PASSWORD.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return getAliasesStatic();
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_SET_PASSWORD);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_SAFETY_WARNING);
            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_START_PROCESSING);
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
