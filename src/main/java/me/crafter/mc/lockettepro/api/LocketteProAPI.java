package me.crafter.mc.lockettepro.api;

import me.crafter.mc.lockettepro.LockettePro;
import me.crafter.mc.lockettepro.config.Config;
import me.crafter.mc.lockettepro.impl.Doors;
import me.crafter.mc.lockettepro.impl.ExpireSign;
import me.crafter.mc.lockettepro.impl.LockSign;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//todo whole expires thing
//todo update lang
public class LocketteProAPI {
    public final static BlockFace[] cardinalFaces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    public final static BlockFace[] allFaces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    /**
     * returns lock sign of a block, sets all additional signs invalid
     *
     * @param signToUpdate
     * @return
     */
    public static @Nullable Sign updateLegacySign(Sign signToUpdate) {
        Block attachedTo = getAttachedBlock(signToUpdate.getBlock());

        if (attachedTo != null) {
            BlockData data = attachedTo.getBlockData();

            if (data instanceof Door) {
                DoorParts attachedDoor = Doors.getDoorParts(attachedTo);

                if (attachedDoor != null) {
                    Sign lockSign = getLockSignDoor(attachedDoor);

                    if (lockSign != null) {
                        for (Sign additional : getAdditionalSignsDoor(attachedDoor)) {
                            LockSign.updateSignFromAdditional(lockSign, additional);
                        }

                        LockSign.updateLegacyUUIDs(lockSign);
                        return lockSign;
                    } else {
                        LockettePro.getPlugin().getLogger().warning("Couldn't find a Lock sign to update, but the door is locked.");
                    }
                } else {
                    LockettePro.getPlugin().getLogger().warning("Couldn't get door parts, but data says there should be one. Is it half? " + attachedTo.getLocation().toString());
                }
            } else if (data instanceof Chest) {
                Sign lockSign = getLockSignChest(attachedTo);
                if (lockSign != null) {
                    for (Sign additional : getAdditionalSignsChest(attachedTo)) {
                        LockSign.updateSignFromAdditional(lockSign, additional);
                    }

                    LockSign.updateLegacyUUIDs(lockSign);
                    return lockSign;
                } else {
                    //todo logging
                }
            } else {
                Sign lockSign = getLockSignSingleBlock(attachedTo, null);

                if (lockSign != null) {
                    for (Sign additional : getAdditionalSignsSingleBlock(attachedTo, null)) {
                        LockSign.updateSignFromAdditional(lockSign, additional);
                    }

                    LockSign.updateNamesByUuid(lockSign);
                    return lockSign;
                } else {
                    //todo logging
                }
            }
        } else {
            setInvalid(signToUpdate);
        }

        return null;
    }

    public static void setInvalid(@NotNull Sign sign) {
        LockSign.setInvalid(sign);
    }

