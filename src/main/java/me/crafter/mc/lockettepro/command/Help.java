package me.crafter.mc.lockettepro.command;

import me.crafter.mc.lockettepro.config.Config;
import me.crafter.mc.lockettepro.impl.MiscUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Help extends SubCommand {
    protected Help(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission("lockettepro.help");
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("help");
    }

    @Override
    protected @NotNull String getHelpText() {
        return Config.getCmdHelp("help");
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
        if (args.length >= 2) {
            SubCommand command = Command.getSubCommandFromString(sender, args[2]);

            if (command != null) {
                String header = Config.getLang("cmd.help.header");
                String helpText = command.getHelpText();

                if (!helpText.isEmpty()) {
                    sender.sendMessage(header + "\n" + helpText);
                } else {
                    //hardcoded since it's pretty likely something is broken
                    sender.sendMessage(ChatColor.RED + "[Error] Couldn't get help text.");
                }
            } else {
                MiscUtils.sendMessages(sender, Config.getLang("cmd.no-subcommand"));
                return false;
            }
        } else {
            StringBuilder textBuilder = new StringBuilder(Config.getLang("cmd.help.header"));
            textBuilder.append("\n");

            for (SubCommand subCommand : Command.getSubCommands(sender)) {
                textBuilder.append(" - ");

                for (String alias : subCommand.getAlias()) {
                    textBuilder.append(alias);
                    textBuilder.append(", ");
                }

                textBuilder.append("\n");
            }

            MiscUtils.sendMessages(sender, textBuilder.toString());
        }

        return true;
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
        return null;
    }
}
