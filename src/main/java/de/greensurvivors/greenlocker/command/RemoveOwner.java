package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class RemoveOwner extends SubCommand {
    protected RemoveOwner(@NotNull GreenLocker plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission(PermissionManager.EDIT.getPerm()) && sender.hasPermission(PermissionManager.PERMISSION_MANAGER.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("ro", "removeowner");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_REMOVE_OWNER);
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
        return Command.onRemovePlayer(sender, args, true);
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
        return Command.onTapCompleateRemovePlayer(sender, true);
    }
}