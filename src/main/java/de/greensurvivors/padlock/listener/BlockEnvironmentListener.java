package de.greensurvivors.padlock.listener;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.ConfigManager;
import de.greensurvivors.padlock.impl.openabledata.Openables;
import org.bukkit.Tag;
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

/**
 * checks for everything around a block;
 * everything here can get exempted in the config.
 */
public class BlockEnvironmentListener implements Listener {
    private final Padlock plugin;

    public BlockEnvironmentListener(Padlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent explosion break block
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.EXPLOSION)) {
            event.blockList().removeIf(PadlockAPI::isProtected);
        }
    }

    /**
     * Prevent bed / respawn anchor break block
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.EXPLOSION)) {
            event.blockList().removeIf(PadlockAPI::isProtected);
        }
    }

    /**
     * Prevent trees / mushroom growth break block
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.GROWTH)) {
            for (BlockState blockstate : event.getBlocks()) {
                if (PadlockAPI.isProtected(blockstate.getBlock())) {
                    event.setCancelled(true);
                    return;
                }
            }
        } // exempted
    }

    /**
     * Prevent piston extend break lock
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.PISTON)) {
            for (Block block : event.getBlocks()) {
                if (PadlockAPI.isProtected(block)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } // exempted
    }

    /**
     * Prevent piston retract break lock
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.PISTON)) {
            for (Block block : event.getBlocks()) {
                if (PadlockAPI.isProtected(block)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } // exempted
    }

    /**
     * Prevent redstone current open (tap)doors / fence gates,
     * however does not prevent dispensers / droppers to do their thing.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        if (!plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.REDSTONE)) {
            Block block = event.getBlock();

            if (plugin.getConfigManager().isCacheEnabled() && plugin.getLockCacheManager().tryGetProtectedFromCache(block)) { // Cache is enabled
                event.setNewCurrent(event.getOldCurrent());
            } else if (PadlockAPI.isProtected(block)) { // Cache is disabled
                event.setNewCurrent(event.getOldCurrent());
            }
        } // redstone exempted
    }

    /**
     * Prevent villager open door
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerOpenDoor(EntityInteractEvent event) {
        if (plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.VILLAGER)) return;
        // Explicitly to villager vs all doors
        if (event.getEntity() instanceof Villager &&
                (Openables.isSingleOpenable(event.getBlock().getType()) || Tag.DOORS.isTagged(event.getBlock().getType())) &&
                PadlockAPI.isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent mob change block
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobChangeBlock(EntityChangeBlockEvent event) {
        if ((event.getEntity() instanceof Enderman && !plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.ENDERMAN)) ||// enderman pick up/place block
                (event.getEntity() instanceof EnderDragon && !plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.ENDER_DRAGON)) || // ender dragons break blocks
                (event.getEntity() instanceof Wither && !plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.WITHER)) ||// wither break block
                (event.getEntity() instanceof Zombie && !plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.ZOMBIE)) ||// zombie break door
                (event.getEntity() instanceof Silverfish && !plugin.getConfigManager().isProtectionExempted(ConfigManager.ProtectionExemption.SILVERFISH))) {
            if (PadlockAPI.isProtected(event.getBlock())) {
                event.setCancelled(true);
            }
        }// ignore other reason (boat break lily pad, arrow ignite tnt, rabbit eating carrots, sheep eating grass etc.)
    }
}
