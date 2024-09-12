package de.greensurvivors.padlock;

import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.dataTypes.DoubleBlockParts;
import de.greensurvivors.padlock.impl.dataTypes.LazySignProperties;
import de.greensurvivors.padlock.impl.openabledata.Openables;
import de.greensurvivors.padlock.impl.signdata.SignExpiration;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class PadlockAPI {
    public final static Set<BlockFace> cardinalFaces = Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    public final static Set<BlockFace> allFaces = Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    /**
     * set a sign - legacy additional at best - invalid. the plugin will ignore it in the future.
     */
    public static void setInvalid(@NotNull Sign sign) {
        SignLock.setInvalid(sign);
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
    private static @Nullable Sign getLockChest(@NotNull Block chestBlock) {
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
     * get the not expired lock sign of a block, might be null if no where found.
     */
    public static @Nullable Sign getLock(@NotNull Block block, boolean ignoreCache) {
        if (!ignoreCache && Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            return Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(block.getLocation()).getLock();
        } else {
            if (block.getState() instanceof Sign sign) {
                if (isValidLockSign(sign)) {
                    return sign;
                } else {
                    Block attatchedTo = getAttachedBlock(block);
                    if (attatchedTo != null) {
                        return getLock(attatchedTo, ignoreCache);
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
            } else { // it may be a block over / under door
                Block blockup = block.getRelative(BlockFace.UP);
                if (blockup.getBlockData() instanceof Bisected && !Tag.STAIRS.isTagged(blockup.getType())) { // door, also stupid stairs
                    DoubleBlockParts doubleBlockParts = Openables.getDoubleBlockParts(blockup);

                    if (doubleBlockParts != null) {
                        return getNearLockDoubleBlock(doubleBlockParts);
                    } else {
                        return null;
                    }
                }

                Block blockdown = block.getRelative(BlockFace.DOWN);
                if (blockdown.getBlockData() instanceof Bisected && !Tag.STAIRS.isTagged(blockdown.getType())) { // door, also stupid stairs
                    DoubleBlockParts doubleBlockParts = Openables.getDoubleBlockParts(blockdown);

                    if (doubleBlockParts != null) {
                        return getNearLockDoubleBlock(doubleBlockParts);
                    } else {
                        return null;
                    }
                }
            }
            return getLockSignSingleBlock(block, null);
        }
    }

    /**
     * checks if the player is an owner of the locked block
     *
     * @return will also true if the block is not protected
     */
    public static boolean isOwner(@NotNull Block block, @NotNull UUID playerUUid) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            Set<String> ownerUUIDStrs = Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(block.getLocation()).getOwnerUUIDStrs();
            String playerUUIDStr = playerUUid.toString();

            return ownerUUIDStrs != null && ownerUUIDStrs.contains(playerUUIDStr);
        } else {
            Sign lock = getLock(block, true);
            return lock != null && SignLock.isOwner(lock, playerUUid);
        }
    }

    /**
     * checks if the player is a member of the locked block.
     */
    public static boolean isMember(@NotNull Block block, @NotNull UUID playerUUid) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            LazySignProperties lazySignPropertys = Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(block.getLocation());

            if (lazySignPropertys.isLock()) {
                return SignLock.isMember(lazySignPropertys.getLock(), playerUUid);
            } else {
                return false;
            }
        }

        Sign lock = getLock(block, true);
        return lock != null && SignLock.isMember(lock, playerUUid);
    }

    /**
     * get if the block is locked or a lock sign itself
     */
    public static boolean isProtected(@NotNull Block block) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            return Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(block.getLocation()).isLock();
        } else {
            return getLock(block, true) != null;
        }
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
            if (isUpDownAlsoLockableBlock(blockup)) {
                return true;
            }
            Block blockdown = block.getRelative(BlockFace.DOWN);
            return isUpDownAlsoLockableBlock(blockdown);
        }
    }

    /**
     * return true if the block above / below the lockable block is also a lockable spot
     */
    public static boolean isUpDownAlsoLockableBlock(@NotNull Block block) {
        Material material = block.getType();

        return Padlock.getPlugin().getConfigManager().isLockable(material) && (Openables.isSingleOpenable(material) ||
            (!Tag.STAIRS.isTagged(material) && block.getBlockData() instanceof Bisected));
    }

    /**
     * return true, if interference is forbidden, true otherwise
     */
    public static boolean isInterfering(@NotNull Block block, @NotNull UUID playerUUid) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            return Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(block.getLocation()).isLock();
        }

        Sign lock = getLock(block, true);

        if (lock != null && !SignLock.isOwner(lock, playerUUid)) {
            return true;
        } else if (block.getState() instanceof Container && Padlock.getPlugin().getConfigManager().isInterferePlacementBlocked()) { // container need additional space because of hopper / minecarts
            for (BlockFace blockface : allFaces) {
                lock = getLock(block.getRelative(blockface), false);
                if (lock != null && !SignLock.isOwner(lock, playerUUid)) {
                    return true;
                }
            }
        }

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
     * check if a lock is expired
     */
    public static boolean isSignExpired(@NotNull Sign sign) {
        return SignExpiration.isSignExpired(sign);
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
    private static @Nullable BlockFace getRelativeChestFace(@NotNull Chest chest) {
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