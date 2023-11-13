package de.greensurvivors.padlock.listener;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * protects against blocks / non player entities taking items out / putting in a locked block
 */
public class BlockInventoryMoveListener implements Listener {
    private final Padlock plugin;

    public BlockInventoryMoveListener(Padlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (plugin.getConfigManager().isItemTransferOutBlocked() ||
                plugin.getConfigManager().getHopperMinecartAction() != ConfigManager.HopperMinecartBlockedOption.FALSE) {
            if (isInventoryLocked(event.getSource())) {
                if (plugin.getConfigManager().isItemTransferOutBlocked()) {
                    event.setCancelled(true);
                }
                // Additional Hopper Minecart Check
                if (event.getDestination().getHolder() instanceof HopperMinecart hopperMinecart) {
                    ConfigManager.HopperMinecartBlockedOption hopperminecartaction = plugin.getConfigManager().getHopperMinecartAction();
                    switch (hopperminecartaction) {
                        // case 0 - Impossible
                        case TRUE -> // Cancel only, it is not called if !Config.isItemTransferOutBlocked()
                                event.setCancelled(true);
                        case REMOVE -> { // Extra action - HopperMinecart removal
                            event.setCancelled(true);
                            // just removing the entity doesn't drop the minecart-item itself, so we dropping it manually
                            hopperMinecart.getWorld().dropItemNaturally(hopperMinecart.getLocation(), new ItemStack(Material.HOPPER_MINECART));
                            hopperMinecart.remove();
                        }
                    }
                }

                return;
            }
        }

        if (plugin.getConfigManager().isItemTransferInBlocked()) {
            if (isInventoryLocked(event.getDestination())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * @param inventory
     * @return
     */
    public boolean isInventoryLocked(Inventory inventory) {
        // get holder
        InventoryHolder inventoryholder = inventory.getHolder();
        if (inventoryholder instanceof DoubleChest) {
            inventoryholder = ((DoubleChest) inventoryholder).getLeftSide();
        }

        // get lock
        if (inventoryholder instanceof BlockState blockState) {
            Block block = blockState.getBlock();
            if (plugin.getConfigManager().isCacheEnabled()) { // Cache is enabled
                return plugin.getLockCacheManager().tryGetProtectedFromCache(block);
            } else { // Cache is disabled
                return PadlockAPI.isProtected(block);
            }
        } //entities are not lockable
        return false;
    }
}
