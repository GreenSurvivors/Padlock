package me.crafter.mc.lockettepro;

import me.crafter.mc.lockettepro.api.LocketteProAPI;
import me.crafter.mc.lockettepro.config.Config;
import me.crafter.mc.lockettepro.dependency.Dependency;
import me.crafter.mc.lockettepro.impl.LockSign;
import me.crafter.mc.lockettepro.impl.MiscUtils;
import me.crafter.mc.lockettepro.impl.SignSelection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LockCmd implements CommandExecutor, TabCompleter {
    private final Plugin plugin;

    protected LockCmd(Plugin plugin) {
        this.plugin = plugin;
    }

    private boolean onVersion(@NotNull CommandSender sender) {
        if (sender.hasPermission("lockettepro.version")) {
            sender.sendMessage(plugin.getDescription().getFullName());
        } else {
            MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String alias, String[] args) { //todo
        List<String> commands = new ArrayList<>();
        commands.add("reload");
        commands.add("version");
        commands.add("debug");
        if (args != null && args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String s : commands) {
                if (s.startsWith(args[0])) {
                    list.add(s);
                }
            }
            return list;
        }
        return null;
    }

    @SuppressWarnings("deprecation") // we know what we are doing.
    private static @Nullable OfflinePlayer getPlayerFromArgument(String arg) {
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
     * /lock removemember <name>
     * /lock removeowner <name>
     * /lock removemember <uuid>
     * /lock removeowner <uuid>
     *
     * @param sender
     * @param args
     * @return
     */
    private boolean onRemovePlayer(@NotNull CommandSender sender, final @NotNull String @NotNull [] args, boolean removeOwner) { //todo arg uuid
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
                                    MiscUtils.sendMessages(player, Config.getLang("sign-need-reselect"));
                                    return true;
                                } else {
                                    sign = otherSign;
                                }
                            }

                            if (player.hasPermission("lockettepro.edit.admin") || (!removeOwner && LockSign.isOwner(sign, player.getUniqueId()))) {
                                if (offlinePlayer != null) {
                                    if (LocketteProAPI.isLockSign(sign)) {
                                        if (LockSign.removePlayer(sign, removeOwner, offlinePlayer.getUniqueId())) {
                                            if (removeOwner) {
                                                MiscUtils.sendMessages(player, Config.getLang("sign-removed-owner"));
                                            } else {
                                                MiscUtils.sendMessages(player, Config.getLang("sign-removed-member"));
                                            }
                                        } else {
                                            MiscUtils.sendMessages(player, Config.getLang("sign-couldnt-remove"));
                                        }
                                    } else {
                                        MiscUtils.sendMessages(player, Config.getLang("sign-need-reselect"));
                                    }
                                } else {
                                    MiscUtils.sendMessages(sender, Config.getLang("unknown-player")); //todo new lines
                                }
                            } else {
                                MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
                            }
                        } else {
                            MiscUtils.sendMessages(player, Config.getLang("sign-need-reselect"));
                        }
                    } else {
                        MiscUtils.sendMessages(player, Config.getLang("no-sign-selected"));
                    }
                } else {
                    MiscUtils.sendMessages(sender, Config.getLang("command-not-enough-args"));
                }
            } else {
                MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
            }

            return true;
        } else {
            MiscUtils.sendMessages(sender, Config.getLang("not-player"));
            return false;
        }
    }

    private boolean onReload(@NotNull CommandSender sender) {
        if (sender.hasPermission("lockettepro.reload")) {
            Config.reload();

            MiscUtils.sendMessages(sender, Config.getLang("config-reloaded"));
        } else {
            MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
        }

        return true;
    }

    private boolean onDebug(@NotNull CommandSender sender) {
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
     * /lock addmember <name>
     * /lock addowner <name>
     * /lock addmember <uuid>
     * /lock addowner <uuid>
     *
     * @param sender
     * @param args
     * @return
     */
    private static boolean onAddPlayer(@NotNull CommandSender sender, final @NotNull String @NotNull [] args, boolean addOwner) { //todo arg uuid
        if (sender instanceof Player player) {
            if (sender.hasPermission("lockettepro.edit")) {
                if (args.length >= 2) {
                    OfflinePlayer offlinePlayer = LockCmd.getPlayerFromArgument(args[1]);
                    Block block = SignSelection.getSelectedSign(player);

                    if (block != null) {
                        if (block instanceof Sign sign) {
                            if (LocketteProAPI.isAdditionalSign(sign) || LockSign.isLegacySign(sign)) {
                                Sign otherSign = LocketteProAPI.updateLegacySign(sign); //get main sign

                                if (otherSign == null) {
                                    LocketteProAPI.setInvalid(sign);
                                    MiscUtils.sendMessages(player, Config.getLang("sign-need-reselect"));
                                    return true;
                                } else {
                                    sign = otherSign;
                                }
                            }

                            if (player.hasPermission("lockettepro.edit.admin") || (!addOwner && LocketteProAPI.isOwnerOfSign(sign, player))) {
                                if (offlinePlayer != null) {

                                    if (LocketteProAPI.isLockSign(sign)) {
                                        LockSign.addPlayer(sign, addOwner, offlinePlayer);

                                        if (addOwner) {
                                            MiscUtils.sendMessages(player, Config.getLang("sign-added-owner"));
                                        } else {
                                            MiscUtils.sendMessages(player, Config.getLang("sign-added-member"));
                                        }
                                    } else {
                                        MiscUtils.sendMessages(player, Config.getLang("sign-need-reselect"));
                                    }
                                } else {
                                    MiscUtils.sendMessages(sender, Config.getLang("unknown-player"));
                                }
                            } else {
                                MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
                            }
                        } else {
                            MiscUtils.sendMessages(player, Config.getLang("sign-need-reselect"));
                        }
                    } else {
                        MiscUtils.sendMessages(player, Config.getLang("no-sign-selected"));
                    }
                } else {
                    MiscUtils.sendMessages(sender, Config.getLang("command-not-enough-args"));
                }
            } else {
                MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
            }

            return true;
        } else {
            MiscUtils.sendMessages(sender, Config.getLang("not-player"));
            return false;
        }
    }

    private static boolean onUpdateSign(@NotNull CommandSender sender) {
        if (sender instanceof Player player) {
            if (player.hasPermission("lockettepro.edit.admin")) {
                //todo
            } else {
                MiscUtils.sendMessages(sender, Config.getLang("no-permission"));
            }
            return true;
        } else {
            MiscUtils.sendMessages(sender, Config.getLang("not-player"));
            return false;
        }
    }

    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.Command cmd, @NotNull String commandLabel, final String[] args) {
        if (cmd.getName().equals("lockettepro")) {
            if (args.length == 0) {
                MiscUtils.sendMessages(sender, Config.getLang("command-usage"));
            } else {
                // The following commands does not require player
                return switch (args[0].toLowerCase()) {
                    case "addowner", "addo" -> onAddPlayer(sender, args, true);
                    case "addmember", "addm" -> onAddPlayer(sender, args, false);
                    case "removeowner", "remo" -> onRemovePlayer(sender, args, true);
                    case "removemember", "remm" -> onRemovePlayer(sender, args, false);
                    // admin commands
                    case "reload" -> onReload(sender);
                    case "version" -> onVersion(sender);
                    case "debug" -> onDebug(sender);
                    case "updatesign" -> onUpdateSign(sender);
                    default -> {
                        MiscUtils.sendMessages(sender, Config.getLang("command-usage"));
                        yield false;
                    }
                };
            }
        }
        return true;
    }
}
