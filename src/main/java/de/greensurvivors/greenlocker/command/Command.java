package de.greensurvivors.greenlocker.command;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import de.greensurvivors.greenlocker.impl.MiscUtils;
import de.greensurvivors.greenlocker.impl.signdata.LockSign;
import de.greensurvivors.greenlocker.impl.signdata.SignSelection;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Command implements CommandExecutor, TabCompleter {
    private final static Set<SubCommand> SUBCOMMANDS = new HashSet<>();
    private static GreenLocker plugin;

    public Command(GreenLocker plugin) {
        Command.plugin = plugin;

        SUBCOMMANDS.add(new Help(plugin));
        SUBCOMMANDS.add(new Info(plugin));
        SUBCOMMANDS.add(new AddMember(plugin));
        SUBCOMMANDS.add(new RemoveMember(plugin));
        // admin sub commands
        SUBCOMMANDS.add(new AddOwner(plugin));
        SUBCOMMANDS.add(new RemoveOwner(plugin));
        SUBCOMMANDS.add(new UpdateSign(plugin));
        SUBCOMMANDS.add(new Version(plugin));
        SUBCOMMANDS.add(new Debug(plugin));
        SUBCOMMANDS.add(new Reload(plugin));
    }

    protected static @Nullable OfflinePlayer getPlayerFromArgument(String arg) {
        if (MiscUtils.isUserName(arg)) { //check valid names
            return Bukkit.getOfflinePlayer(arg); //Utils.getUuidByUsernameFromMojang();
        } else {
            try {
                return Bukkit.getOfflinePlayer(UUID.fromString(arg));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    protected static @Nullable List<String> onTapCompleateRemovePlayer(@NotNull CommandSender sender, boolean removeOwner) {
        if (sender instanceof Player player) {
            if (sender.hasPermission(PermissionManager.edit.getPerm())) {
                Block block = SignSelection.getSelectedSign(player);

                if (block != null) {
                    if (block instanceof Sign sign) {
                        if (GreenLockerAPI.isAdditionalSign(sign) || LockSign.isLegacySign(sign)) {
                            Sign otherSign = GreenLockerAPI.updateLegacySign(sign); //get main sign

                            if (otherSign == null) {
                                GreenLockerAPI.setInvalid(sign);
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNeedReselect);
                                return null;
                            } else {
                                sign = otherSign;
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.updateSignSuccess);
                            }
                        }

                        if (removeOwner) {
                            if (sender.hasPermission(PermissionManager.adminEdit.getPerm())) {
                                ListOrderedSet<String> uuidStrs = LockSign.getUUIDs(sign, true);
                                List<String> result = new ArrayList<>();

                                for (String uuidStr : uuidStrs) {
                                    UUID uuid;

                                    try {
                                        uuid = UUID.fromString(uuidStr);
                                    } catch (IllegalArgumentException e) {
                                        plugin.getLogger().log(Level.WARNING, "couldn't get UUID from String \"" + uuidStr + "\" of Sign at " + sign.getLocation(), e);

                                        result.add(uuidStr);
                                        continue;
                                    }

                                    result.add(Bukkit.getOfflinePlayer(uuid).getName());
                                }

                                return result;
                            }
                        } else {
                            ListOrderedSet<String> uuidStrs = LockSign.getUUIDs(sign, false);
                            List<String> result = new ArrayList<>();

                            for (String uuidStr : uuidStrs) {
                                UUID uuid;

                                try {
                                    uuid = UUID.fromString(uuidStr);
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().log(Level.WARNING, "couldn't get UUID from String \"" + uuidStr + "\" of Sign at " + sign.getLocation(), e);

                                    result.add(uuidStr);
                                    continue;
                                }

                                result.add(Bukkit.getOfflinePlayer(uuid).getName());
                            }

                            return result;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * /lock addmember <name>
     * /lock addowner <name>
     * /lock addmember <uuid>
     * /lock addowner <uuid>
     *
     * @param sender
     * @param args
     * @return
     */
    protected static boolean onAddPlayer(@NotNull CommandSender sender, final @NotNull String @NotNull [] args, boolean addOwner) {
        if (sender instanceof Player player) {
            if (sender.hasPermission(PermissionManager.edit.getPerm())) {
                if (args.length >= 2) {
                    OfflinePlayer offlinePlayer = Command.getPlayerFromArgument(args[1]);
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block instanceof Sign sign) {
                            if (GreenLockerAPI.isAdditionalSign(sign) || LockSign.isLegacySign(sign)) {
                                Sign otherSign = GreenLockerAPI.updateLegacySign(sign); //get main sign

                                if (otherSign == null) {
                                    GreenLockerAPI.setInvalid(sign);
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNeedReselect);
                                    return true;
                                } else {
                                    sign = otherSign;
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.updateSignSuccess);
                                }
                            }

                            if (player.hasPermission(PermissionManager.adminEdit.getPerm()) || (!addOwner && GreenLockerAPI.isOwnerOfSign(sign, player))) {
                                if (offlinePlayer != null) {

                                    if (GreenLockerAPI.isLockSign(sign)) {
                                        LockSign.addPlayer(sign, addOwner, offlinePlayer);

                                        if (addOwner) {
                                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.addOwnerSuccess);
                                        } else {
                                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.addMemberSuccess);
                                        }
                                    } else {
                                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNeedReselect);
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.unknownPlayer);
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.noPermission);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNeedReselect);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNotSelected);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.notEnoughArgs);
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.noPermission);
            }

            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.notAPlayer);
            return false;
        }
    }

    /**
     * /lock removemember <name>
     * /lock removeowner <name>
     * /lock removemember <uuid>
     * /lock removeowner <uuid>
     *
     * @param sender
     * @param args
     * @return
     */
    protected static boolean onRemovePlayer(@NotNull CommandSender sender, final @NotNull String @NotNull [] args, boolean removeOwner) {
        if (sender instanceof Player player) {
            if (sender.hasPermission(PermissionManager.edit.getPerm())) {
                if (args.length >= 2) {
                    OfflinePlayer offlinePlayer = getPlayerFromArgument(args[1]);
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block instanceof Sign sign) {
                            if (GreenLockerAPI.isAdditionalSign(sign) || LockSign.isLegacySign(sign)) {
                                Sign otherSign = GreenLockerAPI.updateLegacySign(sign); //get main sign

                                if (otherSign == null) {
                                    GreenLockerAPI.setInvalid(sign);
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNeedReselect);
                                    return true;
                                } else {
                                    sign = otherSign;
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.updateSignSuccess);
                                }
                            }

                            if (player.hasPermission(PermissionManager.adminEdit.getPerm()) || (!removeOwner && LockSign.isOwner(sign, player.getUniqueId()))) {
                                if (offlinePlayer != null) {
                                    if (GreenLockerAPI.isLockSign(sign)) {
                                        if (LockSign.removePlayer(sign, removeOwner, offlinePlayer.getUniqueId())) {
                                            if (removeOwner) {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.removeOwnerSuccess);
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.removeMemberSuccess);
                                            }
                                        } else {
                                            if (removeOwner) {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.removeOwnerError);
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.removeMemberError);
                                            }
                                        }
                                    } else {
                                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNeedReselect);
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.unknownPlayer);
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.noPermission);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNeedReselect);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.signNotSelected);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.notEnoughArgs);
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.noPermission);
            }

            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.notAPlayer);
            return false;
        }
    }

    protected static Set<SubCommand> getSubCommands(@NotNull CommandSender sender) {
        return SUBCOMMANDS.stream().filter(subCommand -> subCommand.checkPermission(sender)).collect(Collectors.toSet());
    }

    protected static @Nullable SubCommand getSubCommandFromString(@NotNull CommandSender sender, @NotNull String string) {
        String subCmdStr = string.toLowerCase();

        for (SubCommand subCommand : SUBCOMMANDS) {
            if (subCommand.getAlias().contains(subCmdStr) && subCommand.checkPermission(sender)) {
                return subCommand;
            }
        }

        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String alias, @NotNull String[] args) { //todo
        List<String> suggestionList = null;

        if (args.length == 1) {
            suggestionList = new ArrayList<>();

            for (SubCommand subCommand : SUBCOMMANDS) {
                if (subCommand.checkPermission(sender)) {
                    suggestionList.addAll(subCommand.getAlias());
                }
            }
        } else if (args.length >= 2) {
            SubCommand subCommand = getSubCommandFromString(sender, args[1]);

            if (subCommand != null) {
                suggestionList = subCommand.onTabComplete(sender, args);
            }
        } else {
            return null;
        }

        //filter through already typed arguments
        if (suggestionList != null) {
            List<String> filteredList = new ArrayList<>();
            for (String s : suggestionList) {
                if (s.startsWith(args[0])) {
                    filteredList.add(s);
                }
            }

            return filteredList;
        } else {
            return null;
        }
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command cmd, @NotNull String commandLabel, final String[] args) {
        if (args.length == 0) {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.cmdUsage);
            return true;
        } else {
            SubCommand subCommand = getSubCommandFromString(sender, args[0]);

            if (subCommand != null) {
                return subCommand.onCommand(sender, args);
            }

            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.cmdUsage);
            return false;
        }
    }
}
