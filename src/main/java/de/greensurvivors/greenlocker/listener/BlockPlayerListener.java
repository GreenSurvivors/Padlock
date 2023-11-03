package de.greensurvivors.greenlocker.listener;

import de.greensurvivors.greenlocker.Dependency;
import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
import de.greensurvivors.greenlocker.config.ConfigManager;
import de.greensurvivors.greenlocker.config.MessageManager;
import de.greensurvivors.greenlocker.config.PermissionManager;
import de.greensurvivors.greenlocker.impl.Cache;
import de.greensurvivors.greenlocker.impl.MiscUtils;
import de.greensurvivors.greenlocker.impl.doordata.DoorToggleTask;
import de.greensurvivors.greenlocker.impl.doordata.Doors;
import de.greensurvivors.greenlocker.impl.signdata.ExpireSign;
import de.greensurvivors.greenlocker.impl.signdata.LockSign;
import de.greensurvivors.greenlocker.impl.signdata.SignSelection;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlockPlayerListener implements Listener { //todo this whole class
    private final GreenLocker plugin;

    public BlockPlayerListener(GreenLocker plugin) {
        this.plugin = plugin;
    }

    // Quick protect for chests
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerQuickLockChest(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check quick lock status
        switch (plugin.getConfigManager().getQuickProtectAction()) {
            case OFF -> {
                return;
            }
            case SNEAK_REQUIRED -> {
                if (!player.isSneaking()) {
                    return;
                }
            }
            case NOT_SNEAKING_REQUIRED -> {
                if (player.isSneaking()) {
                    return;
                }
            }
        }

        // Get player and action info
        Action action = event.getAction();
        // Check action correctness
        if (action == Action.RIGHT_CLICK_BLOCK && Tag.SIGNS.isTagged(player.getInventory().getItemInMainHand().getType())) {
            if (player.getGameMode().equals(GameMode.SPECTATOR)) {
                return;
            }
            // Check permission 
            if (player.hasPermission(PermissionManager.actionLock.getPerm())) {
                // Get target block to lock
                BlockFace blockface = event.getBlockFace();
                if (blockface == BlockFace.NORTH || blockface == BlockFace.WEST || blockface == BlockFace.EAST || blockface == BlockFace.SOUTH) {
                    Block block = event.getClickedBlock();
                    if (block == null) return;
                    // Check permission with external plugin
                    if (Dependency.isProtectedFrom(block, player)) return; // blockwise
                    if (Dependency.isProtectedFrom(block.getRelative(event.getBlockFace()), player)) return; // signwise
                    // Check whether locking location is obstructed
                    Block signLoc = block.getRelative(blockface);
                    if (signLoc.isEmpty()) {
                        // Check whether this block is lockable
                        if (GreenLockerAPI.isLockable(block)) { //todo check for legacy additional signs
                            // Is this block already locked?
                            boolean locked = GreenLockerAPI.isLocked(block);
                            // Cancel event here
                            event.setCancelled(true);
                            // Check lock info
                            if (!locked && !GreenLockerAPI.isPartOfLockedDoor(block)) {
                                // Get type
                                Material signType = player.getInventory().getItemInMainHand().getType();
                                // Not locked, not a locked door nearby
                                MiscUtils.removeASign(player);
                                // Put sign on
                                Block newsign = MiscUtils.putSignOn(block, blockface, plugin.getMessageManager().getLang(MessageManager.LangPath.PRIVATE_SIGN), player.name(), signType);
                                Cache.resetCache(block);
                                // Send message
                                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_SUCCESS);
                                // Cleanups - old names
                                LockSign.updateNamesByUuid((Sign) newsign.getState());

                                // Cleanups - Expiracy
                                if (plugin.getConfigManager().doLocksExpire()) {
                                    // set created to now
                                    ExpireSign.updateLineWithTime((Sign) newsign.getState(), player.hasPermission(PermissionManager.NO_EXPIRE.getPerm())); // set created to -1 (no expire) or now
                                }
                                Dependency.logPlacement(player, newsign);
                            } else {
                                // Cannot lock this block
                                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.QUICK_LOCK_ERROR);
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual protection
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onManualLock(@NotNull SignChangeEvent event) { //todo correct signLines according to correct casing;
        if (!Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) return;

        Component topline = event.line(0);
        if (topline == null) return;
        Player player = event.getPlayer();

        if (GreenLockerAPI.isLockComp(topline)) {
            Block block = GreenLockerAPI.getAttachedBlock(event.getBlock());
            if (block != null && GreenLockerAPI.isLockable(block)) {
                boolean locked = GreenLockerAPI.isLocked(block);
                if (!locked && !GreenLockerAPI.isPartOfLockedDoor(block)) { //todo reorganize this
                    if (GreenLockerAPI.isLockComp(topline)) {
                        Sign sign = (Sign) event.getBlock().getState();
                        sign.setWaxed(true);
                        sign.update();

                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_SUCCESS);
                        if (!player.hasPermission(PermissionManager.actionLockOthers.getPerm())) { // Player with permission can lock with another name
                            event.line(1, Component.text(player.getName()));
                        }
                        Cache.resetCache(block);
                    }
                } else if (!locked && GreenLockerAPI.isOwnerUpDownLockedDoor(block, player)) {
                    if (GreenLockerAPI.isLockComp(topline)) {
                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_ALREADY_LOCKED);
                        event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.ERROR_SIGN));
                    }
                } else if (GreenLockerAPI.isOwner(block, player)) {
                    if (GreenLockerAPI.isLockComp(topline)) {
                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_ALREADY_LOCKED);
                        event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.ERROR_SIGN));
                    }
                } else { // Not possible to fall here except override
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_ALREADY_LOCKED);
                    event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.ERROR_SIGN));
                    MiscUtils.playAccessDenyEffect(player, block);
                }
            } else {
                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_NOT_LOCKABLE);
                event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.ERROR_SIGN));
                MiscUtils.playAccessDenyEffect(player, block);
            }
        }
    }

    // Player select sign
    @EventHandler(priority = EventPriority.LOW)
    private void playerSelectSign(@NotNull PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && Tag.WALL_SIGNS.isTagged(clickedBlock.getType())) {
            Player player = event.getPlayer();
            if (!player.hasPermission(PermissionManager.EDIT.getPerm())) return;

            if (clickedBlock.getState() instanceof Sign sign &&
                    (GreenLockerAPI.isOwnerOfSign(sign, player) ||
                            ((GreenLockerAPI.isLockSign(sign) || GreenLockerAPI.isAdditionalSign(sign)) && player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm())))) {
                SignSelection.selectSign(player, clickedBlock);
                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.SELECT_SIGN);
                MiscUtils.playLockEffect(player, clickedBlock);
            }
        }
    }

    // Player break sign
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onAttemptBreakSign(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm())) return;

        if (block.getState() instanceof Sign sign) {
            if (GreenLockerAPI.isLockSign(sign)) {
                if (GreenLockerAPI.isOwnerOfSign(sign, player)) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.BREAK_LOCK_SUCCESS);
                    Cache.resetCache(GreenLockerAPI.getAttachedBlock(block));
                } else {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOT_OWNER);
                    event.setCancelled(true);
                    MiscUtils.playAccessDenyEffect(player, block);
                }
            } else if (GreenLockerAPI.isAdditionalSign(sign)) {
                if (GreenLockerAPI.isOwnerOfSign(sign, player)) {
                    //todo update but don't add members of this additional sign
                } else {
                    GreenLockerAPI.updateLegacySign(sign);
                    event.setCancelled(true);
                    MiscUtils.playAccessDenyEffect(player, block);
                }
            }
        }
    }

    //protect sign from being changed
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onAttemptChangeLockerSign(@NotNull SignChangeEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign sign && (GreenLockerAPI.isLockSign(sign))) {
            sign.setWaxed(true);
            sign.update();
            event.setCancelled(true);

            // langua added this, however it doesn't seem on brant since
            // a.) everything works as intended
            // b.) smoke particles are used nowhere else
            //block.getWorld().spawnParticle(Particle.SMOKE_NORMAL, block.getLocation(), 5);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onAttemptBreakWaxedLockerSign(@NotNull PlayerInteractEvent event) {
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK &&
                (block.getState() instanceof Sign sign &&
                        (GreenLockerAPI.isLockSign(sign) || GreenLockerAPI.isAdditionalSign(sign))) &&
                event.getItem() != null && Tag.ITEMS_AXES.isTagged(event.getItem().getType())) {
            event.setCancelled(true);
        }
    }

    // Protect block from being destroyed
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onAttemptBreakLockedBlocks(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (GreenLockerAPI.isLocked(block) || GreenLockerAPI.isPartOfLockedDoor(block)) {
            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
            event.setCancelled(true);
            MiscUtils.playAccessDenyEffect(player, block);
        }
    }

    // Protect block from being used & handle double doors
    @EventHandler(priority = EventPriority.HIGH)
    private void onAttemptInteractLockedBlocks(@NotNull PlayerInteractEvent event) {
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (block == null) return;
        if (GreenLocker.needCheckHand()) {
            if (event.getHand() != EquipmentSlot.HAND) {
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    if (GreenLockerAPI.isChest(block)) {
                        // something not right
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
        switch (action) {
            case LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK -> {
                Player player = event.getPlayer();
                if (((GreenLockerAPI.isLocked(block) && !GreenLockerAPI.isMember(block, player)) ||
                        (GreenLockerAPI.isPartOfLockedDoor(block) && !GreenLockerAPI.isUserUpDownLockedDoor(block, player)))
                        && !player.hasPermission(PermissionManager.ADMIN_USE.getPerm())) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
                    event.setCancelled(true);
                    MiscUtils.playAccessDenyEffect(player, block);
                } else { // Handle double doors
                    if (action == Action.RIGHT_CLICK_BLOCK) {
                        if ((Doors.isDoubleDoorBlock(block) || Doors.isSingleDoorBlock(block)) && GreenLockerAPI.isLocked(block)) {
                            Block doorblock = Doors.getBottomDoorBlock(block);
                            int closetime = GreenLockerAPI.getTimerDoor(doorblock);
                            List<Block> doors = new ArrayList<>();
                            doors.add(doorblock);
                            if (doorblock.getType() == Material.IRON_DOOR || doorblock.getType() == Material.IRON_TRAPDOOR) {
                                Doors.toggleDoor(doorblock);
                            }
                            for (BlockFace blockface : GreenLockerAPI.cardinalFaces) {
                                Block relative = doorblock.getRelative(blockface);
                                if (relative.getType() == doorblock.getType()) {
                                    doors.add(relative);
                                    Doors.toggleDoor(relative);
                                }
                            }
                            if (closetime > 0) {
                                for (Block door : doors) {
                                    if (door.hasMetadata("greenlocker.toggle")) {
                                        return;
                                    }
                                }
                                for (Block door : doors) {
                                    door.setMetadata("greenlocker.toggle", new FixedMetadataValue(plugin, true));
                                }
                                Bukkit.getScheduler().runTaskLater(plugin, new DoorToggleTask(plugin, doors), closetime * 20L);
                            }
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    // Protect block from interfere block
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onAttemptPlaceInterfereBlocks(@NotNull BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!player.hasPermission(PermissionManager.ADMIN_INTERFERE.getPerm())) {
            if (GreenLockerAPI.mayInterfere(block, player)) {
                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_INTERFERE);
                event.setCancelled(true);
                MiscUtils.playAccessDenyEffect(player, block);
            }
        }
    }

    // Tell player about greenlocker
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlaceFirstBlockNotify(@NotNull BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.hasPermission(PermissionManager.actionLock.getPerm())) {
            if (MiscUtils.shouldNotify(player) && plugin.getConfigManager().isLockable(block.getType())) {
                if (Objects.requireNonNull(plugin.getConfigManager().getQuickProtectAction()) == ConfigManager.QuickProtectOption.OFF) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOTICE_MANUEL_LOCK);
                } else {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOTICE_QUICK_LOCK);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        if (GreenLockerAPI.isProtected(block) && !(GreenLockerAPI.isOwner(block, player) ||
                (block.getState() instanceof Sign sign && GreenLockerAPI.isOwnerOfSign(sign, player)))) {
            event.setCancelled(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isDead()) {
                        player.updateInventory();
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onBucketUse(@NotNull PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (GreenLockerAPI.isProtected(block) && !(GreenLockerAPI.isOwner(block, player) ||
                (block.getState() instanceof Sign sign && GreenLockerAPI.isOwnerOfSign(sign, player)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onLecternTake(@NotNull PlayerTakeLecternBookEvent event) {
        Player player = event.getPlayer();
        Block block = event.getLectern().getBlock();
        if (GreenLockerAPI.isProtected(block) && !(GreenLockerAPI.isOwner(block, player) ||
                (block.getState() instanceof Sign sign && GreenLockerAPI.isOwnerOfSign(sign, player)))) {
            event.setCancelled(true);
        }
    }
}
