package me.crafter.mc.lockettepro.listener;

import me.crafter.mc.lockettepro.LockettePro;
import me.crafter.mc.lockettepro.api.LocketteProAPI;
import me.crafter.mc.lockettepro.config.Config;
import me.crafter.mc.lockettepro.dependency.Dependency;
import me.crafter.mc.lockettepro.impl.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BlockPlayerListener implements Listener {
    private final Plugin plugin;

    public BlockPlayerListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // Quick protect for chests
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuickLockChest(PlayerInteractEvent event) {
        // Check quick lock enabled
        if (Config.getQuickProtectAction() == (byte) 0) return;
        // Get player and action info
        Action action = event.getAction();
        Player player = event.getPlayer();
        // Check action correctness
        if (action == Action.RIGHT_CLICK_BLOCK && Tag.SIGNS.isTagged(player.getInventory().getItemInMainHand().getType())) {
            if (player.getGameMode().equals(GameMode.SPECTATOR)) {
                return;
            }
            // Check quick lock action correctness
            if (!((event.getPlayer().isSneaking() && Config.getQuickProtectAction() == (byte) 2) ||
                    (!event.getPlayer().isSneaking() && Config.getQuickProtectAction() == (byte) 1))) return;
            // Check permission 
            if (!player.hasPermission("lockettepro.lock")) return;
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
                if (!signLoc.isEmpty()) return;
                // Check whether this block is lockable
                if (LocketteProAPI.isLockable(block)) { //todo check for legacy additional signs
                    // Is this block already locked?
                    boolean locked = LocketteProAPI.isLocked(block);
                    // Cancel event here
                    event.setCancelled(true);
                    // Check lock info
                    if (!locked && !LocketteProAPI.isUpDownOfLockedDoor(block)) {
                        // Get type
                        Material signType = player.getInventory().getItemInMainHand().getType();
                        // Not locked, not a locked door nearby
                        MiscUtils.removeASign(player);
                        // Send message
                        MiscUtils.sendMessages(player, Config.getLangComp("locked-quick"));
                        // Put sign on
                        Block newsign = MiscUtils.putSignOn(block, blockface, Config.getPrivateString(), player.getName(), signType);
                        Cache.resetCache(block);
                        // Cleanups - old names
                        LockSign.updateNamesByUuid((Sign) newsign.getState());

                        // Cleanups - Expiracy
                        if (Config.isLockExpire()) {
                            // set created to now
                            ExpireSign.updateLineWithTime((Sign) newsign.getState(), player.hasPermission("lockettepro.noexpire")); // set created to -1 (no expire) or now
                        }
                        Dependency.logPlacement(player, newsign);
                    } else if (!locked && LocketteProAPI.isOwnerUpDownLockedDoor(block, player)) {
                        // Not locked, (is locked door nearby), is owner of locked door nearby
                        MiscUtils.removeASign(player);
                        MiscUtils.sendMessages(player, Config.getLangComp("additional-sign-added-quick"));
                        MiscUtils.putSignOn(block, blockface, Config.getDefaultAdditionalString(), "", player.getInventory().getItemInMainHand().getType());
                        Dependency.logPlacement(player, block.getRelative(blockface));
                    } else {
                        // Cannot lock this block
                        MiscUtils.sendMessages(player, Config.getLangComp("cannot-lock-quick"));
                    }
                }
            }
        }
    }

    // Manual protection
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onManualLock(@NotNull SignChangeEvent event) {
        if (!Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) return;

        Component component = event.line(0);
        if (component == null) return;
        String topline = PlainTextComponentSerializer.plainText().serialize(component);
        Player player = event.getPlayer();
        /*  Issue #46 - Old version of Minecraft trim signs in unexpected way.
         *  This is caused by Minecraft was doing: (unconfirmed but seemingly)
         *  Place Sign -> Event Fire -> Trim Sign
         *  The event.getLine() will be inaccurate if the line has white space to trim
         *
         *  This will cause player without permission will be able to lock chests by
         *  adding a white space after the [private] word.
         *  Currently, this is fixed by using trimmed line in checking permission. Trimmed
         *  line should not be used anywhere else.
         */
        if (!player.hasPermission("lockettepro.lock")) {
            String toplinetrimmed = topline.trim();
            if (LocketteProAPI.isLockString(toplinetrimmed)) {
                event.line(0, Config.getLangComp("sign-error"));
                MiscUtils.sendMessages(player, Config.getLangComp("cannot-lock-manual"));
                return;
            }
        }
        if (LocketteProAPI.isLockString(topline)) {
            Block block = LocketteProAPI.getAttachedBlock(event.getBlock());
            if (block != null && LocketteProAPI.isLockable(block)) {
                if (Dependency.isProtectedFrom(block, player)) { // External check here
                    event.line(0, Config.getLangComp("sign-error"));
                    MiscUtils.sendMessages(player, Config.getLangComp("cannot-lock-manual"));
                    return;
                }
                boolean locked = LocketteProAPI.isLocked(block);
                if (!locked && !LocketteProAPI.isUpDownOfLockedDoor(block)) {
                    if (LocketteProAPI.isLockString(topline)) {
                        Sign sign = (Sign) event.getBlock().getState();
                        sign.setWaxed(true);
                        sign.update();

                        MiscUtils.sendMessages(player, Config.getLangComp("locked-manual"));
                        if (!player.hasPermission("lockettepro.lockothers")) { // Player with permission can lock with another name
                            event.line(1, Component.text(player.getName()));
                        }
                        Cache.resetCache(block);
                    } else {
                        MiscUtils.sendMessages(player, Config.getLangComp("not-locked-yet-manual"));
                        event.line(0, Config.getLangComp("sign-error"));
                    }
                } else if (!locked && LocketteProAPI.isOwnerUpDownLockedDoor(block, player)) {
                    if (LocketteProAPI.isLockString(topline)) {
                        MiscUtils.sendMessages(player, Config.getLangComp("cannot-lock-door-nearby-manual"));
                        event.line(0, Config.getLangComp("sign-error"));
                    } else {
                        MiscUtils.sendMessages(player, Config.getLangComp("additional-sign-added-manual"));
                    }
                } else if (LocketteProAPI.isOwner(block, player)) {
                    if (LocketteProAPI.isLockString(topline)) {
                        MiscUtils.sendMessages(player, Config.getLangComp("block-already-locked-manual"));
                        event.line(0, Config.getLangComp("sign-error"));
                    } else {
                        MiscUtils.sendMessages(player, Config.getLangComp("additional-sign-added-manual"));
                    }
                } else { // Not possible to fall here except override
                    MiscUtils.sendMessages(player, Config.getLangComp("block-already-locked-manual"));
                    event.getBlock().breakNaturally();
                    MiscUtils.playAccessDenyEffect(player, block);
                }
            } else {
                MiscUtils.sendMessages(player, Config.getLangComp("block-is-not-lockable"));
                event.line(0, Config.getLangComp("sign-error"));
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
            if (!player.hasPermission("lockettepro.edit")) return;

            if (clickedBlock.getState() instanceof Sign sign &&
                    (LocketteProAPI.isOwnerOfSign(sign, player) ||
                            ((LocketteProAPI.isLockSign(sign) || LocketteProAPI.isAdditionalSign(sign)) && player.hasPermission("lockettepro.admin.edit")))) {
                SignSelection.selectSign(player, clickedBlock);
                MiscUtils.sendMessages(player, Config.getLangComp("sign-selected"));
                MiscUtils.playLockEffect(player, clickedBlock);
            }
        }
    }

    // Player break sign
    @EventHandler(priority = EventPriority.HIGH)
    private void onAttemptBreakSign(@NotNull BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.hasPermission("lockettepro.admin.break")) return;

        if (block.getState() instanceof Sign sign) {
            if (LocketteProAPI.isLockSign(sign)) {
                if (LocketteProAPI.isOwnerOfSign(sign, player)) {
                    MiscUtils.sendMessages(player, Config.getLangComp("break-own-lock-sign"));
                    Cache.resetCache(LocketteProAPI.getAttachedBlock(block));
                    // Remove additional signs?
                } else {
                    MiscUtils.sendMessages(player, Config.getLangComp("cannot-break-this-lock-sign"));
                    event.setCancelled(true);
                    MiscUtils.playAccessDenyEffect(player, block);
                }
            } else if (LocketteProAPI.isAdditionalSign(sign)) {// todo update additional signs

                if (!LocketteProAPI.isLocked(LocketteProAPI.getAttachedBlock(block))) {
                    // phew, the locked block is expired!
                    // nothing
                } else if (LocketteProAPI.isOwnerOfSign(sign, player)) {
                    MiscUtils.sendMessages(player, Config.getLangComp("break-own-additional-sign"));
                } else if (!LocketteProAPI.isProtected(LocketteProAPI.getAttachedBlock(block))) {
                    MiscUtils.sendMessages(player, Config.getLangComp("break-redundant-additional-sign"));
                } else {
                    MiscUtils.sendMessages(player, Config.getLangComp("cannot-break-this-additional-sign"));
                    event.setCancelled(true);
                    MiscUtils.playAccessDenyEffect(player, block);
                }
            }
        }
    }

    //protect sign from being changed
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onAttemptChangeLockerSign(@NotNull SignChangeEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign sign &&
                (LocketteProAPI.isLockSign(sign) || LocketteProAPI.isAdditionalSign(sign))) {
            sign.setWaxed(true);
            sign.update();
            event.setCancelled(true);

            // langua added this, however it doesn't seem on brant since
            // a.) everything works as intended
            // b.) smoke particles are used nowhere else
            //block.getWorld().spawnParticle(Particle.SMOKE_NORMAL, block.getLocation(), 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onAttemptBreakWaxedLockerSign(@NotNull PlayerInteractEvent event) {
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK &&
                (block.getState() instanceof Sign sign &&
                        (LocketteProAPI.isLockSign(sign) || LocketteProAPI.isAdditionalSign(sign))) &&
                event.getItem() != null && Tag.ITEMS_AXES.isTagged(event.getItem().getType())) {
            event.setCancelled(true);
        }
    }

    // Protect block from being destroyed
    @EventHandler(priority = EventPriority.HIGH)
    private void onAttemptBreakLockedBlocks(@NotNull BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (LocketteProAPI.isLocked(block) || LocketteProAPI.isUpDownOfLockedDoor(block)) {
            MiscUtils.sendMessages(player, Config.getLangComp("block-is-locked"));
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
        if (LockettePro.needCheckHand()) {
            if (event.getHand() != EquipmentSlot.HAND) {
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    if (LocketteProAPI.isChest(block)) {
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
                if (((LocketteProAPI.isLocked(block) && !LocketteProAPI.isMember(block, player)) ||
                        (LocketteProAPI.isUpDownOfLockedDoor(block) && !LocketteProAPI.isUserUpDownLockedDoor(block, player)))
                        && !player.hasPermission("lockettepro.admin.use")) {
                    MiscUtils.sendMessages(player, Config.getLangComp("block-is-locked"));
                    event.setCancelled(true);
                    MiscUtils.playAccessDenyEffect(player, block);
                } else { // Handle double doors
                    if (action == Action.RIGHT_CLICK_BLOCK) {
                        if ((Doors.isDoubleDoorBlock(block) || Doors.isSingleDoorBlock(block)) && LocketteProAPI.isLocked(block)) {
                            Block doorblock = Doors.getBottomDoorBlock(block);
                            int closetime = LocketteProAPI.getTimerDoor(doorblock);
                            List<Block> doors = new ArrayList<>();
                            doors.add(doorblock);
                            if (doorblock.getType() == Material.IRON_DOOR || doorblock.getType() == Material.IRON_TRAPDOOR) {
                                Doors.toggleDoor(doorblock);
                            }
                            for (BlockFace blockface : LocketteProAPI.cardinalFaces) {
                                Block relative = doorblock.getRelative(blockface);
                                if (relative.getType() == doorblock.getType()) {
                                    doors.add(relative);
                                    Doors.toggleDoor(relative);
                                }
                            }
                            if (closetime > 0) {
                                for (Block door : doors) {
                                    if (door.hasMetadata("lockettepro.toggle")) {
                                        return;
                                    }
                                }
                                for (Block door : doors) {
                                    door.setMetadata("lockettepro.toggle", new FixedMetadataValue(plugin, true));
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
    @EventHandler(priority = EventPriority.HIGH)
    private void onAttemptPlaceInterfereBlocks(@NotNull BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.hasPermission("lockettepro.admin.interfere")) return;
        if (LocketteProAPI.mayInterfere(block, player)) {
            MiscUtils.sendMessages(player, Config.getLangComp("cannot-interfere-with-others"));
            event.setCancelled(true);
            MiscUtils.playAccessDenyEffect(player, block);
        }
    }

    // Tell player about lockettepro
    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlaceFirstBlockNotify(@NotNull BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (!player.hasPermission("lockettepro.lock")) return;
        if (MiscUtils.shouldNotify(player) && Config.isLockable(block.getType())) {
            switch (Config.getQuickProtectAction()) {
                case (byte) 0 -> MiscUtils.sendMessages(player, Config.getLangComp("you-can-manual-lock-it"));
                case (byte) 1, (byte) 2 -> MiscUtils.sendMessages(player, Config.getLangComp("you-can-quick-lock-it"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (LocketteProAPI.isProtected(block) && !(LocketteProAPI.isOwner(block, player) ||
                (block.getState() instanceof Sign sign && LocketteProAPI.isOwnerOfSign(sign, player)))) {
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
        if (LocketteProAPI.isProtected(block) && !(LocketteProAPI.isOwner(block, player) ||
                (block.getState() instanceof Sign sign && LocketteProAPI.isOwnerOfSign(sign, player)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onLecternTake(@NotNull PlayerTakeLecternBookEvent event) {
        Player player = event.getPlayer();
        Block block = event.getLectern().getBlock();
        if (LocketteProAPI.isProtected(block) && !(LocketteProAPI.isOwner(block, player) ||
                (block.getState() instanceof Sign sign && LocketteProAPI.isOwnerOfSign(sign, player)))) {
            event.setCancelled(true);
        }
    }
}
