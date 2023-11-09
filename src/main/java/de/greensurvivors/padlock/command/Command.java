package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * main command of this plugin, does by itself
 * nothing but call it's subcommands and supply command functions for them
 */
public class Command implements CommandExecutor, TabCompleter {
    /**
     * contains all the registered subcommands
     * (they get registered when a new instance get created)
     */
    private final static Set<SubCommand> SUBCOMMANDS = new HashSet<>();
    private static Padlock plugin;

    public Command(@NotNull Padlock plugin) {
        Command.plugin = plugin;

        //register all subcommands
        SUBCOMMANDS.add(new Help(plugin));
        SUBCOMMANDS.add(new Info(plugin));
        SUBCOMMANDS.add(new AddMember(plugin));
        SUBCOMMANDS.add(new RemoveMember(plugin));
        SUBCOMMANDS.add(new SetTimer(plugin));
        SUBCOMMANDS.add(new SetEveryone(plugin));
        // admin sub commands
        SUBCOMMANDS.add(new AddOwner(plugin));
        SUBCOMMANDS.add(new RemoveOwner(plugin));
        SUBCOMMANDS.add(new SetCreated(plugin));
        SUBCOMMANDS.add(new UpdateSign(plugin));
        SUBCOMMANDS.add(new Version(plugin));
        SUBCOMMANDS.add(new Debug(plugin));
        SUBCOMMANDS.add(new Reload(plugin));
    }

