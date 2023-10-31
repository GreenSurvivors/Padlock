package me.crafter.mc.lockettepro.impl.doordata;

import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static me.crafter.mc.lockettepro.LocketteProAPI.cardinalFaces;

public class Doors {
    public static void toggleDoor(Block block) {
        if (block.getBlockData() instanceof Openable openablestate) {
            boolean open = !openablestate.isOpen();

            openablestate.setOpen(open);
            block.setBlockData(openablestate);
            block.getWorld().playSound(block.getLocation(), open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE, 1, 1); //todo this plays allways, even in case of iron doors.
        }
    }

    public static @Nullable DoorParts getDoorParts(@NotNull Block block) {
        DoorParts door = null;
        Block up = block.getRelative(BlockFace.UP), down = block.getRelative(BlockFace.DOWN);

        if (up.getType() == block.getType()) {
            door = new DoorParts(up, block);
        }

        if (down.getType() == block.getType()) {
            if (door != null) { // error 3 doors
                return null;
            }

            door = new DoorParts(block, down);
        }

        return door;
    }

    public static boolean isDoubleDoorBlock(@NotNull Block block) {
        return Tag.DOORS.isTagged(block.getType());
    }

    public static boolean isSingleDoorBlock(@NotNull Block block) {
        return block.getBlockData() instanceof TrapDoor || block.getBlockData() instanceof Gate;
    }

    public static Block getBottomDoorBlock(@NotNull Block block) { // Requires isDoubleDoorBlock || isSingleDoorBlock
        if (isDoubleDoorBlock(block)) {
            Block relative = block.getRelative(BlockFace.DOWN);
            if (relative.getType() == block.getType()) {
                return relative;
            } else {
                return block;
            }
        } else {
            return block;
        }
    }

    /**
     * get mapping from cardinal directions to adjacent doors
     *
     * @param door
     * @return
     */
    public static Map<BlockFace, DoorParts> getConnectedDoors(@NotNull DoorParts door) {
        Map<BlockFace, DoorParts> adjacent = new HashMap<>();

        for (BlockFace doorface : cardinalFaces) {
            Block relative0 = door.downPart().getRelative(doorface), relative1 = door.upPart().getRelative(doorface);

            if (relative0.getType() == door.downPart().getType() && relative1.getType() == door.upPart().getType()) {
                adjacent.put(doorface, new DoorParts(relative0, relative1));
            }
        }

        return adjacent;
    }
}
