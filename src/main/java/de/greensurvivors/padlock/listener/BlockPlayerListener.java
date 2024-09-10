package de.greensurvivors.padlock.listener;

import com.sk89q.worldguard.bukkit.event.block.UseBlockEvent;
import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.ConfigManager;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.openabledata.Openables;
import de.greensurvivors.padlock.impl.signdata.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
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
    private final static Set<Material> replaceableMaterials = Set.of(Material.AIR, Material.CAVE_AIR, Material.LIGHT,
            Material.WATER, Material.LAVA, Material.VINE, Material.GLOW_LICHEN);

    public BlockPlayerListener(Padlock plugin) {
        this.plugin = plugin;
    }

    /**
     * checks for vanilla spawn protection
     *
     * @param player
     * @param location
     * @return
     */
    private static boolean isSpawnProtected(@NotNull Player player, @NotNull Location location) {
        int spawnSize = Bukkit.getServer().getSpawnRadius();

        if (Bukkit.getServer().getWorlds().get(0).getEnvironment() == World.Environment.NORMAL &&
                spawnSize > 0 && !player.isOp()) {
            Location spawnLocation = player.getWorld().getSpawnLocation();
            return Math.abs(location.x() - spawnLocation.z()) > spawnSize ||
                    Math.abs(location.z() - spawnLocation.z()) > spawnSize;
        }
        return true;
    }

    /**
     * Quick aka automatic protect
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onQuickLock(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check quick lock status
        switch (plugin.getConfigManager().getQuickProtectAction()) {
            case NO_QUICKLOCK -> {
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
            if (action == Action.RIGHT_CLICK_BLOCK && Tag.SIGNS.isTagged(signType)) { // note: this does NOT include hanging signs by intention
                // Check permission
                if (player.hasPermission(PermissionManager.ACTION_LOCK.getPerm())) {
                    // Get target block to lock
                    BlockFace blockface = event.getBlockFace();
                    if (PadlockAPI.cardinalFaces.contains(blockface)) {
                        Block block = event.getClickedBlock();
                        if (block != null) {
                            Block signLocBlock = block.getRelative(blockface);

                            // Check permission with external plugin and
                            // whether locking location is obstructed and
                            // whether this block is lockable
                            // Note: we check for break, not interact here, since we want to ensure the player can break the lock!
                            if (!plugin.getDependencyManager().isProtectedFromBreak(block, player) &&
                                    replaceableMaterials.contains(signLocBlock.getType()) &&
                                    PadlockAPI.isLockable(block)) {
                                // Cancel event here
                                event.setCancelled(true);
                                // Is this block already locked?
                                if (!PadlockAPI.isProtected(block)) {
                                    // Not locked, not a locked door nearby

                                    //copy original state
                                    BlockState replacedState = signLocBlock.getState(true);
                                    // Put sign on
                                    SignLock.putPrivateSignOn(signLocBlock, blockface, signType, player); // no copy!

                                    // because of this BlockPlaceEvent we have to first change the block and if it was canceled reset later, instead
                                    // of setting the state after all checks
                                    boolean canBuild = isSpawnProtected(player, signLocBlock.getLocation());
                                    BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(signLocBlock, replacedState,
                                            block, player.getInventory().getItemInMainHand(), player, canBuild, event.getHand());

                                    Bukkit.getPluginManager().callEvent(blockPlaceEvent);
                                    if (!blockPlaceEvent.isCancelled() && blockPlaceEvent.canBuild()) {
                                        // all good. update inventory, apply physics, invalidate cache and send message
                                        MiscUtils.removeAItemMainHand(player);

                                        // I know this does set the data a second time. But I don't now any way to just apply physics without reflection
                                        signLocBlock.getState().update(true, true);

                                        plugin.getLockCacheManager().removeFromCache(block.getLocation());
                                        // Send message
                                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_SUCCESS);
                                    } else {
                                        // reset
                                        replacedState.update(true, false);
                                    }
                                } else {
                                    // Cannot lock this block
                                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.QUICK_LOCK_ERROR);
                                }
                            } // not lockable
                        } // clicked block is null
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

        SignAccessType.AccessType accessType = SignAccessType.getAccessTypeFromComp(topline);
        if (accessType != null) {
            //check attached block
            Block attachedBlock = PadlockAPI.getAttachedBlock(event.getBlock());
            if (attachedBlock != null && PadlockAPI.isLockable(attachedBlock)) {
                if (PadlockAPI.isProtected(attachedBlock)) {
                    if (PadlockAPI.isOwner(attachedBlock, player.getUniqueId())) {
                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_ALREADY_LOCKED);
                        event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_ERROR));
                    } else {
                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOT_OWNER);
                        event.setCancelled(true);
                    }
                } else if (player.hasPermission(PermissionManager.ACTION_LOCK.getPerm())) { // all good
                    Sign sign = (Sign) event.getBlock().getState();
                    SignAccessType.setAccessType(sign, accessType, false);
                    sign.setWaxed(true);
                    sign.update();

                    // Player with this permission can lock with another name
                    if (!player.hasPermission(PermissionManager.ACTION_LOCK_OTHERS.getPerm())) {
                        // set the player self as owner
                        event.line(1, Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_PLAYER_NAME_ON,
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
                                event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_ERROR));
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

                    // auto connect doors to stay in line with lockette
                    if (Openables.isUpDownDoor(attachedBlock)) {
                        SignConnectedOpenable.setConnected(sign, true);
                    }

                    //update Sign after event was done
                    Bukkit.getScheduler().runTaskLater(plugin, () -> SignDisplay.updateDisplay(sign), 2);
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_SUCCESS);

                    plugin.getLockCacheManager().removeFromCache(attachedBlock.getLocation());
                } else {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NO_PERMISSION);
                    event.setCancelled(true);
                }
            } else {
                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.LOCK_ERROR_NOT_LOCKABLE);
                event.line(0, plugin.getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_ERROR));
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
                    (PadlockAPI.isLockSign(sign) || PadlockAPI.isAdditionalSign(sign))) {

                SignSelection.selectSign(player, clickedBlock);
                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.SELECT_SIGN);

                // cancel block place when selecting a sign
                if (event.hasItem()) {
                    event.setCancelled(true);
                }
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
                    plugin.getLockCacheManager().removeFromCache(sign);
                } else { // not allowed to break
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOT_OWNER);
                    event.setCancelled(true);
                }
            } else if (PadlockAPI.isAdditionalSign(sign)) {
                if (PadlockAPI.isOwner(block, player.getUniqueId()) || player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm())) {
                    final Sign lockSign = PadlockAPI.getLock(PadlockAPI.getAttachedBlock(block), false);

                    //let the additional sign break but update the others
                    if (lockSign != null) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> PadlockAPI.updateLegacySign(lockSign), 2);
                    }
                } else { // not allowed to break
                    PadlockAPI.updateLegacySign(sign);

                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.NOT_OWNER);
                    event.setCancelled(true);
                }
            }
        } else { // not a sign
            Sign lock = PadlockAPI.getLock(block, false);
            if (lock != null) {
                if (!(SignLock.isOwner(lock, player.getUniqueId()) || player.hasPermission(PermissionManager.ADMIN_BREAK.getPerm()))) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
                    event.setCancelled(true);
                } else if (!plugin.getDependencyManager().isProtectedFromBreak(block, player)) { // if the player is allowed to break the block. Note: we check for break, not interact here, since we want to ensure the player can break the lock!
                    // only break sign, if the broken block was the last protected block
                    Block attachedBlock = PadlockAPI.getAttachedBlock(event.getBlock());
                    if (attachedBlock != null && !PadlockAPI.isLockable(attachedBlock)) {
                        // break lock sign in case it wasn't attached to the block and isn't left over.
                        // wouldn't be a big deal, but just in case there was block type specific settings,
                        // we save us to deal with mixed up settings in the future
                        lock.getBlock().breakNaturally();
                    }
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
            PadlockAPI.updateLegacySign(sign);
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

    // worldguard internal event, NOT part of API.
    // worldguard fires this event, so it goes
    // original event start --> worldguard handling the event and then fires the UseBlockEvent -->
    // we listen as early as possible to the UseBlockEvent and allowing the original event to pass -->
    // no worldguard listener should cancel the original event -->
    // original event gets handled by every plugin between -->
    // if not canceled we finally handle the original event ourselves below
    // of course this does not just include the PlayerInteractEvent,
    // but also InventoryOpenEvent, BlockDamageEvent (for cakes), EntityInteractEvent, PlayerBedEnterEvent,
    // PlayerTakeLecternBookEvent, CauldronLevelChangeEvent, BlockDispenseEvent (for some reason) and PlayerOpenSignEvent
    // (worldguard version 7.0.9, 2024)
    // we only allow the use of blocks when we do handle the original event,
    // but it might be a good inspiration to support more block types
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onWorldguardUseBlockEvent(final @NotNull UseBlockEvent event) {
        if (event.getOriginalEvent() instanceof PlayerInteractEvent ||
            event.getOriginalEvent() instanceof InventoryOpenEvent ||
            event.getOriginalEvent() instanceof PlayerTakeLecternBookEvent) { // only care for events we handle
            for (final @NotNull Block block : event.getBlocks()) { // this means WE have to check every interacted block at least twice! I recommend to turn on caching if using in combination with world guard
                Sign lockSign = PadlockAPI.getLock(block, false);

                if (lockSign != null) {
                    // allow the interaction of this block
                    event.setAllowed(true);
                }
            }
        }
    }

    /**
     * Protect block from being used
     * & handle connected openables
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    // ensure with priority to fire after worldguard
    // go as last as possible to ensure wolrdguard is done
    private void onAttemptInteractLockedBlocks(@NotNull PlayerInteractEvent event) {
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
            Sign lockSign = PadlockAPI.getLock(block, false);

            if (lockSign != null) {
                Player player = event.getPlayer();

                if (SignSelection.getSelectedSign(player) != lockSign) {
                    if (SignLock.isMember(lockSign, player.getUniqueId()) || SignLock.isOwner(lockSign, player.getUniqueId()) ||
                            player.hasPermission(PermissionManager.ADMIN_USE.getPerm()) ||
                            SignAccessType.getAccessType(lockSign, false) != SignAccessType.AccessType.PRIVATE) {

                        // Handle multi openables
                        if (action == Action.RIGHT_CLICK_BLOCK) {

                            // get openable block
                            Block openableBlock = null;
                            if (Tag.DOORS.isTagged(block.getType())) {
                                openableBlock = Openables.getDoubleBlockParts(block).downPart();
                            } else if (Openables.isSingleOpenable(block.getType())) {
                                openableBlock = block;
                            }

                            if (openableBlock != null &&
                                    // if a place attempt was prevented, this could register as a false positive and flip every block but the clicked one
                                    // therefor everything comes out of sync.
                                    !(player.isSneaking() && event.hasItem()) &&
                                    // event gets fired for both hands, ignore offhand
                                    event.getHand() == EquipmentSlot.HAND) {

                                Set<Block> openables = new HashSet<>();
                                openables.add(openableBlock);

                                if (openableBlock.getType() == Material.IRON_DOOR || openableBlock.getType() == Material.IRON_TRAPDOOR) {
                                    Openables.toggleOpenable(openableBlock);

                                    // stop blocks from getting placed when opening a door.
                                    if (event.hasItem()) {
                                        event.setCancelled(true);
                                    }
                                }

                                if (SignConnectedOpenable.isConnected(lockSign)) {
                                    for (BlockFace blockface : PadlockAPI.cardinalFaces) {
                                        Block relative = openableBlock.getRelative(blockface);
                                        if (relative.getType() == openableBlock.getType()) {
                                            openables.add(relative);
                                            Openables.toggleOpenable(relative);
                                        } //not the same type of block
                                    } // for loop
                                } // not connected

                                Long closetime = SignTimer.getTimer(lockSign, false);
                                if (closetime != null && closetime > 0) {
                                    plugin.getOpenableToggleManager().toggleCancelRunning(openables, closetime);
                                } // timer disabled
                            } // not openable
                        } // not right-click
                    } else {// no permission
                        plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
                        event.setCancelled(true);
                    }
                } // sign was selected. ignoring
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

        if (!player.hasPermission(PermissionManager.ADMIN_INTERFERE.getPerm()) && PadlockAPI.isInterfering(block, player.getUniqueId())) {
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
                if (Objects.requireNonNull(plugin.getConfigManager().getQuickProtectAction()) == ConfigManager.QuickProtectOption.NO_QUICKLOCK) {
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

        if (PadlockAPI.isProtected(block) &&
            !(PadlockAPI.isOwner(block, player.getUniqueId()) || player.hasPermission(PermissionManager.ADMIN_USE.getPerm()))) {

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

        if (!(PadlockAPI.isOwner(block, player.getUniqueId()) || player.hasPermission(PermissionManager.ADMIN_USE.getPerm()))) {
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

        Sign lock = PadlockAPI.getLock(event.getLectern().getBlock(), false);

        if (lock != null) {
            if (!(SignLock.isOwner(lock, player.getUniqueId()) || SignLock.isMember(lock, player.getUniqueId()) ||
                    SignPasswords.hasStillAccess(player.getUniqueId(), lock.getLocation()) ||
                    player.hasPermission(PermissionManager.ADMIN_USE.getPerm()))) {
                SignAccessType.AccessType accessType = SignAccessType.getAccessType(lock, false);

                if (!(accessType == SignAccessType.AccessType.SUPPLY || accessType == SignAccessType.AccessType.PUBLIC)) {
                    plugin.getMessageManager().sendLang(player, MessageManager.LangPath.ACTION_PREVENTED_LOCKED);
                    event.setCancelled(true);
                }
            }
        }
    }

    private void onPlaceItem(@NotNull InventoryInteractEvent event, @NotNull Inventory topInventory) {
        Block block = null;
        if (topInventory.getHolder() instanceof BlockInventoryHolder blockInventoryHolder) {
            block = blockInventoryHolder.getBlock();
        } else if (topInventory.getHolder() instanceof DoubleChest doubleChest) {
            if (doubleChest.getLeftSide() instanceof BlockInventoryHolder blockInventoryHolder) {
                block = blockInventoryHolder.getBlock();
            }
        }

        if (block != null) {
            Sign lock = PadlockAPI.getLock(block, false);

            if (lock != null) {
                // if a player should have access there is no need to cancel the event
                if (event.getWhoClicked() instanceof Player player) {
                    if (SignLock.isMember(lock, player.getUniqueId()) || SignLock.isOwner(lock, player.getUniqueId()) ||
                            SignPasswords.hasStillAccess(player.getUniqueId(), lock.getLocation()) ||
                            player.hasPermission(PermissionManager.ADMIN_USE.getPerm())) {
                        return;
                    }
                }

                SignAccessType.AccessType type = SignAccessType.getAccessType(lock, false);

                switch (type) {
                    case PRIVATE, PUBLIC, DONATION -> {
                    }
                    case DISPLAY, SUPPLY -> {
                        event.setResult(Event.Result.DENY);
                    }
                    /*case null, // todo next java update */
                    default -> {
                    }
                }
            }
        }

    }

    private void onTakeItem(@NotNull InventoryInteractEvent event, @NotNull Inventory topInventory) {
        Block block = null;
        if (topInventory.getHolder() instanceof BlockInventoryHolder blockInventoryHolder) {
            block = blockInventoryHolder.getBlock();
        } else if (topInventory.getHolder() instanceof DoubleChest doubleChest) {
            block = doubleChest.getLocation().getBlock();
        }

        if (block != null) {
            Sign lock = PadlockAPI.getLock(block, false);

            if (lock != null) {
                // if a player should have access there is no need to cancel the event
                if (event.getWhoClicked() instanceof Player player) {
                    if (SignLock.isMember(lock, player.getUniqueId()) || SignLock.isOwner(lock, player.getUniqueId()) ||
                            SignPasswords.hasStillAccess(player.getUniqueId(), lock.getLocation()) ||
                            player.hasPermission(PermissionManager.ADMIN_USE.getPerm())) {
                        return;
                    }
                }

                SignAccessType.AccessType type = SignAccessType.getAccessType(lock, false);

                switch (type) {
                    case PRIVATE, PUBLIC, SUPPLY -> {
                    }
                    case DISPLAY, DONATION -> {
                        event.setResult(Event.Result.DENY);
                    }
                    /*case null, // todo next java update */
                    default -> {
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onInventoryClick(@NotNull InventoryClickEvent event) { // todo warn people when opening what they are opening best with cache to not annoy
        Inventory topInv = event.getView().getTopInventory();

        switch (event.getAction()) {
            case NOTHING, DROP_ALL_CURSOR, DROP_ONE_CURSOR, UNKNOWN -> {
            } // nothing
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, DROP_ALL_SLOT, DROP_ONE_SLOT, HOTBAR_MOVE_AND_READD -> { // may take
                if (event.getClickedInventory() == topInv) {
                    onTakeItem(event, topInv);
                }
            }
            case PLACE_ALL, PLACE_SOME, PLACE_ONE -> { // may place
                if (event.getClickedInventory() == topInv) {
                    onPlaceItem(event, topInv);
                }
            }
            case SWAP_WITH_CURSOR, HOTBAR_SWAP -> { // may give and take
                if (event.getClickedInventory() == topInv) {
                    onPlaceItem(event, topInv);
                    onTakeItem(event, topInv);
                }
            }
            case COLLECT_TO_CURSOR -> { // may take complex
                if (topInv.contains(event.getCursor().getType())) {
                    onTakeItem(event, topInv);
                }
            }
            case MOVE_TO_OTHER_INVENTORY -> { // definitely one or the other
                if (event.getClickedInventory() == topInv) {
                    onTakeItem(event, topInv);
                } else {
                    onPlaceItem(event, topInv);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onInventoryDrag(@NotNull InventoryDragEvent event) {
        for (int rawSlotId : event.getRawSlots()) {
            if (rawSlotId < event.getView().getTopInventory().getSize()) {
                onPlaceItem(event, event.getView().getTopInventory());
            }
        }
    }
}
