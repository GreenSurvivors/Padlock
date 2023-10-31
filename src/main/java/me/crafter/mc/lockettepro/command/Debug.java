package me.crafter.mc.lockettepro.command;

import me.crafter.mc.lockettepro.Dependency;
import me.crafter.mc.lockettepro.LockettePro;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Debug extends SubCommand {
    protected Debug(@NotNull LockettePro plugin) {
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
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getCmdHelp("debug");
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
            sender.sendMessage("LockettePro: " + plugin.getPluginMeta().getVersion());
            // Version
            sender.sendMessage("Server version: " + Bukkit.getVersion());
            sender.sendMessage("Expire: " + plugin.getConfigManager().isLockExpire() + " " +
                    (plugin.getConfigManager().isLockExpire() ? plugin.getConfigManager().getLockExpireDays() : ""));

            // Other
            sender.sendMessage("Linked plugins:");
            boolean linked = false;
            if (Dependency.getWorldguard() != null) {
                linked = true;
                sender.sendMessage(" - Worldguard: " + Dependency.getWorldguard().getPluginMeta().getVersion());
            }

            Plugin coreProtect = Bukkit.getPluginManager().getPlugin("CoreProtect");
            if (coreProtect != null) {
                linked = true;
                sender.sendMessage(" - CoreProtect: " + coreProtect.getPluginMeta().getVersion()); //todo get co from Depencency
            }
            if (!linked) {
                sender.sendMessage(" - none");
            }
        } else {
            plugin.getMessageManager().sendLang(sender, "no-permission");
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
