package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * gives back a list of all for the cmd-sender available commands or information about a specific command.
 */
public class Help extends SubCommand {
    protected Help(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_HELP.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("help");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_HELP);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (this.checkPermission(sender)) {
            if (args.length >= 2) {
                SubCommand command = Command.getSubCommandFromString(sender, args[1]);

                if (command != null) {
                    TextComponent.Builder builder = Component.text();
                    builder.append(plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_HEADER));
                    builder.append(Component.newline());
                    builder.append(command.getHelpText());

                    sender.sendMessage(builder);
                } else { // didn't type a valid subcommand.
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.CMD_NOT_A_SUBCOMMAND,
                            Placeholder.unparsed(MessageManager.PlaceHolder.ARGUMENT.getPlaceholder(), args[1]));
                    return false;
                }
            } else { //todo maybe pages
                Component component = plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_HEADER)
                        .append(Component.newline())
                        .append(plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_DESCRIPTION)
                        .append(Component.newline()));
                // list all subcommands alias per line
                component = component.append(MiniMessage.miniMessage().deserialize(
                        Command.getSubCommands(sender).stream().map(subCommand ->
                                " - " + String.join(", ", subCommand.getAliases())). //todo get delimiter from config
                                collect(Collectors.joining("<newline>")))); //todo better line break than newline + minimessage

                sender.sendMessage(component);
            }
        }

        return true;
    }

    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            List<String> result = new ArrayList<>();

            for (SubCommand subCommand : Command.getSubCommands(sender)) {
                result.addAll(subCommand.getAliases());
            }

            return result;
        } else {
            return null;
        }
    }
}
