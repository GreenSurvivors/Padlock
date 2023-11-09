package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.Dependency;
import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Debug command, useful if problems with this plugin occur, since it returns all sorts of technical detail.
 */
public class Debug extends SubCommand {
    protected Debug(@NotNull GreenLocker plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.DEBUG.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("debug");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_DEBUG);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // This is not the author debug, this prints out info
        if (this.checkPermission(sender)) {
            sender.sendMessage("GreenLocker Debug Message");
            // Basic
            sender.sendMessage("GreenLocker: " + plugin.getPluginMeta().getVersion());
            // Version
            sender.sendMessage("Server version: " + Bukkit.getVersion());
            sender.sendMessage("Expire: " + plugin.getConfigManager().doLocksExpire() + " " +
                    (plugin.getConfigManager().doLocksExpire() ? plugin.getConfigManager().getLockExpireDays() : ""));

            // Other
            sender.sendMessage("Linked plugins:");
            boolean linked = false;
            if (Dependency.getWorldguard() != null) {
                linked = true;
                sender.sendMessage(" - Worldguard: " + Dependency.getWorldguard().getPluginMeta().getVersion());
            }

            String CoreProtectAPIVersion = Dependency.getCoreProtectAPIVersion();

            if (CoreProtectAPIVersion != null) {
                linked = true;
                sender.sendMessage(" - CoreProtectAPI: " + CoreProtectAPIVersion);
            }
            if (!linked) {
                sender.sendMessage(" - none");
            }
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
        }

        return true;
    }

    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return null;
    }
}
