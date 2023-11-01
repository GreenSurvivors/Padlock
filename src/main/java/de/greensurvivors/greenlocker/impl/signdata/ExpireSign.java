package de.greensurvivors.greenlocker.impl.signdata;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.impl.MiscUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

public class ExpireSign { //todo update
    public static void updateLineWithTime(Sign sign, boolean noexpire) {
        if (noexpire) {
            sign.getSide(Side.FRONT).line(0, sign.getSide(Side.FRONT).line(0).append(Component.text("#created:" + -1)));
        } else {
            sign.getSide(Side.FRONT).line(0, sign.getSide(Side.FRONT).line(0).append(Component.text("#created:" + (int) (System.currentTimeMillis() / 1000))));
        }
        sign.update();
    }

    public static boolean isSignExpired(Sign sign) {
        if (GreenLockerAPI.isLockSign(sign)) {
            return isLineExpired(sign.getSide(Side.FRONT).getLine(0));
        } else {
            return false;
        }
    }

    @Deprecated(forRemoval = true)
    private static boolean isLineExpired(String line) {
        long createdtime = MiscUtils.getCreatedFromLine(line);
        if (createdtime == -1L) return false; // No expire
        long currenttime = (int) (System.currentTimeMillis() / 1000);
        return createdtime + GreenLocker.getPlugin().getConfigManager().getLockExpireDays() * 86400L < currenttime;
    }
}
