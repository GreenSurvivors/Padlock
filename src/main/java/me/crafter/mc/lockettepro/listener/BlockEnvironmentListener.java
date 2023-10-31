package me.crafter.mc.lockettepro.listener;

import me.crafter.mc.lockettepro.LockettePro;
import me.crafter.mc.lockettepro.LocketteProAPI;
import me.crafter.mc.lockettepro.impl.doordata.Doors;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class BlockEnvironmentListener implements Listener {
    private final LockettePro plugin;

    public BlockEnvironmentListener(LockettePro plugin) {
        this.plugin = plugin;
    }

    // Prevent explosion break block
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted("explosion")) {
            event.blockList().removeIf(LocketteProAPI::isProtected);
        }
    }

    // Prevent bed / respawn anchor break block
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted("explosion")) {
            event.blockList().removeIf(LocketteProAPI::isProtected);
        }
    }

    // Prevent tree break block
    @EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted("growth")) {
            for (BlockState blockstate : event.getBlocks()) {
                if (LocketteProAPI.isProtected(blockstate.getBlock())) {
                    event.setCancelled(true);
                    return;
                }
            }
        } // exempted
    }

    // Prevent piston extend break lock
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted("piston")) {
            for (Block block : event.getBlocks()) {
                if (LocketteProAPI.isProtected(block)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } // exempted
    }

    // Prevent piston retract break lock
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted("piston")) {
            for (Block block : event.getBlocks()) {
                if (LocketteProAPI.isProtected(block)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } // exempted
    }

    // Prevent redstone current open doors
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted("redstone")) {
            if (LocketteProAPI.isProtected(event.getBlock())) {
                event.setNewCurrent(event.getOldCurrent());
            }
        }
    }

    // Prevent villager open door
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerOpenDoor(EntityInteractEvent event) {
        if (plugin.getConfigManager().isProtectionExempted("villager")) return;
        // Explicitly to villager vs all doors
        if (event.getEntity() instanceof Villager &&
                (Doors.isSingleDoorBlock(event.getBlock()) || Doors.isDoubleDoorBlock(event.getBlock())) &&
                LocketteProAPI.isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // Prevent mob change block
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobChangeBlock(EntityChangeBlockEvent event) { //todo there are so many more mobgriefs
        if ((event.getEntity() instanceof Enderman && !plugin.getConfigManager().isProtectionExempted("enderman")) ||// enderman pick up/place block
                (event.getEntity() instanceof Wither && !plugin.getConfigManager().isProtectionExempted("wither")) ||// wither break block
                (event.getEntity() instanceof Zombie && !plugin.getConfigManager().isProtectionExempted("zombie")) ||// zombie break door
                (event.getEntity() instanceof Silverfish && !plugin.getConfigManager().isProtectionExempted("silverfish"))) {
            if (LocketteProAPI.isProtected(event.getBlock())) {
                event.setCancelled(true);
            }
        }// ignore other reason (boat break lily pad, arrow ignite tnt, etc.)
    }
}
