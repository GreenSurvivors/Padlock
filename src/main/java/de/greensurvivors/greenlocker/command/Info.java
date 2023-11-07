package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import de.greensurvivors.greenlocker.impl.MiscUtils;
import de.greensurvivors.greenlocker.impl.SignSelection;
import de.greensurvivors.greenlocker.impl.signdata.SignLock;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Info extends SubCommand {
    protected Info(@NotNull GreenLocker plugin) {
        super(plugin);
    }

    @Override
    protected boolean checkPermission(Permissible sender) {
        return sender.hasPermission(PermissionManager.CMD_INFO.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAlias() {
        return Set.of("info");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_INFO);
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
    @Override //todo check if member or admin
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) { //todo this needs formatting and general glow up
        //todo needs timer, redstone, created + expired, everyone
        if (sender instanceof Player player) {
            if (this.checkPermission(sender)) {
                Block block = SignSelection.getSelectedSign(player);
                if (block != null) {
                    if (block instanceof Sign sign) {
                        if (GreenLockerAPI.isAdditionalSign(sign) || SignLock.isLegacySign(sign)) {
                            Sign otherSign = GreenLockerAPI.updateLegacySign(sign); //get main sign

                            if (otherSign == null) {
                                GreenLockerAPI.setInvalid(sign);
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                                return true;
                            } else {
                                sign = otherSign;
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UPDATE_SIGN_SUCCESS);
                            }
                        }

                        Component component = plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_OWNERS);
                        for (String name : MiscUtils.getNamesFromUUIDStrSet(SignLock.getUUIDs(sign, true))) {
                            component = component.append(Component.text(name));
                            component = component.append(Component.text(", "));
                        }
                        component = component.append(Component.newline());

                        component = component.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_MEMBERS));
                        for (String name : MiscUtils.getNamesFromUUIDStrSet(SignLock.getUUIDs(sign, false))) {
                            component = component.append(Component.text(name));
                            component = component.append(Component.text(", "));
                        }

                        plugin.getMessageManager().sendMessages(sender, component);
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
            }
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
            return false;
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
