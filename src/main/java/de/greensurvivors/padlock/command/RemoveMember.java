package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * removes a member from a lock sign
 */
public class RemoveMember extends SubCommand {
    protected RemoveMember(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.EDIT.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("rm", "removemember");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_REMOVE_MEMBER);
    }

    /**
     * since it shares most of the code with the subcommand removeOwner, both of them get executed in the main commands class
     *
     * @param sender Source of the command
     * @param args   Passed command arguments.
     *               Please Note: The subcommand will ALWAYS be the first argument aka arg[0].
     */
    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        return MainCommand.onRemovePlayer(sender, args, false);
    }

    /**
     * since it shares most of the code with the subcommand removeOwner, both of them get completed in the main commands class
     *
     * @param sender Source of the command
     * @param args   Passed command arguments.
     *               Please Note: The subcommand will ALWAYS be the first argument aka arg[0].
     */
    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return MainCommand.onTapCompleteRemovePlayer(sender, false);
    }
}
