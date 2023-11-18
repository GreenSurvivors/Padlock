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
 * reloads config and language files
 */
public class Reload extends SubCommand {
    protected Reload(@NotNull Padlock plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_RELOAD.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("reload");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_RELOAD);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (this.checkPermission(sender)) {
            plugin.getConfigManager().reload();

            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.RELOAD_SUCCESS);
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
