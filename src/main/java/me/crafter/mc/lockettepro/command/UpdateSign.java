package me.crafter.mc.lockettepro.command;

import me.crafter.mc.lockettepro.api.LocketteProAPI;
import me.crafter.mc.lockettepro.config.Config;
import me.crafter.mc.lockettepro.impl.MiscUtils;
import me.crafter.mc.lockettepro.impl.SignSelection;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class UpdateSign extends SubCommand {
    protected UpdateSign(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission("lockettepro.updatesign");
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("updatesign");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return Config.getCmdHelp("updatesign");
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
        if (sender instanceof Player player) {
            if (player.hasPermission("lockettepro.update")) {
                Block block = SignSelection.getSelectedSign(player);
                if (block != null) {
                    if (block instanceof Sign sign) {
                        if (LocketteProAPI.updateLegacySign(sign) != null) {
                            MiscUtils.sendMessages(player, Config.getLangComp("sign-updated"));
                        } else {
                            MiscUtils.sendMessages(player, Config.getLangComp("sign-need-reselect"));
                        }
                    } else if (LocketteProAPI.isLocked(block)) { //something went wrong, try to recover
                        Sign sign = LocketteProAPI.getLockSign(block);

                        if (sign != null) {
                            if (LocketteProAPI.updateLegacySign(sign) != null) {
                                MiscUtils.sendMessages(player, Config.getLangComp("sign-updated"));
                            } else {
                                MiscUtils.sendMessages(player, Config.getLangComp("sign-need-reselect"));
                            }
                        } else {
                            MiscUtils.sendMessages(player, Config.getLangComp("sign-need-reselect"));
                        }
                    } else {
                        MiscUtils.sendMessages(player, Config.getLangComp("sign-need-reselect"));
                    }
                } else {
                    MiscUtils.sendMessages(player, Config.getLangComp("no-sign-selected"));
                }
            } else {
                MiscUtils.sendMessages(sender, Config.getLangComp("no-permission"));
            }
            return true;
        } else {
            MiscUtils.sendMessages(sender, Config.getLangComp("not-player"));
            return false;
        }
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
