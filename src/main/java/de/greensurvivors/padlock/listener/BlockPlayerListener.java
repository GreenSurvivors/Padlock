package de.greensurvivors.padlock.listener;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.ConfigManager;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.openabledata.Openables;
import de.greensurvivors.padlock.impl.signdata.SignDisplay;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Chest;
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
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * All events a player and a locked block is directly involved in
 * lock creation, lock removes, preventing, sign selection
 */
public class BlockPlayerListener implements Listener {
    private final Padlock plugin;

    public BlockPlayerListener(Padlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Quick aka automatic protect
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onQuickLock(PlayerInteractEvent event) {
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

        if (!player.getGameMode().equals(GameMode.SPECTATOR)) {
            // Get player and action info
            Action action = event.getAction();

            // Get type
            Material signType = player.getInventory().getItemInMainHand().getType();

            // Check action correctness
            if (action == Action.RIGHT_CLICK_BLOCK && Tag.SIGNS.isTagged(signType)) {
                // Check permission
                if (player.hasPermission(PermissionManager.ACTION_LOCK.getPerm())) {
                    // Get target block to lock
                    BlockFace blockface = event.getBlockFace();
                    if (PadlockAPI.cardinalFaces.contains(blockface)) {
                        Block block = event.getClickedBlock();
                        if (block == null) return;
                        Block signLocBlock = block.getRelative(blockface);

                        // Check permission with external plugin
                        if (plugin.getDependencyManager().isProtectedFrom(block, player)) return; // blockwise
                        if (plugin.getDependencyManager().isProtectedFrom(signLocBlock, player)) return; // signwise

                        // Check whether locking location is obstructed
                        if (signLocBlock.isEmpty()) {
                            // Check whether this block is lockable
                            if (PadlockAPI.isLockable(block)) {
                                // Is this block already locked?
                                boolean locked = PadlockAPI.isProtected(block);
                                // Cancel event here
                                event.setCancelled(true);
                                // Check lock info
                                if (!locked && !PadlockAPI.isPartOfLockedDoor(block)) {
                                    // Not locked, not a locked door nearby
                                    MiscUtils.removeAItemMainHand(player);
                                    // Put sign on
                                    Block newsign = SignLock.putPrivateSignOn(signLocBlock, blockface, signType, player);
                                    plugin.getLockCacheManager().resetCache(block);
                                    // Send message
                                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_SUCCESS);
                                    // Cleanups - Expiracy
                                    if (plugin.getConfigManager().doLocksExpire()) {
                                        // set created to now
                                        //todo --> last login
                                        // SignExpiration.updateWithTimeNow((Sign) newsign.getState(), player.hasPermission(PermissionManager.NO_EXPIRE.getPerm())); // set created to -1 (no expire) or now
                                    }
                                    plugin.getDependencyManager().logPlacement(player, newsign);
                                } else {
                                    // Cannot lock this block
                                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.QUICK_LOCK_ERROR);
                                }
                            }
                        }
                    } // not cardinal face of block
                } // no permission
            } // not right-click or not holding signs
        } // game mode spectator
    }

    /**
     * Manual protection
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onManualLock(@NotNull SignChangeEvent event) {
        if (!Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) return;

        Component topline = event.line(0);
        if (topline == null) return;
        Player player = event.getPlayer();

        if (plugin.getMessageManager().isSignComp(topline, MessageManager.LangPath.PRIVATE_SIGN)) { //todo
            //check attached block
            Block attachedBlock = PadlockAPI.getAttachedBlock(event.getBlock());
            if (attachedBlock != null && PadlockAPI.isLockable(attachedBlock)) {
                if (PadlockAPI.isProtected(attachedBlock)) {
                    if (PadlockAPI.isOwner(attachedBlock, player)) {
                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_ALREADY_LOCKED);
                        event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.ERROR_SIGN));
                    } else {
                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOT_OWNER);
                        event.setCancelled(true);
                    }
                } else { // all good
                    Sign sign = (Sign) event.getBlock().getState();
                    sign.setWaxed(true);
                    sign.update();

                    // Player with this permission can lock with another name
                    if (!player.hasPermission(PermissionManager.ACTION_LOCK_OTHERS.getPerm())) {
                        // set the player self as owner
                        event.line(1, Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.PLAYER_NAME_ON_SIGN,
                                Placeholder.unparsed(MessageManager.PlaceHolder.PLAYER.getPlaceholder(), player.getName())));

                        SignLock.addPlayer(sign, true, player);
                    } else {
                        // first line is Owner
                        Component line = event.line(1);

                        if (line != null) {
                            String strLine = PlainTextComponentSerializer.plainText().serialize(line);

                            if (MiscUtils.isUserName(strLine)) {
                                SignLock.addPlayer(sign, true, Bukkit.getOfflinePlayer(strLine));
                            } else if (strLine.isBlank() && player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm())) { // failsafe: only allow setting a sign without owner if you could break it!
                                SignLock.addPlayer(sign, true, null);
                            } else {
                                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.UNKNOWN_PLAYER,
                                        Placeholder.unparsed(MessageManager.PlaceHolder.PLAYER.getPlaceholder(), strLine));
                                event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.ERROR_SIGN));
                                return;
                            }
                        } else if (player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm())) { // failsafe: only allow setting a sign without owner if you could break it! (this event should never return null here anyway)
                            SignLock.addPlayer(sign, true, null);
                        } else {
                            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_NO_OWNER);
                            event.setCancelled(true);
                            return;
                        }
                    } // owner done

                    //check other lines
                    for (int i = 2; i < event.lines().size(); i++) {
                        Component line = event.line(i);
                        if (line == null) {
                            continue;
                        }

                        String strLine = PlainTextComponentSerializer.plainText().serialize(line);
                        Long timer = SignTimer.getTimerFromComp(line);

                        if (timer != null) {
                            SignTimer.setTimer(sign, timer, true);
                        } else if (MiscUtils.isUserName(strLine)) {
                            SignLock.addPlayer(sign, false, Bukkit.getOfflinePlayer(strLine));
                        } // else invalid line
                    } // for loop

                    //update Sign after event was done
                    Bukkit.getScheduler().runTaskLater(plugin, () -> SignDisplay.updateDisplay(sign), 2);
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_SUCCESS);

                    plugin.getLockCacheManager().resetCache(attachedBlock);
                }
            } else {
                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_NOT_LOCKABLE);
                event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.ERROR_SIGN));
            }
        } // not a lock sign
    }

    /**
     * Player select sign
     */
    @EventHandler(priority = EventPriority.LOW)
    private void playerSelectSign(@NotNull PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && Tag.WALL_SIGNS.isTagged(clickedBlock.getType())) {
            Player player = event.getPlayer();
            if (!player.hasPermission(PermissionManager.EDIT.getPerm())) return;

            // check permission: owner or admin
            if (clickedBlock.getState() instanceof Sign sign &&
                    (PadlockAPI.isLockSign(sign) || PadlockAPI.isAdditionalSign(sign)) /*&&
                    (PadlockAPI.isMember(clickedBlock, player) ||
                            player.hasPermission(PermissionManager.ADMIN_EDIT.getPerm()))*/) {

                SignSelection.selectSign(player, clickedBlock);
                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.SELECT_SIGN);
            }
        }
    }

    /**
     * Player attempts to break block;
     * if it's a sign: checks if the player is allowed to break it,
     * else: if it is locked prevent the block from getting broken
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onAttemptBreakBlock(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getState() instanceof Sign sign) {
            if (PadlockAPI.isValidLockSign(sign)) {
                if (SignLock.isOwner(sign, player.getUniqueId()) || player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm())) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.BREAK_LOCK_SUCCESS);
                    plugin.getLockCacheManager().resetCache(PadlockAPI.getAttachedBlock(block));
                } else { // not allowed to break
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOT_OWNER);
                    event.setCancelled(true);
                }
            } else if (PadlockAPI.isAdditionalSign(sign)) {
                if (PadlockAPI.isOwner(block, player) || player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm())) {
                    final Sign lockSign = PadlockAPI.getLock(PadlockAPI.getAttachedBlock(block));

                    //let the additional sign break but update the others
                    Bukkit.getScheduler().runTaskLater(plugin, () -> PadlockAPI.updateLegacySign(lockSign), 2);
                } else { // not allowed to break
                    PadlockAPI.updateLegacySign(sign);

                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOT_OWNER);
                    event.setCancelled(true);
                }
            }
        } else { // not a sign
            Sign lock = PadlockAPI.getLock(block);
            if (lock != null) {
                if (!(SignLock.isOwner(lock, player.getUniqueId()) || player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm()))) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
                    event.setCancelled(true);
                } else if (!plugin.getDependencyManager().isProtectedFrom(block, player)) { // if the player is allowed to break the block
                    // break lock sign in case it wasn't attached to the block
                    lock.getBlock().breakNaturally();
                }
            }
        }
    }

    /**
     * protect sign from being changed
     */
    @Deprecated(forRemoval = true) // only needed for legacy signs.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onAttemptChangeLockSign(@NotNull SignChangeEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign sign &&
                (PadlockAPI.isLockSign(sign) || PadlockAPI.isAdditionalSign(sign))) {
            plugin.getMessageManager().sendLang(event.getPlayer(), MessageManager.LangPath.ACTION_PREVENTED_USE_CMDS);
            sign.setWaxed(true);
            sign.update();
            event.setCancelled(true);
        }
    }

    /**
     * protect against wax removing
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onAttemptRemoveWaxOfLockerSign(@NotNull PlayerInteractEvent event) {
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK &&
                (block.getState() instanceof Sign sign &&
                        (PadlockAPI.isLockSign(sign) || PadlockAPI.isAdditionalSign(sign))) &&
                event.getItem() != null && Tag.ITEMS_AXES.isTagged(event.getItem().getType())) {
            plugin.getMessageManager().sendLang(event.getPlayer(), MessageManager.LangPath.ACTION_PREVENTED_USE_CMDS);
            event.setCancelled(true);
        }
    }

    /**
     * Protect block from being used
     * & handle connected openables
     */
    @EventHandler(priority = EventPriority.HIGH)
    private void onAttemptInteractLockedBlocks(@NotNull PlayerInteractEvent event) { //todo players need to select even if they have no access right now for passwords
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (block == null) return;

        //don't allow to open locked Chests (via 3rd party plugins?)
        if (event.getHand() != EquipmentSlot.HAND) {
            if (action == Action.RIGHT_CLICK_BLOCK) {
                if (block.getBlockData() instanceof Chest || block.getBlockData() instanceof DoubleChest) {
                    plugin.getLogger().fine("I stopped someone opening a chest without their hands!");
                    // something not right
                    event.setCancelled(true);
                }
                return;
            }
        }
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            Sign lockSign = PadlockAPI.getLock(block);

            if (lockSign != null) {
                if (SignLock.isMember(lockSign, player.getUniqueId()) || SignLock.isOwner(lockSign, player.getUniqueId()) ||
                        player.hasPermission(PermissionManager.ADMIN_USE.getPerm())) {

                    // Handle multi openables
                    if (action == Action.RIGHT_CLICK_BLOCK &&
                            // if a place attempt was prevented, this could register as a false positive and flip every block but the clicked one
                            // therefor everything comes out of sync.
                            !(player.isSneaking() && event.hasItem()) &&
                            // event gets fired for both hands, ignore offhand
                            event.getHand() == EquipmentSlot.HAND) {

                        // get openable block
                        Block openableBlock = null;
                        if (Tag.DOORS.isTagged(block.getType())) {
                            openableBlock = Openables.getDoubleBlockParts(block).downPart();
                        } else if (Openables.isSingleOpenable(block.getType())) {
                            openableBlock = block;
                        }

                        if (openableBlock != null) {
                            Long closetime = SignTimer.getTimer(lockSign);

                            if (closetime != null) {
                                Set<Block> openables = new HashSet<>();
                                openables.add(openableBlock);

                                if (openableBlock.getType() == Material.IRON_DOOR || openableBlock.getType() == Material.IRON_TRAPDOOR) {
                                    Openables.toggleOpenable(openableBlock);
                                }
                                for (BlockFace blockface : PadlockAPI.cardinalFaces) {
                                    Block relative = openableBlock.getRelative(blockface);
                                    if (relative.getType() == openableBlock.getType()) {
                                        openables.add(relative);
                                        Openables.toggleOpenable(relative);
                                    }
                                }

                                if (closetime > 0) {
                                    plugin.getOpenableToggleManager().toggleCancelRunning(openables, closetime);
                                } // timer disabled
                            } // no timer
                        } // not openable
                    } // not right-click
                } else {// no permission
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
                    event.setCancelled(true);
                }
            } // not locked
        }
    }

    /**
     * Protect block from interfere block
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onAttemptPlaceInterfereBlocks(@NotNull BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!player.hasPermission(PermissionManager.ADMIN_INTERFERE.getPerm()) && PadlockAPI.isInterfering(block, player)) {
            // no permission
            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_INTERFERE);
            event.setCancelled(true);
        }
    }

    /**
     * Tell player about padlock
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlaceFirstBlockNotify(@NotNull BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.hasPermission(PermissionManager.ACTION_LOCK.getPerm())) {
            if (MiscUtils.shouldNotify(player) && plugin.getConfigManager().isLockable(block.getType())) {
                //notice me Senpai (o//_//o)
                if (Objects.requireNonNull(plugin.getConfigManager().getQuickProtectAction()) == ConfigManager.QuickProtectOption.OFF) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOTICE_MANUEL_LOCK);
                } else {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOTICE_QUICK_LOCK);
                }
            } // already notified
        } // no permission
    }

    /**
     * prevent water logging
     */
    @EventHandler(priority = EventPriority.HIGH)
    private void onBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        if (!(PadlockAPI.isOwner(block, player) ||
                player.hasPermission(PermissionManager.ADMIN_USE.getPerm()))) {

            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
            event.setCancelled(true);

            // resync the bucket
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isDead()) {
                    player.updateInventory();
                }
            }, 1L);
        } // has permission
    }

    /**
     * prevent water unlogging
     */
    @EventHandler(priority = EventPriority.HIGH)
    private void onBucketUse(@NotNull PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        if (!(PadlockAPI.isOwner(block, player) || player.hasPermission(PermissionManager.ADMIN_USE.getPerm()))) {
            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
            event.setCancelled(true);
        } // has permission
    }

    /**
     * prevent non owners / admins from taking books of locked lecterns
     */
    @EventHandler(priority = EventPriority.HIGH)
    private void onLecternTake(@NotNull PlayerTakeLecternBookEvent event) {
        Player player = event.getPlayer();

        if (!(PadlockAPI.isOwner(event.getLectern().getBlock(), player) ||
                player.hasPermission(PermissionManager.ADMIN_USE.getPerm()))) {

            plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
            event.setCancelled(true);
        }
    }
}
