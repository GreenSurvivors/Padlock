package me.crafter.mc.lockettepro.command;

import me.crafter.mc.lockettepro.LockettePro;
import me.crafter.mc.lockettepro.LocketteProAPI;
import me.crafter.mc.lockettepro.impl.MiscUtils;
import me.crafter.mc.lockettepro.impl.signdata.LockSign;
import me.crafter.mc.lockettepro.impl.signdata.SignSelection;
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
import java.util.stream.Collectors;

public class Command implements CommandExecutor, TabCompleter {
    private final static Set<SubCommand> SUBCOMMANDS = new HashSet<>();
    private static LockettePro plugin;

    public Command(LockettePro plugin) {
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
            if (sender.hasPermission("lockettepro.edit")) {
                if (args.length >= 2) {
                    OfflinePlayer offlinePlayer = Command.getPlayerFromArgument(args[1]);
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block instanceof Sign sign) {
                            if (LocketteProAPI.isAdditionalSign(sign) || LockSign.isLegacySign(sign)) {
                                Sign otherSign = LocketteProAPI.updateLegacySign(sign); //get main sign

                                if (otherSign == null) {
                                    LocketteProAPI.setInvalid(sign);
                                    plugin.getMessageManager().sendLang(sender, "sign-need-reselect");
                                    return true;
                                } else {
                                    sign = otherSign;
                                    plugin.getMessageManager().sendLang(sender, "sign-updated");
                                }
                            }

                            if (player.hasPermission("lockettepro.edit.admin") || (!addOwner && LocketteProAPI.isOwnerOfSign(sign, player))) {
                                if (offlinePlayer != null) {

                                    if (LocketteProAPI.isLockSign(sign)) {
                                        LockSign.addPlayer(sign, addOwner, offlinePlayer);

                                        if (addOwner) {
                                            plugin.getMessageManager().sendLang(sender, "sign-added-owner");
                                        } else {
                                            plugin.getMessageManager().sendLang(sender, "sign-added-member");
                                        }
                                    } else {
                                        plugin.getMessageManager().sendLang(sender, "sign-need-reselect");
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, "unknown-player");
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, "no-permission");
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, "sign-need-reselect");
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, "no-sign-selected");
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, "command-not-enough-args");
                }
            } else {
                plugin.getMessageManager().sendLang(sender, "no-permission");
            }

            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, "not-player");
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
            if (sender.hasPermission("lockettepro.edit")) {
                if (args.length >= 2) {
                    OfflinePlayer offlinePlayer = getPlayerFromArgument(args[1]);
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block instanceof Sign sign) {
                            if (LocketteProAPI.isAdditionalSign(sign) || LockSign.isLegacySign(sign)) {
                                Sign otherSign = LocketteProAPI.updateLegacySign(sign); //get main sign

                                if (otherSign == null) {
                                    LocketteProAPI.setInvalid(sign);
                                    plugin.getMessageManager().sendLang(sender, "sign-need-reselect");
                                    return true;
                                } else {
                                    sign = otherSign;
                                    plugin.getMessageManager().sendLang(sender, "sign-updated");
                                }
                            }

                            if (player.hasPermission("lockettepro.edit.admin") || (!removeOwner && LockSign.isOwner(sign, player.getUniqueId()))) {
                                if (offlinePlayer != null) {
                                    if (LocketteProAPI.isLockSign(sign)) {
                                        if (LockSign.removePlayer(sign, removeOwner, offlinePlayer.getUniqueId())) {
                                            if (removeOwner) {
                                                plugin.getMessageManager().sendLang(sender, "sign-removed-owner");
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, "sign-removed-member");
                                            }
                                        } else {
                                            plugin.getMessageManager().sendLang(sender, "sign-couldnt-remove");
                                        }
                                    } else {
                                        plugin.getMessageManager().sendLang(sender, "sign-need-reselect");
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, "unknown-player");
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, "no-permission");
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, "sign-need-reselect");
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, "no-sign-selected");
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, "command-not-enough-args");
                }
            } else {
                plugin.getMessageManager().sendLang(sender, "no-permission");
            }

            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, "not-player");
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

    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.Command cmd, @NotNull String commandLabel, final String[] args) {
        if (cmd.getName().equals("lockettepro")) {
            if (args.length == 0) {
                plugin.getMessageManager().sendLang(sender, "command-usage");
            } else {
                SubCommand subCommand = getSubCommandFromString(sender, args[1]);

                if (subCommand != null) {
                    return subCommand.onCommand(sender, args);
                }

                plugin.getMessageManager().sendLang(sender, "command-usage");
                return false;
            }
        }
        return true;
    }
}
