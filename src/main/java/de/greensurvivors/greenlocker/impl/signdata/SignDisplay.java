package de.greensurvivors.greenlocker.impl.signdata;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class SignDisplay {

    private static void fillWithPlayers(Component[] toFill, @NotNull Sign sign, boolean owners) {
        Set<String> players = SignLock.getUUIDs(sign, owners);

        if (!players.isEmpty()) {
            Iterator<String> it = players.iterator();

            for (int i = 0; i < toFill.length; i++) {
                if (toFill[i] == null) {
                    while (it.hasNext()) {
                        String uuidStr = it.next();

                        try {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));

                            if (player.getName() != null) {
                                toFill[i] = Component.text(player.getName());

                                //we have written a line; back to main loop to get the next one!
                                break;
                            } else {
                                GreenLocker.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + uuidStr + "\" (no " + (owners ? "Owner" : "member") + ") in Lock-Sign at " + sign.getLocation());
                            }
                        } catch (IllegalArgumentException e) {
                            GreenLocker.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + uuidStr + "\" (invalid) in Lock-Sign at " + sign.getLocation(), e);
                        }
                    } //inner loop

                    //no other players
                    break;
                } //line already filled
            } //outer loop
        } else if (owners) {
            GreenLocker.getPlugin().getLogger().log(Level.WARNING, "Lock sign without Owners at " + sign.getLocation());
        }

    }

    public static void updateDisplay(@NotNull Sign sign) {
        final int amountOfLines = sign.getSide(Side.FRONT).lines().size();
        int lastIndex = amountOfLines - 1;
        final Component[] linesToUpdate = new Component[amountOfLines];

        linesToUpdate[0] = GreenLocker.getPlugin().getMessageManager().getLang(MessageManager.LangPath.PRIVATE_SIGN);

        Boolean isEveryOneSign = EveryoneSign.getAccessEveryone(sign);
        if (isEveryOneSign != null && isEveryOneSign) {
            linesToUpdate[lastIndex] = GreenLocker.getPlugin().getMessageManager().getLang(MessageManager.LangPath.EVERYONE_SIGN);
            lastIndex--;
        }

        Component timerComponent = SignTimer.getTimerComponent(sign);
        if (timerComponent != null) {
            linesToUpdate[lastIndex] = timerComponent;
            lastIndex--;
        }

        fillWithPlayers(linesToUpdate, sign, true);
        if (isEveryOneSign == null || !isEveryOneSign) {
            fillWithPlayers(linesToUpdate, sign, false);
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
