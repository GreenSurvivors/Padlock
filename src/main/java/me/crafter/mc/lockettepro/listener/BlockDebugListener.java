package me.crafter.mc.lockettepro.listener;

import me.crafter.mc.lockettepro.api.LocketteProAPI;
import me.crafter.mc.lockettepro.impl.MiscUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockDebugListener implements Listener {

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDebugClick(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (p.isSneaking() && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block b = event.getClickedBlock();
            if (b == null) return;
            p.sendMessage(Component.text("===========================").color(NamedTextColor.GREEN));
            p.sendMessage(Component.text("isLockable: ").append(formatBoolean(LocketteProAPI.isLockable(b))));
            p.sendMessage(Component.text("isLocked: ").append(formatBoolean(LocketteProAPI.isLocked(b))));
            p.sendMessage(Component.text(" - isOwner/User: ").append(formatBoolean(LocketteProAPI.isOwner(b, p))).append(Component.text("/")).append(formatBoolean(LocketteProAPI.isMember(b, p))));
            p.sendMessage(Component.text("isLockedSingle: ").append(formatBoolean(LocketteProAPI.isLockedSingleBlock(b, null))));
            p.sendMessage(Component.text(" - isOwner/UserSingle: ").append(formatBoolean(LocketteProAPI.isOwnerSingleBlock(b, null, p))).append(Component.text("/")).append(formatBoolean(LocketteProAPI.isUserSingleBlock(b, null, p))));
            p.sendMessage(Component.text("isLockedUpDownLockedDoor: ").append(formatBoolean(LocketteProAPI.isUpDownOfLockedDoor(b))));
            p.sendMessage(Component.text(" - isOwner/UserSingle: ").append(formatBoolean(LocketteProAPI.isOwnerUpDownLockedDoor(b, p))).append(Component.text("/")).append(formatBoolean(LocketteProAPI.isOwnerUpDownLockedDoor(b, p))));
            if (b.getState() instanceof Sign sign && LocketteProAPI.isLockSign(sign)) {
                p.sendMessage(Component.text("isSignExpired: ").append(formatBoolean(LocketteProAPI.isSignExpired(sign))));
                p.sendMessage(Component.text(" - created: ").append(Component.text(MiscUtils.getCreatedFromLine(((Sign) b.getState()).getLine(0)))));
                p.sendMessage(Component.text(" - now     : ").append(Component.text((int) (System.currentTimeMillis() / 1000))));
            }

            p.sendMessage("Block: " + b.getType() + " " + b.getData());

            if (Tag.WALL_SIGNS.isTagged(b.getType())) {
                for (String line : ((Sign) b.getState()).getLines()) {
                    p.sendMessage(ChatColor.GREEN + line);
                }
            }
            p.sendMessage(p.getUniqueId().toString());
        }
    }

    public Component formatBoolean(boolean tf) {
        return tf ? Component.text("true").color(NamedTextColor.GREEN) : Component.text("false").color(NamedTextColor.RED);
    }

}