    private static @Nullable Sign getLockSignDoor(@NotNull DoorParts doorToCheck) { //todo check for adittional signs and if found update; also do everything async if possible
        Map<BlockFace, DoorParts> connectedDoors = Doors.getConnectedDoors(doorToCheck);

        for (BlockFace blockFace : cardinalFaces) {
            DoorParts doorInDirection = connectedDoors.get(blockFace);

            if (doorInDirection == null) {
                //above
                Sign sign = getFacingSign(doorToCheck.upPart().getRelative(0, 1, 0), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //up
                sign = getFacingSign(doorToCheck.upPart(), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //down
                sign = getFacingSign(doorToCheck.downPart(), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }

                //below
                sign = getFacingSign(doorToCheck.downPart().getRelative(0, -1, 0), blockFace);
                if (isValidLockSign(sign)) {
                    return sign;
                }
            } else {
                //above
                Sign sign = getLockedSingleBlock(doorInDirection.upPart().getRelative(0, 1, 0), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //up
                sign = getLockedSingleBlock(doorInDirection.upPart(), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //down
                sign = getLockedSingleBlock(doorInDirection.downPart(), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }

                //below
                sign = getLockedSingleBlock(doorInDirection.downPart().getRelative(0, -1, 0), blockFace.getOppositeFace());
                if (sign != null) {
                    return sign;
                }
            }

        }

        return null;
    }

    private static @Nullable Sign getLockSignChest(Block chestBlock) {
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

    @Deprecated(forRemoval = true)
    private static @NotNull List<Sign> getAdditionalSignsDoor(@NotNull DoorParts doorToCheck) { //todo
        List<Sign> additionalSigns = new ArrayList<>();
        Map<BlockFace, DoorParts> connectedDoors = Doors.getConnectedDoors(doorToCheck);

        for (BlockFace blockFace : cardinalFaces) {
            DoorParts doorInDirection = connectedDoors.get(blockFace);

            if (doorInDirection == null) {
                //above
                Sign sign = getFacingSign(doorToCheck.upPart().getRelative(0, 1, 0), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }

                //up
                sign = getFacingSign(doorToCheck.upPart(), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }

                //down
                sign = getFacingSign(doorToCheck.downPart(), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }

                //below
                sign = getFacingSign(doorToCheck.downPart().getRelative(0, -1, 0), blockFace);
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }
            } else {
                BlockFace exempt = blockFace.getOppositeFace();
                Block above = doorInDirection.upPart().getRelative(0, 1, 0);
                Block below = doorInDirection.downPart().getRelative(0, -1, 0);

                for (BlockFace blockface : cardinalFaces) {
                    if (blockface != exempt) {
                        //above
                        Sign sign = getFacingSign(above, blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }

                        //up
                        sign = getFacingSign(doorInDirection.upPart(), blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }

                        //down
                        sign = getFacingSign(doorInDirection.downPart(), blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }

                        //below
                        sign = getFacingSign(below, blockface);

                        if (sign != null && isAdditionalSign(sign)) {
                            additionalSigns.add(sign);
                        }
                    } // exempted blockface
                } // for loop
            }
        }

        return additionalSigns;
    }

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

    public static @Nullable Sign getLockSign(Block block) {
        if (block.getBlockData() instanceof Door) {
            DoorParts door = Doors.getDoorParts(block);

            if (door != null) {
                return getLockSignDoor(door);
            } else {
                return null;
            }
        } else if (block.getBlockData() instanceof Chest) {
            return getLockSignChest(block);
        }
        return getLockSignSingleBlock(block, null);
    }

    public static boolean isLocked(@NotNull Block block) {
        if (block.getBlockData() instanceof Door) {
            DoorParts door = Doors.getDoorParts(block);

            if (door != null) {
                return getLockSignDoor(door) != null;
            } else {
                return false;
            }
        } else if (block.getBlockData() instanceof Chest) {
            return getLockSignChest(block) != null;
        }
        return isLockedSingleBlock(block, null);
    }

    public static boolean isOwner(@NotNull Block block, @NotNull OfflinePlayer player) {
        if (block.getBlockData() instanceof Door) {
            DoorParts door = Doors.getDoorParts(block);
            if (door != null) {
                Sign sign = getLockSignDoor(door);

                if (sign != null) {
                    return LockSign.isOwner(sign, player.getUniqueId());
                }
            } else {
                return false;
            }

        } else if (block.getBlockData() instanceof Chest) {
            Sign sign = getLockSignChest(block);

            if (sign != null) {
                return LockSign.isOwner(sign, player.getUniqueId());
            }
        }

        return isOwnerSingleBlock(block, null, player);
    }

    public static boolean isMember(@NotNull Block block, @NotNull Player player) {
        if (block.getType().equals(Material.LECTERN)) return true; //Lecterns can be used, but not stolen from
        if (block.getBlockData() instanceof Door) {
            DoorParts door = Doors.getDoorParts(block);

            if (door != null) {
                Sign sign = getLockSignDoor(door);

                if (sign != null) {
                    return LockSign.isMember(sign, player.getUniqueId());
                }
            } else {
                return false;
            }

        } else if (block.getBlockData() instanceof Chest) {
            Sign sign = getLockSignChest(block);

            if (sign != null) {
                return LockSign.isMember(sign, player.getUniqueId());
            }
        }

        return isUserSingleBlock(block, null, player);
    }

    public static boolean isProtected(@NotNull Block block) {
        return ((block.getState() instanceof Sign sign && isLockSign(sign)) || isLocked(block) || isUpDownOfLockedDoor(block));
    }

    private static @Nullable Sign getFacingSign(@NotNull Block block, @NotNull BlockFace blockface) {
        Block relativeblock = block.getRelative(blockface);

        if (relativeblock.getState() instanceof Sign sign && getFacing(block) == blockface) {
            return sign;
        } else {
            return null;
        }
    }

    private static @Nullable Sign getLockedSingleBlock(@NotNull Block block, @Nullable BlockFace exempt) {
        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = getFacingSign(block, blockface);

                // Find [Private] sign?
                if (isValidLockSign(sign)) {
                    return sign;
                }
            } // exempted blockface
        } // for loop

        return null;
    }

    public static boolean isLockedSingleBlock(@NotNull Block block, @Nullable BlockFace exempt) {
        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = getFacingSign(block, blockface);

                // Find [Private] sign?
                if (isValidLockSign(sign)) {
                    return true;
                }
            } // exempted blockface
        } // for loop

        return false;
    }

    private static @Nullable BlockFace getFacing(@NotNull Block block) {
        if (block.getBlockData() instanceof Directional directional) {
            return directional.getFacing();
        } else {
            return null;
        }
    }