    /**
     * Try to get an offline player from a name or a
     * string representation of a UUID.
     * <br>
     * I don't plan to support offline mode servers.
     * If you don't have a valid UUID working for you,
     * I'm not going through the hassle to service you.
     * <br>
     * Please note: this will return some kind of offline player,
     * But as it goes in Bukkit's API this player might have never played
     * on the server, nor is it a valid minecraft account to begin with.
     *
     * @param arg username or a string representation of a UUID.
     * @return an offline player or null, if the argument couldn't be nighter a username nor an uuid
     */
    protected static @Nullable OfflinePlayer getPlayerFromArgument(@NotNull String arg) {
        if (MiscUtils.isUserName(arg)) { //check valid names
            return Bukkit.getOfflinePlayer(arg); //MiscUtils.getUuidByUsernameFromMojang();
        } else {
            try {
                return Bukkit.getOfflinePlayer(UUID.fromString(arg));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    /**
     * get a subcommand by its alias, filtered by the permission check of each subcommand against the permissible
     *
     * @param permissible to check against for permission to using the subcommand
     * @param string      the string to contain an alias of a subcommand to get
     * @return the subcommand with the alias or null if no where found
     */
    protected static @Nullable SubCommand getSubCommandFromString(@NotNull Permissible permissible, @NotNull String string) {
        String subCmdStr = string.toLowerCase();

        for (SubCommand subCommand : SUBCOMMANDS) {
            if (subCommand.getAlias().contains(subCmdStr) && subCommand.checkPermission(permissible)) {
                return subCommand;
            }
        }

        return null;
    }

    /**
     * get all registered Subcommands, filtered by the permission check of each subcommand against the permissible
     *
     * @param permissible to check against for permission to using the subcommand
     */
    protected static Set<SubCommand> getSubCommands(@NotNull Permissible permissible) {
        return SUBCOMMANDS.stream().filter(subCommand -> subCommand.checkPermission(permissible)).collect(Collectors.toSet());
    }

    /**
     * Checks if a sign is a legacy or an additional sign and starts the update process.
     *
     * @return the main lock sign if found or null else
     */
    @Deprecated(forRemoval = true)
    protected static @Nullable Sign checkAndUpdateLegacySign(@NotNull Sign sign, @NotNull Audience audience) {
        //check for old Lockett(Pro) signs and try to update them
        if (PadlockAPI.isAdditionalSign(sign) || SignLock.isLegacySign(sign)) {
            Sign otherSign = PadlockAPI.updateLegacySign(sign); //get main sign

            if (otherSign == null) { // couldn't find the main sign of the block.
                //the calling function will return feedback to the player, since this may also get called on tabCompletable
                PadlockAPI.setInvalid(sign);
                return null;
            } else {
                plugin.getMessageManager().sendLang(audience, MessageManager.LangPath.UPDATE_SIGN_SUCCESS);
                return otherSign;
            }
        }

        return sign;
    }

    /**
     * Since removing a member or an owner share most of their code,
     * Both of it gets dealt here in a common place instead of in the subcommands
     * calling thins method
     * /lock removemember <name>
     * /lock removeowner <name>
     * /lock removemember <uuid>
     * /lock removeowner <uuid>
     *
     * @param sender      sender of the (sub) command to get called back on
     * @param removeOwner if the subcommand /lock removeowner (true) was called or /lock removemember (false)
     * @return a list of possible tab completions
     */
    protected static @Nullable List<String> onTapCompleteRemovePlayer(@NotNull CommandSender sender, boolean removeOwner) {
        // you have to be present in the world to select signs
        if (sender instanceof Player player) {
            //check permission
            if (sender.hasPermission(PermissionManager.EDIT.getPerm())) {
                //get and check selected sign
                Block block = SignSelection.getSelectedSign(player);

                if (block != null) {
                    if (block.getState() instanceof Sign sign) {
                        //check for old Lockett(Pro) signs and try to update them
                        sign = checkAndUpdateLegacySign(sign, player);
                        if (sign == null) {
                            return null;
                        }

                        if (PadlockAPI.isLockSign(sign)) {
                            // check sign or admin permission, since only admins can mess with owners
                            if (sender.hasPermission(PermissionManager.ADMIN_EDIT.getPerm()) || // /lock removeowner
                                    (!removeOwner && SignLock.isOwner(sign, player.getUniqueId()))) { // /lock removemember
                                // get all members/owners names of a sign as a result
                                ListOrderedSet<String> uuidStrs = SignLock.getUUIDs(sign, removeOwner);
                                List<String> result = new ArrayList<>();

                                for (String uuidStr : uuidStrs) {
                                    try {
                                        result.add(Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName());
                                    } catch (IllegalArgumentException e) {
                                        plugin.getLogger().log(Level.WARNING, "couldn't get UUID from String \"" + uuidStr + "\" of Sign at " + sign.getLocation(), e);

                                        // invalid uuid. you can still remove it nevertheless, but just as an uuid
                                        result.add(uuidStr);
                                    }
                                }

                                return result;
                            } // not an owner nor has admin permission
                        } // not a lock sign --> sign needs reselect but calling the command will tell that, no need to spam the player here
                    } // block is not a sign --> same as above
                } // no block was selected --> calling the command will tell that, no need to spam the player here
            } // no permission --> again the same
        } // sender is not a player

        return null;
    }

    /**
     * Since adding a member or an owner share most of their code,
     * Both of it gets dealt here in a common place instead of in the subcommands
     * calling thins method
     * /lock addmember <name>
     * /lock addowner <name>
     * /lock addmember <uuid>
     * /lock addowner <uuid>
     *
     * @param sender sender of the (sub) command to get called back on
     * @param args   arguments the (sub) command was called with
     * @return true if the command was correctly called - but not necessary successful
     */
    protected static boolean onAddPlayer(@NotNull CommandSender sender, final @NotNull String @NotNull [] args, boolean addOwner) {
        // you have to be present in the world to select signs
        if (sender instanceof Player player) {
            // check permission to edit
            if (sender.hasPermission(PermissionManager.EDIT.getPerm())) {
                // check for name or uuid as argument of subcommand
                if (args.length >= 2) {
                    //get and check selected sign
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block.getState() instanceof Sign sign) {
                            //check for old Lockett(Pro) signs and try to update them
                            sign = checkAndUpdateLegacySign(sign, player);
                            if (sign == null) {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                                return true;
                            }

                            if (PadlockAPI.isLockSign(sign)) {
                                // check sign or admin permission, since only admins can mess with owners
                                if (player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm()) || // /lock addowner
                                        (!addOwner && SignLock.isOwner(sign, player.getUniqueId()))) { // /lock addmember

                                    // try to get player from argument
                                    OfflinePlayer offlinePlayer = Command.getPlayerFromArgument(args[1]);
                                    if (offlinePlayer != null) {
                                        //success!
                                        SignLock.addPlayer(sign, addOwner, offlinePlayer);

                                        TagResolver tagResolver = Placeholder.unparsed(MessageManager.PlaceHolder.PLAYER.getPlaceholder(),
                                                offlinePlayer.getName() == null ? offlinePlayer.getUniqueId().toString() : offlinePlayer.getName());
                                        // tell the player
                                        if (addOwner) {
                                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.ADD_OWNER_SUCCESS, tagResolver);
                                        } else {
                                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.ADD_MEMBER_SUCCESS, tagResolver);
                                        }
                                    } else {
                                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UNKNOWN_PLAYER,
                                                Placeholder.unparsed(MessageManager.PlaceHolder.PLAYER.getPlaceholder(), args[1]));
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_ENOUGH_ARGS);
                    return false;
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
            }

            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
            return false;
        }
    }

    /**
     * Since removing a member or an owner share most of their code,
     * Both of it gets dealt here in a common place instead of in the subcommands
     * calling thins method
     * /lock removemember <name>
     * /lock removeowner <name>
     * /lock removemember <uuid>
     * /lock removeowner <uuid>
     *
     * @param sender sender of the (sub) command to get called back on
     * @param args   arguments the (sub) command was called with
     * @return true if the command was correctly called - but not necessary successful
     */
    protected static boolean onRemovePlayer(@NotNull CommandSender sender, final @NotNull String @NotNull [] args, boolean removeOwner) {
        // you have to be present in the world to select signs
        if (sender instanceof Player player) {
            // check permission to edit
            if (sender.hasPermission(PermissionManager.EDIT.getPerm())) {
                // check for name or uuid as argument of subcommand
                if (args.length >= 2) {
                    //get and check selected sign
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block.getState() instanceof Sign sign) {
                            //check for old Lockett(Pro) signs and try to update them
                            sign = checkAndUpdateLegacySign(sign, player);
                            if (sign == null) {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                                return true;
                            }

                            if (PadlockAPI.isLockSign(sign)) {
                                // check sign or admin permission, since only admins can mess with owners
                                if (player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm()) || // /lock removeowner
                                        (!removeOwner && SignLock.isOwner(sign, player.getUniqueId()))) { // /lock removemember

                                    // try to get player from argument
                                    OfflinePlayer offlinePlayer = getPlayerFromArgument(args[1]);
                                    if (offlinePlayer != null) {
                                        //prepare resolber
                                        TagResolver tagResolver = Placeholder.unparsed(MessageManager.PlaceHolder.PLAYER.getPlaceholder(),
                                                offlinePlayer.getName() == null ? offlinePlayer.getUniqueId().toString() : offlinePlayer.getName());

                                        // try to remove the member/owner, may fail if the player is not a member/owner
                                        if (SignLock.removePlayer(sign, removeOwner, offlinePlayer.getUniqueId())) {
                                            //success
                                            if (removeOwner) {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.REMOVE_OWNER_SUCCESS, tagResolver);
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.REMOVE_MEMBER_SUCCESS, tagResolver);
                                            }
                                        } else {
                                            if (removeOwner) {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.REMOVE_OWNER_ERROR, tagResolver);
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.REMOVE_MEMBER_ERROR, tagResolver);
                                            }
                                        }
                                    } else {
                                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.UNKNOWN_PLAYER,
                                                Placeholder.unparsed(MessageManager.PlaceHolder.PLAYER.getPlaceholder(), args[1]));
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_ENOUGH_ARGS);
                    return false;
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
            }

            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
            return false;
        }
    }

    /**
     * Requests a list of possible completions for a command argument.
     * If one argument was given, return a list of all subcommands, filtered by permission of the sender,
     * else pass down all the arguments to the called subcommand, if found.
     * Will provide filtering against the last argument (for all subcommands)
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside of a command block, this will be the player, not
     *                the command block.
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestionList = null;

        if (args.length == 1) {
            suggestionList = new ArrayList<>();

            // return all alias of all subcommands the sender has permission to use
            for (SubCommand subCommand : SUBCOMMANDS) {
                if (subCommand.checkPermission(sender)) {
                    suggestionList.addAll(subCommand.getAlias());
                }
            }
        } else {
            // get subcommand from string and call its method
            SubCommand subCommand = getSubCommandFromString(sender, args[0]);

            if (subCommand != null) {
                suggestionList = subCommand.onTabComplete(sender, args);
            }
        }

        //filter through already typed arguments
        if (suggestionList != null) {
            List<String> filteredList = new ArrayList<>();
            for (String s : suggestionList) {
                if (s.startsWith(args[args.length - 1])) {
                    filteredList.add(s);
                }
            }

            return filteredList;
        }

        // list was empty
        return null;
    }

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     * calls the subcommands onCommand method
     *
     * @param sender       Source of the command
     * @param command      Command which was executed
     * @param commandLabel Alias of the command which was used
     * @param args         Passed command arguments
     * @return true if a valid command, otherwise false
     */
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String commandLabel, final String[] args) {
        if (args.length == 0) {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.CMD_USAGE);
            return true;
        } else {
            SubCommand subCommand = getSubCommandFromString(sender, args[0]);

            if (subCommand != null) {
                return subCommand.onCommand(sender, args);
            }

            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.CMD_USAGE);
            return false;
        }
    }
}
