package de.greensurvivors.padlock.listener;

import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.impl.signdata.SignExpiration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Debug listener, only important for developing this plugin or really heavy problem search.
 * Must be enabled in {@link de.greensurvivors.padlock.Padlock}
 */
public class BlockDebugListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDebugClick(PlayerInteractEvent event) { //todo use MessageManager
        Player player = event.getPlayer();
        if (player.isSneaking() && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block clickedBlock = event.getClickedBlock();

            if (clickedBlock != null) {
                player.sendMessage(Component.text("===========================", NamedTextColor.GREEN));
                player.sendMessage(Component.text("isLockable: ").append(formatBoolean(PadlockAPI.isLockable(clickedBlock))));
                player.sendMessage(Component.text("isProtected: ").append(formatBoolean(PadlockAPI.isProtected(clickedBlock))));
                player.sendMessage(Component.text(" - isOwner/User: ").append(formatBoolean(PadlockAPI.isOwner(clickedBlock, player.getUniqueId()))).
                    append(Component.text("/")).append(formatBoolean(PadlockAPI.isMember(clickedBlock, player.getUniqueId()))));
                if (clickedBlock.getState() instanceof Sign sign && PadlockAPI.isLockSign(sign)) {
                    player.sendMessage(Component.text("isSignExpired: ").append(formatBoolean(PadlockAPI.isSignExpired(sign))));
                    player.sendMessage(Component.text(" - last used: " + SignExpiration.getLastUsed(sign)));
                    player.sendMessage(Component.text(" - now      : " + System.currentTimeMillis()));
                }

                player.sendMessage("Block: " + clickedBlock.getType() + " " + clickedBlock.getBlockData().getAsString());

                if (Tag.WALL_SIGNS.isTagged(clickedBlock.getType())) {
                    for (Component line : ((Sign) clickedBlock.getState()).getSide(Side.FRONT).lines()) {
                        player.sendMessage(line.color(NamedTextColor.GREEN));
                    }
                }
                player.sendMessage(player.getUniqueId().toString());
            }
        }
    }

    private @NotNull Component formatBoolean(boolean tf) {
        return tf ? Component.text("true", NamedTextColor.GREEN) : Component.text("false", NamedTextColor.RED);
    }
}