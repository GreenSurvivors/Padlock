package me.crafter.mc.lockettepro.command;

import me.crafter.mc.lockettepro.config.Config;
import me.crafter.mc.lockettepro.dependency.Dependency;
import me.crafter.mc.lockettepro.impl.MiscUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Debug extends SubCommand {
    protected Debug(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission("lockettepro.debug");
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("debug");
    }

    @Override
    protected @NotNull String getHelpText() {
        return Config.getCmdHelp("debug");
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
        // This is not the author debug, this prints out info
        if (sender.hasPermission("lockettepro.debug")) {
            sender.sendMessage("LockettePro Debug Message");
            // Basic
            sender.sendMessage("LockettePro: " + plugin.getDescription().getVersion());
            // Version
            sender.sendMessage("Server version: " + Bukkit.getVersion());
            sender.sendMessage("Expire: " + Config.isLockExpire() + " " + (Config.isLockExpire() ? Config.getLockExpireDays() : ""));

            // Other
            sender.sendMessage("Linked plugins:");
            boolean linked = false;
            if (Dependency.getWorldguard() != null) {
                linked = true;
                sender.sendMessage(" - Worldguard: " + Dependency.getWorldguard().getDescription().getVersion());
            }
            if (Bukkit.getPluginManager().getPlugin("CoreProtect") != null) {
                linked = true;
                sender.sendMessage(" - CoreProtect: " + Bukkit.getPluginManager().getPlugin("CoreProtect").getDescription().getVersion());
            }
            if (!linked) {
                sender.sendMessage(" - none");
            }
        } else {
            MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
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
