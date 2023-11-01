package de.greensurvivors.greenlocker.impl.doordata;

import de.greensurvivors.greenlocker.GreenLockerAPI;
import org.bukkit.Material;
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

public class Doors {
    private enum DoorSound { // todo find a way to use net.minecraft.world.level.block.state.properties.BlockSetType
        OAK(Material.OAK_DOOR),
        SPRUCE(Material.SPRUCE_DOOR),
        BIRCH(Material.BIRCH_DOOR),
        ACACIA(Material.ACACIA_DOOR),
        JUNGLE(Material.JUNGLE_DOOR),
        DARK_OAK(Material.DARK_OAK_DOOR),
        MANGROVE(Material.MANGROVE_DOOR),
        IRON(Material.IRON_DOOR, Sound.BLOCK_IRON_DOOR_CLOSE, Sound.BLOCK_IRON_DOOR_OPEN),
        CRIMSON(Material.CRIMSON_DOOR, Sound.BLOCK_NETHER_WOOD_DOOR_CLOSE, Sound.BLOCK_NETHER_WOOD_DOOR_OPEN),
        WARPED(Material.WARPED_DOOR, Sound.BLOCK_NETHER_WOOD_DOOR_CLOSE, Sound.BLOCK_NETHER_WOOD_DOOR_OPEN),
        CHERRY(Material.CHERRY_DOOR, Sound.BLOCK_CHERRY_WOOD_DOOR_CLOSE, Sound.BLOCK_CHERRY_WOOD_DOOR_OPEN),
        BAMBOO(Material.BAMBOO_DOOR, Sound.BLOCK_BAMBOO_WOOD_DOOR_CLOSE, Sound.BLOCK_BAMBOO_WOOD_DOOR_OPEN);

        private final Material material;
        private final Sound closeSound;
        private final Sound openSound;

        DoorSound(Material material) {
            this.material = material;
            this.closeSound = Sound.BLOCK_WOODEN_DOOR_CLOSE;
            this.openSound = Sound.BLOCK_WOODEN_DOOR_OPEN;
        }

        DoorSound(Material material, Sound closeSound, Sound openSound) {
            this.material = material;
            this.closeSound = closeSound;
            this.openSound = openSound;
        }

        public static Sound getCloseSound(Material material) {
            for (DoorSound doorSound : DoorSound.values()) {
                if (doorSound.material.equals(material)) {
                    return doorSound.closeSound;
                }
            }

            // fallback in case a new door wasn't implemented yet
            return Sound.BLOCK_WOODEN_DOOR_CLOSE;
        }

        public static Sound getOpenSound(Material material) {
            for (DoorSound doorSound : DoorSound.values()) {
                if (doorSound.material.equals(material)) {
                    return doorSound.openSound;
                }
            }

            // fallback in case a new door wasn't implemented yet
            return Sound.BLOCK_WOODEN_DOOR_OPEN;
        }
    }
    public static void toggleDoor(Block block) {
        if (block.getBlockData() instanceof Openable openable) {
            boolean open = !openable.isOpen();

            openable.setOpen(open);
            block.setBlockData(openable);
            block.getWorld().playSound(block.getLocation(), open ? DoorSound.getOpenSound(block.getType()) : DoorSound.getCloseSound(block.getType()), 1, 1);
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

        for (BlockFace doorface : GreenLockerAPI.cardinalFaces) {
            Block relative0 = door.downPart().getRelative(doorface), relative1 = door.upPart().getRelative(doorface);

            if (relative0.getType() == door.downPart().getType() && relative1.getType() == door.upPart().getType()) {
                adjacent.put(doorface, new DoorParts(relative0, relative1));
            }
        }

        return adjacent;
    }
}
