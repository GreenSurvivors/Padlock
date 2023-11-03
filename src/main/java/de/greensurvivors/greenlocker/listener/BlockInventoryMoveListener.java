package de.greensurvivors.greenlocker.listener;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.ConfigManager;
import de.greensurvivors.greenlocker.impl.Cache;
import de.greensurvivors.greenlocker.impl.MiscUtils;
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

public class BlockInventoryMoveListener implements Listener {
    private final GreenLocker plugin;

    public BlockInventoryMoveListener(GreenLocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (plugin.getConfigManager().isItemTransferOutBlocked() || plugin.getConfigManager().getHopperMinecartAction() != ConfigManager.HopperMinecartBlockedOption.FALSE) {
            if (isInventoryLocked(event.getSource())) {
                if (plugin.getConfigManager().isItemTransferOutBlocked()) {
                    event.setCancelled(true);
                }
                // Additional Hopper Minecart Check
                if (event.getDestination().getHolder() instanceof HopperMinecart) {
                    ConfigManager.HopperMinecartBlockedOption hopperminecartaction = plugin.getConfigManager().getHopperMinecartAction();
                    switch (hopperminecartaction) {
                        // case 0 - Impossible
                        case TRUE -> // Cancel only, it is not called if !Config.isItemTransferOutBlocked()
                                event.setCancelled(true);
                        case REMOVE -> { // Extra action - HopperMinecart removal
                            event.setCancelled(true);
                            ((HopperMinecart) event.getDestination().getHolder()).remove(); //todo test if this drops the cart and its items
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

    public boolean isInventoryLocked(Inventory inventory) {
        InventoryHolder inventoryholder = inventory.getHolder();
        if (inventoryholder instanceof DoubleChest) {
            inventoryholder = ((DoubleChest) inventoryholder).getLeftSide();
        }
        if (inventoryholder instanceof BlockState) {
            Block block = ((BlockState) inventoryholder).getBlock();
            if (plugin.getConfigManager().isCacheEnabled()) { // Cache is enabled
                if (Cache.hasValidCache(block)) {
                    return MiscUtils.getAccess(block);
                } else {
                    if (GreenLockerAPI.isLocked(block)) {
                        Cache.setCache(block, true);
                        return true;
                    } else {
                        Cache.setCache(block, false);
                        return false;
                    }
                }
            } else { // Cache is disabled
                return GreenLockerAPI.isLocked(block);
            }
        }
        return false;
    }
}
