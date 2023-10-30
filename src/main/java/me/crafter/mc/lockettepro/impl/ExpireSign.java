package me.crafter.mc.lockettepro.impl;

import me.crafter.mc.lockettepro.api.LocketteProAPI;
import me.crafter.mc.lockettepro.config.Config;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

public class ExpireSign { //todo update
    public static void updateLineWithTime(Sign sign, boolean noexpire) {
        if (noexpire) {
            sign.getSide(Side.FRONT).setLine(0, sign.getSide(Side.FRONT).getLine(0) + "#created:" + -1);
        } else {
            sign.getSide(Side.FRONT).setLine(0, sign.getSide(Side.FRONT).getLine(0) + "#created:" + (int) (System.currentTimeMillis() / 1000));
        }
        sign.update();
    }

    public static boolean isSignExpired(Sign sign) {
        if (LocketteProAPI.isLockSign(sign)) {
            return isLineExpired(sign.getSide(Side.FRONT).getLine(0));
        } else {
            return false;
        }
    }

    private static boolean isLineExpired(String line) {
        long createdtime = MiscUtils.getCreatedFromLine(line);
        if (createdtime == -1L) return false; // No expire
        long currenttime = (int) (System.currentTimeMillis() / 1000);
        return createdtime + Config.getLockExpireDays() * 86400L < currenttime;
    }
}
