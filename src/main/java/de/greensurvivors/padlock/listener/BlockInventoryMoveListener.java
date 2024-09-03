package de.greensurvivors.padlock.listener;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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
            plugin.getConfigManager().getHopperMinecartAction() != ConfigManager.HopperMinecartMoveItemOption.ALLOWED) {
            if (isInventoryLocked(event.getSource())) {
                if (plugin.getConfigManager().isItemTransferOutBlocked()) {
                    event.setCancelled(true);
                }
                // Additional Hopper Minecart Check
                if (event.getDestination().getHolder() instanceof HopperMinecart hopperMinecart) {
                    ConfigManager.HopperMinecartMoveItemOption hopperminecartaction = plugin.getConfigManager().getHopperMinecartAction();
                    switch (hopperminecartaction) { // note: hopper minecarts don't have cooldowns, if you experience lag you should turn the remove option on
                        case BLOCKED -> // Cancel only, it is not called if !Config.isItemTransferOutBlocked()
                                event.setCancelled(true);
                        case REMOVE -> { // Extra action - HopperMinecart removal
                            event.setCancelled(true);
                            // just removing the entity doesn't drop the minecart-item itself, so we dropping it manually
                            hopperMinecart.getWorld().dropItemNaturally(hopperMinecart.getLocation(), new ItemStack(Material.HOPPER_MINECART));
                            hopperMinecart.remove();
                        }
                    }
                } else if (event.getDestination().getHolder() instanceof Hopper hopper) {
                    // don't check this hopper again for some time, don't waste compute time
                    hopper.setTransferCooldown(plugin.getConfigManager().getItemTransferCooldown());
                }

                return;
            }
        }

        if (plugin.getConfigManager().isItemTransferInBlocked()) {
            if (isInventoryLocked(event.getDestination())) {
                event.setCancelled(true);

                // don't check this hopper again for some time, don't waste compute time
                if (event.getSource().getHolder() instanceof Hopper hopper) {
                    hopper.setTransferCooldown(plugin.getConfigManager().getItemTransferCooldown());
                }
            }
        }
    }

    /**
     * @param inventory
     * @return
     */
    public boolean isInventoryLocked(@NotNull Inventory inventory) {
        // get holder
        InventoryHolder inventoryholder = inventory.getHolder();
        if (inventoryholder instanceof DoubleChest) {
            inventoryholder = ((DoubleChest) inventoryholder).getLeftSide();
        }

        // get lock
        if (inventoryholder instanceof BlockState blockState) {
            if (plugin.getConfigManager().isCacheEnabled()) { // Cache is enabled
                return plugin.getLockCacheManager().getProtectedFromCache(blockState.getLocation()).isLock();
            } else { // Cache is disabled
                return PadlockAPI.isProtected(blockState.getBlock());
            }
        } //entities are not lockable
        return false;
    }
}
