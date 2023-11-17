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
 * removes an owner from a lock sign
 */
public class RemoveOwner extends SubCommand {
    protected RemoveOwner(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.EDIT.getPerm()) && permissible.hasPermission(PermissionManager.ADMIN_EDIT.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("ro", "removeowner");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_REMOVE_OWNER);
    }

    /**
     * since it shares most of the code with the subcommand removeMember, both of them get executed in the main commands class
     *
     * @param sender Source of the command
     * @param args   Passed command arguments.
     *               Please Note: The subcommand will ALWAYS be the first argument aka arg[0].
     */
    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        return Command.onRemovePlayer(sender, args, true);
    }

    /**
     * since it shares most of the code with the subcommand removeMember, both of them get completed in the main commands class
     *
     * @param sender Source of the command
     * @param args   Passed command arguments.
     *               Please Note: The subcommand will ALWAYS be the first argument aka arg[0].
     */
    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Command.onTapCompleteRemovePlayer(sender, true);
    }
}
