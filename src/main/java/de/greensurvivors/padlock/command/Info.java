package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.signdata.SignAccessType;
import de.greensurvivors.padlock.impl.signdata.SignExpiration;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

/**
 * get all important information about a lock sign.
 * members, owners, timer, expiration, etc.
 */
public class Info extends SubCommand {
    protected Info(@NotNull Padlock plugin) {
        super(plugin);
    }

    /**
     * Try to get a list of names from the set of string versions of uuids given back by the lock.
     * Will ignore any entry without a valid uuid.
     * May return a UUID string instead of a name if the name was null
     */
    private static @NotNull Set<@Nullable String> getNamesFromUUIDStrSet(final @NotNull Set<String> stringUUIDs) {
        Set<String> players = new HashSet<>(stringUUIDs.size());

        for (String uuidStr : stringUUIDs) {
            try {
                String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();

                players.add(Objects.requireNonNullElse(name, uuidStr));
            } catch (IllegalArgumentException e) {
                Padlock.getPlugin().getLogger().log(Level.WARNING, "couldn't get UUID from String \"" + uuidStr + "\"", e);
            }
        }

        return players;
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_INFO.getPerm());
    }

    @Override
    protected @NotNull Set<String> getAliases() {
        return Set.of("info");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_INFO);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) { //todo this needs formatting and general glow up; connected info; payers seem to be broken
        if (this.checkPermission(sender)) {
            if (sender instanceof Player player) {
                Sign sign = SignSelection.getSelectedSign(player);
                if (sign != null) {
                    //check for old Lockett(Pro) signs and try to update them
                    sign = Command.checkAndUpdateLegacySign(sign, player);
                    if (sign == null) {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        return true;
                    }

                    // only admins, owners and members
                    if (player.hasPermission(PermissionManager.ADMIN_USE.getPerm()) ||
                            SignLock.isOwner(sign, player.getUniqueId()) ||
                            SignLock.isMember(sign, player.getUniqueId())) {

                        // owners
                        TextComponent.Builder builder = Component.text();
                        builder.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_OWNERS).appendSpace());
                        for (String name : getNamesFromUUIDStrSet(SignLock.getUUIDs(sign, true, false))) {
                            builder.append(Component.text(name));
                            builder.append(Component.text(", "));
                        }

                        // members
                        builder.append(Component.newline());
                        builder.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_MEMBERS)).appendSpace();
                        if (SignAccessType.getAccessType(sign, false) != SignAccessType.AccessType.PUBLIC) {
                            for (String name : getNamesFromUUIDStrSet(SignLock.getUUIDs(sign, false, false))) {
                                builder.append(Component.text(name));
                                builder.append(Component.text(", "));
                            }
                        }

                        // access type
                        builder.append(Component.newline());
                        builder.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_ACCESS_TYPE)).appendSpace();

                        switch (SignAccessType.getAccessType(sign, false)) {
                            case PRIVATE ->
                                    builder.append(Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_PRIVATE));
                            case PUBLIC ->
                                    builder.append(Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_PUBLIC));
                            case DONATION ->
                                    builder.append(Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_DONATION));
                            case DISPLAY ->
                                    builder.append(Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_DISPLAY));
                            case SUPPLY ->
                                    builder.append(Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_SUPPLY_SIGN));
                            case null, default ->
                                    builder.append(Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_ERROR));
                        }

                        // timer
                        Long timer = SignTimer.getTimer(sign, false);
                        if (timer != null) {
                            builder.append(Component.newline());
                            builder.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_TIMER)).appendSpace();
                            builder.append(Component.text(timer));
                        }

                        // expiration
                        builder.append(Component.newline());
                        builder.append(plugin.getMessageManager().getLang(MessageManager.LangPath.INFO_EXPIRED)).appendSpace();
                        builder.append(Component.text(SignExpiration.isSignExpired(sign)));

                        plugin.getMessageManager().sendMessageWithPrefix(sender, builder.asComponent());
                    } else {
                        plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NO_PERMISSION);
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.SIGN_NOT_SELECTED);
                }
            } else {
                plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
                return false;
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
