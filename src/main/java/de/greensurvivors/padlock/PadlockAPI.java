package de.greensurvivors.padlock;

import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.openabledata.DoubleBlockParts;
import de.greensurvivors.padlock.impl.openabledata.Openables;
import de.greensurvivors.padlock.impl.signdata.*;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PadlockAPI { //todo this class needs a clean
    public final static Set<BlockFace> cardinalFaces = Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    public final static Set<BlockFace> allFaces = Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    /**
     * set a sign - legacy additional at best - invalid. the plugin will ignore it in the future.
     */
    public static void setInvalid(@NotNull Sign sign) {
        SignLock.setInvalid(sign);
    }

    /**
     * returns lock sign of a block, sets all additional signs invalid
     */
    @Deprecated(forRemoval = true)
    public static @Nullable Sign updateLegacySign(Sign signToUpdate) {
        Block attachedTo = getAttachedBlock(signToUpdate.getBlock());

        if (attachedTo != null) {
            BlockData data = attachedTo.getBlockData();

            if (data instanceof Door) { // in ancient Lockette times, only doors did have special treatment like this
                DoubleBlockParts attachedDoor = Openables.getDoubleBlockParts(attachedTo);

                if (attachedDoor != null) {
                    Sign lockSign = getNearLockDoubleBlock(attachedDoor);

                    if (lockSign != null) {
                        SignLock.updateLegacyLock(lockSign);
                        SignExpiration.updateLegacyTime(lockSign);
                        SignTimer.updateLegacyTimer(lockSign);
                        EveryoneSign.updateLegacy(lockSign);
                        SignDisplay.updateDisplay(lockSign);

                        for (Sign additional : getAdditionalSignsDoor(attachedDoor)) {
                            SignLock.updateSignFromAdditional(lockSign, additional);
                            SignExpiration.updateLegacyTimeFromAdditional(lockSign, additional);
                            SignTimer.updateLegacyTimerFromAdditional(lockSign, additional);
                            EveryoneSign.updateLegacyFromAdditional(lockSign, additional);
                        }

                        return lockSign;
                    } else {
                        Padlock.getPlugin().getLogger().warning("Couldn't find a lock sign to update, but the double block at " + attachedTo.getLocation() + " is locked.");
                    }
                } else {
                    Padlock.getPlugin().getLogger().warning("Couldn't get double block parts, but data says there should be one. Is it half? " + attachedTo.getLocation());
                }
            } else if (data instanceof Chest) {
                Sign lockSign = getLockChest(attachedTo);
                if (lockSign != null) {
                    SignLock.updateLegacyLock(lockSign);
                    SignExpiration.updateLegacyTime(lockSign);
                    SignTimer.updateLegacyTimer(lockSign);
                    EveryoneSign.updateLegacy(lockSign);
                    SignDisplay.updateDisplay(lockSign);

                    for (Sign additional : getAdditionalSignsChest(attachedTo)) {
                        SignLock.updateSignFromAdditional(lockSign, additional);
                        SignExpiration.updateLegacyTimeFromAdditional(lockSign, additional);
                        SignTimer.updateLegacyTimerFromAdditional(lockSign, additional);
                        EveryoneSign.updateLegacyFromAdditional(lockSign, additional);
                    }

                    return lockSign;
                } else {
                    Padlock.getPlugin().getLogger().warning("Couldn't find a lock sign to update, but the door at " + attachedTo.getLocation() + "is locked.");
                }
            } else {
                Sign lockSign = getLockSignSingleBlock(attachedTo, null);

                if (lockSign != null) {
                    SignLock.updateLegacyLock(lockSign);
                    SignExpiration.updateLegacyTime(lockSign);
                    SignTimer.updateLegacyTimer(lockSign);
                    EveryoneSign.updateLegacy(lockSign);
                    SignDisplay.updateDisplay(lockSign);

                    for (Sign additional : getAdditionalSignsSingleBlock(attachedTo, null)) {
                        SignLock.updateSignFromAdditional(lockSign, additional);
                        SignExpiration.updateLegacyTimeFromAdditional(lockSign, additional);
                        SignTimer.updateLegacyTimerFromAdditional(lockSign, additional);
                        EveryoneSign.updateLegacyFromAdditional(lockSign, additional);
                    }

                    return lockSign;
                } else {
                    Padlock.getPlugin().getLogger().warning("Couldn't find a lock sign to update, but the block at " + attachedTo.getLocation() + "is locked.");
                }
            }
        } else {
            setInvalid(signToUpdate);
        }

        return null;
    }

    /**
     * get the lock sign of a simple single block
     */
    public static @Nullable Sign getLockSignSingleBlock(Block block, @Nullable BlockFace exempt) {
        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = MiscUtils.getFacingSign(block, blockface);

                // Find [Private] sign?
                if (isValidLockSign(sign)) {
                    return sign;
                }
            } // exempted blockface
        } // for loop

        return null;
    }

    /**
     * get the lock sign near (above / below) or directly at any connected two high block
     */
    private static @Nullable Sign getNearLockDoubleBlock(@NotNull DoubleBlockParts doubleToCheck) {
        Map<BlockFace, DoubleBlockParts> connectedBlocks = Openables.getConnectedBiParts(doubleToCheck);

        // check in all directions
        for (BlockFace blockFace : cardinalFaces) {
            DoubleBlockParts doubleInDirection = connectedBlocks.get(blockFace);

            if (doubleInDirection == null) {
                //above
                Sign sign = MiscUtils.getFacingSign(doubleToCheck.upPart().getRelative(0, 1, 0), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //up
                sign = MiscUtils.getFacingSign(doubleToCheck.upPart(), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //down
                sign = MiscUtils.getFacingSign(doubleToCheck.downPart(), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //below
                sign = MiscUtils.getFacingSign(doubleToCheck.downPart().getRelative(0, -1, 0), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }
            } else {
                //above
                Sign sign = getLockSingleBlock(doubleInDirection.upPart().getRelative(0, 1, 0), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //up
                sign = getLockSingleBlock(doubleInDirection.upPart(), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //down
                sign = getLockSingleBlock(doubleInDirection.downPart(), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //below
                sign = getLockSingleBlock(doubleInDirection.downPart().getRelative(0, -1, 0), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }
            }

        }

        return null;
    }

    /**
     * get the lock sign near (above / below) or directly at any connected one high block
     */
    private static @Nullable Sign getNearLockSingle(@NotNull Block blockToCheck) {
        Map<BlockFace, Block> connectedBlocks = Openables.getConnectedSingle(blockToCheck);

        for (BlockFace blockFace : cardinalFaces) {
            Block blockInDirection = connectedBlocks.get(blockFace);

            if (blockInDirection == null) {
                //above
                Sign sign = MiscUtils.getFacingSign(blockToCheck.getRelative(0, 1, 0), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //self
                sign = MiscUtils.getFacingSign(blockToCheck, blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //below
                sign = MiscUtils.getFacingSign(blockToCheck.getRelative(0, -1, 0), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }
            } else {
                //above
                Sign sign = getLockSingleBlock(blockInDirection.getRelative(0, 1, 0), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //relative
                sign = getLockSingleBlock(blockInDirection, blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //below
                sign = getLockSingleBlock(blockInDirection.getRelative(0, -1, 0), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }
            }
        }

        return null;
    }

    /**
     * Get lock sign of a chest.
     * Chests need special handeling since they could be double chests
     */
    private static @Nullable Sign getLockChest(Block chestBlock) {
        if (chestBlock.getBlockData() instanceof Chest chest && chest.getType() != Chest.Type.SINGLE) {

            // Check second chest sign
            BlockFace chestface = getRelativeChestFace(chest);
            if (chestface != null) {
                Sign sign = getLockSignSingleBlock(chestBlock, chestface);

                if (sign != null) {
                    return sign;
                } else { // not found, check other half
                    return getLockSignSingleBlock(chestBlock.getRelative(chestface), chestface.getOppositeFace());
                }
            }
        }

        return getLockSignSingleBlock(chestBlock, null);
    }

    /**
     * get lock sign of a single block
     *
     * @param exempt marks the direction to NOT check (we now for sure there could nothing be)
     */
    private static @Nullable Sign getLockSingleBlock(@NotNull Block block, @Nullable BlockFace exempt) {
        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = MiscUtils.getFacingSign(block, blockface);

                // Find [Private] sign?
                if (isValidLockSign(sign)) {
                    return sign;
                }
            } // exempted blockface
        } // for loop

        return null;
    }

    /**
     * get legacy additional signs for a plain old single block.
     *
     * @param exempt marks the direction to NOT check (we now for sure there could nothing be)
     */
    @Deprecated(forRemoval = true)
    public static @NotNull List<Sign> getAdditionalSignsSingleBlock(Block block, @Nullable BlockFace exempt) {
        List<Sign> additionalSigns = new ArrayList<>();

        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = MiscUtils.getFacingSign(block, blockface);

                // Find additional sign?
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }
            } // exempted blockface
        } // for loop

        return additionalSigns;
    }

    /**
     * get legacy additional signs of doors.
     * (doors where the only block supporting additional signs in a special way)
     */
    @Deprecated(forRemoval = true)
    private static @NotNull List<Sign> getAdditionalSignsDoor(@NotNull DoubleBlockParts parts) {
        List<Sign> additionalSigns = new ArrayList<>();
        Map<BlockFace, DoubleBlockParts> connectedParts = Openables.getConnectedBiParts(parts);

        for (BlockFace blockFace : cardinalFaces) {
            DoubleBlockParts doubleBlockInDirection = connectedParts.get(blockFace);

            if (doubleBlockInDirection == null) {
                //above
                Sign sign = MiscUtils.getFacingSign(parts.upPart().getRelative(0, 1, 0), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }

                //up
                sign = MiscUtils.getFacingSign(parts.upPart(), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }

                //down
                sign = MiscUtils.getFacingSign(parts.downPart(), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }

                //below
                sign = MiscUtils.getFacingSign(parts.downPart().getRelative(0, -1, 0), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }
            } else {
                BlockFace exempt = blockFace.getOppositeFace();
                Block above = doubleBlockInDirection.upPart().getRelative(0, 1, 0);
                Block below = doubleBlockInDirection.downPart().getRelative(0, -1, 0);

                for (BlockFace blockface : cardinalFaces) {
                    if (blockface != exempt) {
                        //above
                        Sign sign = MiscUtils.getFacingSign(above, blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }

                        //up
                        sign = MiscUtils.getFacingSign(doubleBlockInDirection.upPart(), blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }

                        //down
                        sign = MiscUtils.getFacingSign(doubleBlockInDirection.downPart(), blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }

                        //below
                        sign = MiscUtils.getFacingSign(below, blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }
                    } // exempted blockface
                } // for loop
            }
        }

        return additionalSigns;
    }

    /**
     * get legacy additional signs of doors.
     */
    @Deprecated(forRemoval = true)
    private static @NotNull List<Sign> getAdditionalSignsChest(Block chestBlock) {
        if (chestBlock.getBlockData() instanceof Chest chest && chest.getType() != Chest.Type.SINGLE) {
            List<Sign> addiditionalSigns = new ArrayList<>();

            // Check second chest sign
            BlockFace chestface = getRelativeChestFace(chest);
            if (chestface != null) {
                addiditionalSigns.addAll(getAdditionalSignsSingleBlock(chestBlock, chestface));
                // check other half

                addiditionalSigns.addAll(getAdditionalSignsSingleBlock(chestBlock.getRelative(chestface), chestface.getOppositeFace()));
                return addiditionalSigns;
            }
        }

        return getAdditionalSignsSingleBlock(chestBlock, null);
    }

    /**
     * get the lock sign of a block, might be null if no where found.
     */
    public static @Nullable Sign getLock(Block block) {
        if (block.getState() instanceof Sign sign) {
            if (isValidLockSign(sign)) {
                return sign;
            } else {
                Block attatchedTo = getAttachedBlock(block);
                if (attatchedTo != null) {
                    return getLock(attatchedTo);
                } else {
                    return getLockSignSingleBlock(block, null);
                }
            }
        } else if (Openables.isSingleOpenable(block.getType())) { // stupid trapdoors being also bisected.
            return getNearLockSingle(block);
        } else if (block.getBlockData() instanceof Bisected && !Tag.STAIRS.isTagged(block.getType())) { // door, also stupid stairs
            DoubleBlockParts doubleBlockParts = Openables.getDoubleBlockParts(block);

            if (doubleBlockParts != null) {
                return getNearLockDoubleBlock(doubleBlockParts);
            } else {
                return null;
            }
        } else if (block.getBlockData() instanceof Chest) {
            return getLockChest(block);
        }
        return getLockSignSingleBlock(block, null);
    }

    /**
     * checks if the player is an owner of the locked block
     *
     * @return will also true if the block is not protected
     */
    public static boolean isOwner(@NotNull Block block, @NotNull OfflinePlayer player) {
        Sign lock = getLock(block);
        return lock != null && SignLock.isOwner(lock, player.getUniqueId());
    }

    /**
     * checks if the player is a member of the locked block.
     */
    public static boolean isMember(@NotNull Block block, @NotNull Player player) {
        Sign lock = getLock(block);
        return lock != null && SignLock.isMember(lock, player.getUniqueId());
    }

    /**
     * get if the block is locked or a lock sign itself
     */
    public static boolean isProtected(@NotNull Block block) {
        return getLock(block) != null;
    }

    /**
     * get if a block is (in)directly lockable
     */
    public static boolean isLockable(@NotNull Block block) {
        Material material = block.getType();
        if (Padlock.getPlugin().getConfigManager().isLockable(material)) { // Directly lockable
            return true;
        } else { // Indirectly lockable
            Block blockup = block.getRelative(BlockFace.UP);
            if (isUpDownAlsoLockableBlock(blockup)) return true;
            Block blockdown = block.getRelative(BlockFace.DOWN);
            return isUpDownAlsoLockableBlock(blockdown);
        }
    }

    /**
     * return true if the block above / below the lockable block is also a lockable spot
     */
    public static boolean isUpDownAlsoLockableBlock(Block block) {
        Material material = block.getType();

        return Padlock.getPlugin().getConfigManager().isLockable(material) && (Openables.isSingleOpenable(material) ||
                (!Tag.STAIRS.isTagged(material) && block.getBlockData() instanceof Bisected));
    }

    /**
     * return true, if interference is forbidden, true otherwise
     */
    public static boolean isInterfering(@NotNull Block block, @NotNull Player player) { //todo
        Sign lock = getLock(block);

        if (lock != null && !SignLock.isOwner(lock, player.getUniqueId())) {
            return true;
        } else if (block.getState() instanceof Container && Padlock.getPlugin().getConfigManager().isInterferePlacementBlocked()) { // container need additional space because of hopper / minecarts
            for (BlockFace blockface : allFaces) {
                lock = getLock(block.getRelative(blockface));
                if (lock != null && !SignLock.isOwner(lock, player.getUniqueId())) {
                    return true;
                }
            }
        }


        Padlock.getPlugin().getLogger().info("no.");
        return false;
    }

    /**
     * get if the sign is a lock sign and still not expired.
     * Please mind, a private sign may have expired,
     * but do how two locked blocks line up it's totally possible for more than one [Private] sign per block.
     * However only the first valid found wil get used
     */
    public static boolean isValidLockSign(@Nullable Sign sign) {
        if (sign != null && isLockSign(sign)) {
            // Found [Private] sign, is expiring turned on and expired? (relative block is now sign)
            return !Padlock.getPlugin().getConfigManager().doLocksExpire() || !isSignExpired(sign);
        } else {
            return false;
        }
    }

    /**
     * get if the sign is a lock sign, may be expired tho
     */
    public static boolean isLockSign(@NotNull Sign sign) {
        return SignLock.isLockSign(sign);
    }

    /**
     * Check if a sign is an additional sign
     */
    @Deprecated(forRemoval = true)
    public static boolean isAdditionalSign(@NotNull Sign sign) {
        return SignLock.isAdditionalSign(sign);
    }

    /**
     * check if a lock is expired
     */
    public static boolean isSignExpired(@NotNull Sign sign) {
        return SignExpiration.isSignExpired(sign);
    }

    public static boolean isPartOfLockedDoor(Block block) {
        Block blockup = block.getRelative(BlockFace.UP);
        if (isUpDownAlsoLockableBlock(blockup) && isProtected(blockup)) return true;
        Block blockdown = block.getRelative(BlockFace.DOWN);
        return isUpDownAlsoLockableBlock(blockdown) && isProtected(blockdown);
    }

    /**
     * get the block a sign is attached to or null if not possible
     */
    public static @Nullable Block getAttachedBlock(@NotNull Block signBlock) {
        if (signBlock.getBlockData() instanceof Directional directional) {
            return signBlock.getRelative(directional.getFacing().getOppositeFace());
        } else {
            return null;
        }
    }

    /**
     * get the face where the second half of a double chest is
     */
    public static @Nullable BlockFace getRelativeChestFace(@NotNull Chest chest) {
        BlockFace face = chest.getFacing();
        BlockFace relativeFace = null;
        if (chest.getType() == Chest.Type.LEFT) {
            if (face == BlockFace.NORTH) {
                relativeFace = BlockFace.EAST;
            } else if (face == BlockFace.SOUTH) {
                relativeFace = BlockFace.WEST;
            } else if (face == BlockFace.WEST) {
                relativeFace = BlockFace.NORTH;
            } else if (face == BlockFace.EAST) {
                relativeFace = BlockFace.SOUTH;
            }
        } else if (chest.getType() == Chest.Type.RIGHT) {
            if (face == BlockFace.NORTH) {
                relativeFace = BlockFace.WEST;
            } else if (face == BlockFace.SOUTH) {
                relativeFace = BlockFace.EAST;
            } else if (face == BlockFace.WEST) {
                relativeFace = BlockFace.SOUTH;
            } else if (face == BlockFace.EAST) {
                relativeFace = BlockFace.NORTH;
            }
        }
        return relativeFace;
    }
}