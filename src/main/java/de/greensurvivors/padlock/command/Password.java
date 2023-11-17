package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignPasswords;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Please note: This is a main command as well as a subcommand.
 * But since we are handling passwords, we want to leak them as less as possible,
 * the {@link #onCommand(CommandSender, String[])} doesn't do anything and this class doesn't even
 * implement {@link CommandExecutor}. Instead, we catch them even before the server checks
 * for the right command in a {@link PlayerCommandPreprocessEvent} in {@link de.greensurvivors.padlock.listener.ChatPlayerListener}.
 * <p>Also, {@link de.greensurvivors.padlock.command.Command} as well as {@link Padlock} itself has a separate instance of this,</p>
 */
public class Password extends SubCommand implements TabCompleter, CommandExecutor {
    public Password(@NotNull Padlock plugin) {
        super(plugin);
    }

    /**
     * Because fuck Java not allowing an abstract function to also be static.
     * I really wish I could implement this cleaner, but nether can an abstract methode static,
     * nor is there a way to have a static Set in the mother class without all subclasses share the same... -.-
     */
    public static @NotNull Set<String> getAliasesStatic() {
        return Set.of("password", "pw", "applypassword");
    }

    public static void onExternalCommand(char @NotNull [] password, @NotNull Player player) {
        // check permission for passwords
        if (player.hasPermission(PermissionManager.CMD_PASSWORD.getPerm())) {

            //get and check selected sign
            Sign sign = SignSelection.getSelectedSign(player);

            if (sign != null) {
                //check for old Lockett(Pro) signs and try to update them
                sign = de.greensurvivors.padlock.command.Command.checkAndUpdateLegacySign(sign, player);
                if (sign == null) {
                    Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SIGN_NEED_RESELECT);
                    return;
                }

                if (PadlockAPI.isLockSign(sign)) {
                    if (!SignPasswords.isOnCooldown(player.getUniqueId(), sign.getLocation())) {
                        // this will communicate if access was granted or not
                        SignPasswords.checkPasswordAndGrandAccess(sign, player, password);
                    } else {
                        Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_ON_COOLDOWN);
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
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_PASSWORD.getPerm());
    }

    @Override
    public @NotNull Set<String> getAliases() {
        return getAliasesStatic();
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_PASSWORD);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return onTabComplete(sender, args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_START_PROCESSING);
            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
            return false;
        }
    }
}
