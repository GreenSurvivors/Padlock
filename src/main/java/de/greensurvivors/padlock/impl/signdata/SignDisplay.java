package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This class manages what is visible on a sign, it updates the lock line,
 * manages visible names and timer / everyone lines.
 * Please note: All but the first line is only visual sugar, as all data is saved in PersistentDateContainer of the sign.
 */
public class SignDisplay {

    /**
     * fill empty components aka lines of a sign with owners / member names of a lock. Players without a name will be ignored
     * (should still work regardless, as this is purely what gets displayed)
     *
     * @param toFill the component lines to fill
     * @param sign   sign to get its users from
     * @param owners if owners (true) or members (false) should get filled in
     * @return if the [more Users] tag should get added
     */
    private static boolean fillWithPlayers(@Nullable Component @NotNull [] toFill, @NotNull Sign sign, boolean owners, int lastIndex) {
        Set<String> uuidStrs = SignLock.getUUIDs(sign, owners, false);
        boolean useMoreUsersTag = false;

        for (String uuidstr : uuidStrs) {
            Padlock.getPlugin().getLogger().info("uuid: " + uuidstr);
        }

        if (!uuidStrs.isEmpty()) {
            Iterator<String> it = uuidStrs.iterator();


            useMoreUsersTag = uuidStrs.size() > lastIndex;
            Padlock.getPlugin().getLogger().info("size: " + uuidStrs.size() + " last index: " + lastIndex);

            for (int i = 0; i < toFill.length; i++) {
                if (toFill[i] == null) {
                    while (it.hasNext()) {
                        String uuidStr = it.next();

                        try {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));

                            if (player.getName() != null) {
                                toFill[i] = Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_PLAYER_NAME_ON,
                                        Placeholder.unparsed(MessageManager.PlaceHolder.PLAYER.getPlaceholder(), player.getName()));

                                //we have written a line; back to main loop to get the next one!
                                break;
                            } else {
                                Padlock.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + uuidStr + "\" (no " + (owners ? "Owner" : "member") + ") in Lock-Sign at " + sign.getLocation());
                                useMoreUsersTag = true; // probably is a valid member, but the server doesn't know their name since they never joined on the server
                            }
                        } catch (IllegalArgumentException e) {
                            Padlock.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + uuidStr + "\" (invalid) in Lock-Sign at " + sign.getLocation(), e);
                        }
                    } //inner loop

                    //no other players
                    break;
                } //line already filled
            } //outer loop
        } else if (owners) {
            Padlock.getPlugin().getLogger().log(Level.WARNING, "Lock sign without Owners at " + sign.getLocation());
        }

        return useMoreUsersTag;
    }

    /**
     * update the front side lines of a lock sign,
     * fist line will always be the lock [private]
     * from buttom up the rest will first get filled with settings like timer / everyone,
     * after that from top down with owner to member names.
     *
     * @param sign the sign to update
     */
    public static void updateDisplay(@NotNull Sign sign) {
        final int amountOfLines = sign.getSide(Side.FRONT).lines().size();
        int lastIndex = amountOfLines - 1;
        final Component[] linesToUpdate = new Component[amountOfLines];

        // first line is always just the lock line
        SignAccessType.AccessType accessType = SignAccessType.getAccessType(sign, false);
        linesToUpdate[0] = switch (accessType) {
            case PRIVATE -> Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_PRIVATE);
            case PUBLIC -> Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_PUBLIC);
            case DONATION ->
                    Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_DONATION);
            case DISPLAY -> Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_DISPLAY);
            case SUPPLY ->
                    Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_SUPPLY_SIGN);
            /*case null, // todo next java version*/
            default -> Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_ERROR);
        };

        //special settings
        Component timerComponent = SignTimer.getTimerComponent(sign);
        if (timerComponent != null) {
            linesToUpdate[lastIndex] = timerComponent;
            lastIndex--;
        }

        // fill with owners, then if there is still space, and this is not a public sign, the members
        boolean shouldAddMoreUsers = fillWithPlayers(linesToUpdate, sign, true, lastIndex);
        if (accessType != SignAccessType.AccessType.PUBLIC) {
            shouldAddMoreUsers = shouldAddMoreUsers && fillWithPlayers(linesToUpdate, sign, false, lastIndex);
        }

        if (shouldAddMoreUsers) {
            linesToUpdate[lastIndex] = Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_MORE_USERS);
        }

        //got everything. Update.
        for (int i = 0; i < amountOfLines; i++) {
            Component line = linesToUpdate[i];

            if (line == null) {
                line = Component.empty();
            }

            sign.getSide(Side.FRONT).line(i, line);
        }

        sign.update();
    }
}
