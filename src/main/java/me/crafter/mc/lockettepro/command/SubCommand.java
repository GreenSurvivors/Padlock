package me.crafter.mc.lockettepro.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public abstract class SubCommand {
    protected final @NotNull Plugin plugin;

    protected SubCommand(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    abstract protected boolean checkPermission(Permissible sender);

    /**
     * has to be lower case
     *
     * @return
     */
    abstract protected @NotNull Set<String> getAlias();

    abstract protected @NotNull Component getHelpText();

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender Source of the command
     * @param args   Passed command arguments.
     *               Please Note: The subcommand will ALWAYS be the first argument aka arg[0].
     * @return true if a valid command, otherwise false
     */
    abstract protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args);

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
    @Nullable
    abstract protected List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args);
}
