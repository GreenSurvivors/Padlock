package me.crafter.mc.lockettepro.listener;

import me.crafter.mc.lockettepro.LocketteProAPI;
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
    public void onDebugClick(PlayerInteractEvent event) { //todo use MessageManager
        Player player = event.getPlayer();
        if (player.isSneaking() && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block clickedBlock = event.getClickedBlock();

            if (clickedBlock != null) {
                player.sendMessage(Component.text("===========================").color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("isLockable: ").append(formatBoolean(LocketteProAPI.isLockable(clickedBlock))));
                player.sendMessage(Component.text("isLocked: ").append(formatBoolean(LocketteProAPI.isLocked(clickedBlock))));
                player.sendMessage(Component.text(" - isOwner/User: ").append(formatBoolean(LocketteProAPI.isOwner(clickedBlock, player))).append(Component.text("/")).append(formatBoolean(LocketteProAPI.isMember(clickedBlock, player))));
                player.sendMessage(Component.text("isLockedSingle: ").append(formatBoolean(LocketteProAPI.isLockedSingleBlock(clickedBlock, null))));
                player.sendMessage(Component.text(" - isOwner/UserSingle: ").append(formatBoolean(LocketteProAPI.isOwnerSingleBlock(clickedBlock, null, player))).append(Component.text("/")).append(formatBoolean(LocketteProAPI.isUserSingleBlock(clickedBlock, null, player))));
                player.sendMessage(Component.text("isLockedUpDownLockedDoor: ").append(formatBoolean(LocketteProAPI.isPartOfLockedDoor(clickedBlock))));
                player.sendMessage(Component.text(" - isOwner/UserSingle: ").append(formatBoolean(LocketteProAPI.isOwnerUpDownLockedDoor(clickedBlock, player))).append(Component.text("/")).append(formatBoolean(LocketteProAPI.isOwnerUpDownLockedDoor(clickedBlock, player))));
                if (clickedBlock.getState() instanceof Sign sign && LocketteProAPI.isLockSign(sign)) {
                    player.sendMessage(Component.text("isSignExpired: ").append(formatBoolean(LocketteProAPI.isSignExpired(sign))));
                    player.sendMessage(Component.text(" - created: ").append(Component.text(MiscUtils.getCreatedFromLine(((Sign) clickedBlock.getState()).getLine(0)))));
                    player.sendMessage(Component.text(" - now     : ").append(Component.text((int) (System.currentTimeMillis() / 1000))));
                }

                player.sendMessage("Block: " + clickedBlock.getType() + " " + clickedBlock.getData());

                if (Tag.WALL_SIGNS.isTagged(clickedBlock.getType())) {
                    for (String line : ((Sign) clickedBlock.getState()).getLines()) {
                        player.sendMessage(ChatColor.GREEN + line);
                    }
                }
                player.sendMessage(player.getUniqueId().toString());
            }
        }
    }

    private Component formatBoolean(boolean tf) {
        return tf ? Component.text("true").color(NamedTextColor.GREEN) : Component.text("false").color(NamedTextColor.RED);
    }
}