    public static @Nullable Sign getLockSignSingleBlock(Block block, @Nullable BlockFace exempt) {
        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = getFacingSign(block, blockface);

                // Find [Private] sign?
                if (sign != null && isLockSign(sign)) {
                    return sign;
                }
            } // exempted blockface
        } // for loop

        return null;
    }

    @Deprecated(forRemoval = true)
    public static @NotNull List<Sign> getAdditionalSignsSingleBlock(Block block, @Nullable BlockFace exempt) {
        List<Sign> additionalSigns = new ArrayList<>();

        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = getFacingSign(block, blockface);

                // Find additional sign?
                if (sign != null && isAdditionalSign(sign)) {
                    additionalSigns.add(sign);
                }
            } // exempted blockface
        } // for loop

        return additionalSigns;
    }

    public static boolean isOwnerSingleBlock(@NotNull Block block, @Nullable BlockFace exempt, @NotNull OfflinePlayer player) {
        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = getFacingSign(block, blockface);

                // Find [Private] sign?
                if (isValidLockSign(sign)) {
                    return LockSign.isOwner(sign, player.getUniqueId());
                }
            } // exempted blockface
        } // for loop

        return false;
    }

    public static boolean isUserSingleBlock(Block block, BlockFace exempt, Player player) { // Requires isLocked
        for (BlockFace blockface : cardinalFaces) {
            if (blockface != exempt) {
                Sign sign = getFacingSign(block, blockface);

                // Find [Private] sign?
                if (isValidLockSign(sign)) {
                    return LockSign.isMember(sign, player.getUniqueId());
                }
            } // exempted blockface
        } // for loop
        return false;
    }

    @Deprecated
    public static boolean isOwnerOfSign(@NotNull Sign sign, @NotNull OfflinePlayer player) { //todo
        Block protectedblock = getAttachedBlock(sign.getBlock());
        // Normal situation, that block is just locked by an adjacent sign
        if (protectedblock != null && isOwner(protectedblock, player)) return true;
        // Situation where double door's block
        return protectedblock != null && isUpDownOfLockedDoor(protectedblock) && isOwnerUpDownLockedDoor(protectedblock, player);
    }

    public static boolean isLockable(Block block) {
        Material material = block.getType();
        //Bad blocks
        if (Tag.SIGNS.isTagged(material)) {
            return false;
        }
        if (Config.isLockable(material)) { // Directly lockable
            return true;
        } else { // Indirectly lockable
            Block blockup = block.getRelative(BlockFace.UP);
            if (isUpDownAlsoLockableBlock(blockup)) return true;
            Block blockdown = block.getRelative(BlockFace.DOWN);
            return isUpDownAlsoLockableBlock(blockdown);
        }
    }


    public static boolean isChest(Block block) {
        return block.getBlockData() instanceof Chest || block.getBlockData() instanceof DoubleChest;
    }

    public static boolean isUpDownAlsoLockableBlock(Block block) {
        if (Config.isLockable(block.getType())) {
            return (block.getBlockData() instanceof Door);
        }
        return false;
    }

    public static boolean mayInterfere(Block block, Player player) {
        if (block.getState() instanceof Container) {
            for (BlockFace blockface : allFaces) {
                Block newblock = block.getRelative(blockface);
                if (isLocked(newblock) && !isOwner(newblock, player)) {
                    return true;
                }
            }
        }

        if (block.getBlockData() instanceof Door) {
            for (BlockFace blockface : cardinalFaces) {
                Block newblock = block.getRelative(blockface);
                if (newblock.getBlockData() instanceof Door) {
                    if (isLocked(newblock) && !isOwner(newblock, player)) {
                        return true;
                    }
                }
            }
            // Temp workaround bad code for checking up and down signs
            Block newblock2 = block.getRelative(BlockFace.UP, 2);
            if (isLocked(newblock2) && !isOwner(newblock2, player)) {
                return true;
            }
            Block newblock3 = block.getRelative(BlockFace.DOWN, 1);
            if (isLocked(newblock3) && !isOwner(newblock3, player)) {
                return true;
            }
            // End temp workaround bad code for checking up and down signs
        }
        if (Tag.SIGNS.isTagged(block.getType()) ||
                block.getBlockData() instanceof Chest ||
                block.getBlockData() instanceof DoubleChest) {
            for (BlockFace blockface : allFaces) {
                Block newblock = block.getRelative(blockface);
                if (newblock.getBlockData() instanceof Chest ||
                        newblock.getBlockData() instanceof DoubleChest) {
                    if (isLockedSingleBlock(newblock, null) && !isOwnerSingleBlock(newblock, null, player)) {
                        return true;
                    }
                }
            }
        }

        // if LEFT may interfere RIGHT
        switch (block.getType()) {

            // This is extra interfere block
            case HOPPER, DISPENSER, DROPPER -> {
                if (!Config.isInterferePlacementBlocked()) return false;
                for (BlockFace blockface : allFaces) {
                    Block newblock = block.getRelative(blockface);
                    switch (newblock.getType()) {
                        case CHEST:
                        case TRAPPED_CHEST:
                        case HOPPER:
                        case DISPENSER:
                        case DROPPER:
                            if (isLocked(newblock) && !isOwner(newblock, player)) {
                                return true;
                            }
                        default:
                            break;
                    }
                }
            }
            default -> {
            }
        }
        return false;
    }

    private static boolean isValidLockSign(@Nullable Sign sign) {//Please mind, a private sign may have expired, but do how two locked blocks line up it's totally possible for more than one [Private] sign per block. However only the first valid found wil get used
        if (sign != null && isLockSign(sign)) {
            // Found [Private] sign, is expire turned on and expired? (relativeblock is now sign)
            return !Config.isLockExpire() || !isSignExpired(sign);
        } else {
            return false;
        }
    }

    public static boolean isLockSign(@NotNull Sign sign) {
        return LockSign.isLockSign(sign);
    }

    @Deprecated(forRemoval = true)
    public static boolean isAdditionalSign(@NotNull Sign sign) {
        return LockSign.isAdditionalSign(sign);
    }

    public static boolean isSignExpired(Sign sign) {
        return ExpireSign.isSignExpired(sign);
    }

    public static boolean isUpDownOfLockedDoor(Block block) { //todo
        Block blockup = block.getRelative(BlockFace.UP);
        if (isUpDownAlsoLockableBlock(blockup) && isLocked(blockup)) return true;
        Block blockdown = block.getRelative(BlockFace.DOWN);
        return isUpDownAlsoLockableBlock(blockdown) && isLocked(blockdown);
    }

    public static boolean isOwnerUpDownLockedDoor(Block block, OfflinePlayer player) {
        Block blockup = block.getRelative(BlockFace.UP);
        if (isUpDownAlsoLockableBlock(blockup) && isOwner(blockup, player)) return true;
        Block blockdown = block.getRelative(BlockFace.DOWN);
        return isUpDownAlsoLockableBlock(blockdown) && isOwner(blockdown, player);
    }

    public static boolean isUserUpDownLockedDoor(Block block, Player player) {
        Block blockup = block.getRelative(BlockFace.UP);
        if (isUpDownAlsoLockableBlock(blockup) && isMember(blockup, player)) return true;
        Block blockdown = block.getRelative(BlockFace.DOWN);
        return isUpDownAlsoLockableBlock(blockdown) && isMember(blockdown, player);
    }

    public static @Nullable Block getAttachedBlock(Block signBlock) { // Requires isSign
        if (signBlock.getBlockData() instanceof Directional directional) {
            return signBlock.getRelative(directional.getFacing().getOppositeFace());
        } else {
            return null;
        }
    }

    public static int getTimerOnSigns(Block block) {
        for (BlockFace blockface : cardinalFaces) {
            Block relative = block.getRelative(blockface);
            if (relative.getState() instanceof Sign sign) {
                for (String line : sign.getSide(Side.FRONT).getLines()) { //todo
                    int linetime = Config.getTimer(line);
                    if (linetime > 0) return linetime;
                }
            }
        }
        return 0;
    }

    public static int getTimerDoor(Block block) {
        int timersingle = getTimerSingleDoor(block);
        if (timersingle > 0) return timersingle;
        for (BlockFace blockface : cardinalFaces) {
            Block relative = block.getRelative(blockface);
            timersingle = getTimerSingleDoor(relative);
            if (timersingle > 0) return timersingle;
        }
        return 0;
    }

    public static int getTimerSingleDoor(Block block) {
        DoorParts doors = Doors.getDoorParts(block);
        if (doors == null) {
            return 0;
        }

        int relativeuptimer = getTimerOnSigns(doors.upPart().getRelative(BlockFace.UP));
        if (relativeuptimer > 0) {
            return relativeuptimer;
        }

        int doors0 = getTimerOnSigns(doors.downPart());
        if (doors0 > 0) {
            return doors0;
        }

        int doors1 = getTimerOnSigns(doors.upPart());
        if (doors1 > 0) {
            return doors1;
        }

        int relativedowntimer = getTimerOnSigns(doors.downPart().getRelative(BlockFace.DOWN));
        return Math.max(relativedowntimer, 0);
    }

    public static @Nullable BlockFace getRelativeChestFace(Chest chest) {
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

    @Deprecated
    public static boolean isLockString(String line) { //todocomponent
        return LockSign.isLockString(line);
    }

    public static boolean isLockComp(Component line) { //todocomponent
        return LockSign.isLockComp(line);
    }
}
