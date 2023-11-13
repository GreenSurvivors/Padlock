package de.greensurvivors.padlock.listener;

import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.impl.signdata.SignExpiration;
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

/**
 * Debug listener, only important for developing this plugin or really heavy problem search.
 * Must be enabled in {@link de.greensurvivors.padlock.Padlock}
 */
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
                player.sendMessage(Component.text("isLockable: ").append(formatBoolean(PadlockAPI.isLockable(clickedBlock))));
                player.sendMessage(Component.text("isProtected: ").append(formatBoolean(PadlockAPI.isProtected(clickedBlock))));
                player.sendMessage(Component.text(" - isOwner/User: ").append(formatBoolean(PadlockAPI.isOwner(clickedBlock, player))).append(Component.text("/")).append(formatBoolean(PadlockAPI.isMember(clickedBlock, player))));
                player.sendMessage(Component.text("isLockedUpDownLockedDoor: ").append(formatBoolean(PadlockAPI.isPartOfLockedDoor(clickedBlock))));
                if (clickedBlock.getState() instanceof Sign sign && PadlockAPI.isLockSign(sign)) {
                    player.sendMessage(Component.text("isSignExpired: ").append(formatBoolean(PadlockAPI.isSignExpired(sign))));
                    Long timeStamp = SignExpiration.getLastUsed(sign);
                    player.sendMessage(Component.text(" - last used: ").append(Component.text(timeStamp == null ? -1L : timeStamp)));
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