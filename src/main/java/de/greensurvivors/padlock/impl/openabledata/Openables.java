package de.greensurvivors.padlock.impl.openabledata;

import de.greensurvivors.padlock.PadlockAPI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Openables {
    /**
     * open/closes the openable block (door, fence gate, trapdoor,...)
     * will do nothing if the block can't be opened.
     */
    public static void toggleOpenable(@NotNull Block block) {
        if (block.getBlockData() instanceof Openable openable) {
            boolean open = !openable.isOpen();

            openable.setOpen(open);
            block.setBlockData(openable);
            block.getWorld().playSound(block.getLocation(), open ? OpenableSound.getOpenSound(block.getType()) : OpenableSound.getCloseSound(block.getType()), 1, 1);
        }
    }

    /**
     * @param block
     * @return
     */
    public static @Nullable DoorParts getDoorParts(@NotNull Block block) {
        if (Tag.DOORS.isTagged(block.getType())) {
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
        } else {
            return null;
        }
    }

    /**
     * Note: Barrels are openable as well but this is just visual and closes itself.
     *
     * @param material
     * @return
     */
    public static boolean isSingleOpenable(@NotNull Material material) {
        return Tag.TRAPDOORS.isTagged(material) || Tag.FENCE_GATES.isTagged(material);
    }

    /**
     * get mapping from cardinal directions to adjacent doors
     *
     * @param door
     * @return
     */
    public static @NotNull Map<@NotNull BlockFace, @NotNull DoorParts> getConnectedDoors(@NotNull DoorParts door) {
        Map<BlockFace, DoorParts> adjacent = new HashMap<>();

        for (BlockFace doorface : PadlockAPI.cardinalFaces) {
            Block relative0 = door.downPart().getRelative(doorface), relative1 = door.upPart().getRelative(doorface);

            if (relative0.getType() == door.downPart().getType() && relative1.getType() == door.upPart().getType()) {
                adjacent.put(doorface, new DoorParts(relative0, relative1));
            }
        }

        return adjacent;
    }

    private enum OpenableSound { // todo find a way to use net.minecraft.world.level.block.state.properties.BlockSetType
        OAK_DOOR(Material.OAK_DOOR),
        SPRUCE_DOOR(Material.SPRUCE_DOOR),
        BIRCH_DOOR(Material.BIRCH_DOOR),
        ACACIA_DOOR(Material.ACACIA_DOOR),
        JUNGLE_DOOR(Material.JUNGLE_DOOR),
        DARK_OAK_DOOR(Material.DARK_OAK_DOOR),
        MANGROVE_DOOR(Material.MANGROVE_DOOR),
        IRON_DOOR(Material.IRON_DOOR, Sound.BLOCK_IRON_DOOR_CLOSE, Sound.BLOCK_IRON_DOOR_OPEN),
        CRIMSON_DOOR(Material.CRIMSON_DOOR, Sound.BLOCK_NETHER_WOOD_DOOR_CLOSE, Sound.BLOCK_NETHER_WOOD_DOOR_OPEN),
        WARPED_DOOR(Material.WARPED_DOOR, Sound.BLOCK_NETHER_WOOD_DOOR_CLOSE, Sound.BLOCK_NETHER_WOOD_DOOR_OPEN),
        CHERRY_DOOR(Material.CHERRY_DOOR, Sound.BLOCK_CHERRY_WOOD_DOOR_CLOSE, Sound.BLOCK_CHERRY_WOOD_DOOR_OPEN),
        BAMBOO_DOOR(Material.BAMBOO_DOOR, Sound.BLOCK_BAMBOO_WOOD_DOOR_CLOSE, Sound.BLOCK_BAMBOO_WOOD_DOOR_OPEN),
        OAK_TRAPDOOR(Material.OAK_TRAPDOOR),
        SPRUCE_TRAPDOOR(Material.SPRUCE_TRAPDOOR),
        BIRCH_TRAPDOOR(Material.BIRCH_TRAPDOOR),
        ACACIA_TRAPDOOR(Material.ACACIA_TRAPDOOR),
        JUNGLE_TRAPDOOR(Material.JUNGLE_TRAPDOOR),
        DARK_OAK_TRAPDOOR(Material.DARK_OAK_TRAPDOOR),
        MANGROVE_TRAPDOOR(Material.MANGROVE_TRAPDOOR),
        IRON_TRAPDOOR(Material.IRON_TRAPDOOR, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, Sound.BLOCK_IRON_TRAPDOOR_OPEN),
        CRIMSON_TRAPDOOR(Material.CRIMSON_TRAPDOOR, Sound.BLOCK_NETHER_WOOD_TRAPDOOR_CLOSE, Sound.BLOCK_NETHER_WOOD_TRAPDOOR_OPEN),
        WARPED_TRAPDOOR(Material.WARPED_TRAPDOOR, Sound.BLOCK_NETHER_WOOD_TRAPDOOR_CLOSE, Sound.BLOCK_NETHER_WOOD_TRAPDOOR_OPEN),
        CHERRY_TRAPDOOR(Material.CHERRY_TRAPDOOR, Sound.BLOCK_CHERRY_WOOD_TRAPDOOR_CLOSE, Sound.BLOCK_CHERRY_WOOD_TRAPDOOR_OPEN),
        BAMBOO_TRAPDOOR(Material.BAMBOO_TRAPDOOR, Sound.BLOCK_BAMBOO_WOOD_TRAPDOOR_CLOSE, Sound.BLOCK_BAMBOO_WOOD_TRAPDOOR_OPEN),
        OAK_FENCE_GATE(Material.OAK_FENCE_GATE),
        SPRUCE_FENCE_GATE(Material.SPRUCE_FENCE_GATE),
        BIRCH_FENCE_GATE(Material.BIRCH_FENCE_GATE),
        ACACIA_FENCE_GATE(Material.ACACIA_FENCE_GATE),
        JUNGLE_FENCE_GATE(Material.JUNGLE_FENCE_GATE),
        DARK_OAK_FENCE_GATE(Material.DARK_OAK_FENCE_GATE),
        MANGROVE_FENCE_GATE(Material.MANGROVE_FENCE_GATE),
        CRIMSON_FENCE_GATE(Material.CRIMSON_FENCE_GATE, Sound.BLOCK_NETHER_WOOD_FENCE_GATE_CLOSE, Sound.BLOCK_NETHER_WOOD_FENCE_GATE_OPEN),
        WARPED_FENCE_GATE(Material.WARPED_FENCE_GATE, Sound.BLOCK_NETHER_WOOD_FENCE_GATE_CLOSE, Sound.BLOCK_NETHER_WOOD_FENCE_GATE_OPEN),
        CHERRY_FENCE_GATE(Material.CHERRY_FENCE_GATE, Sound.BLOCK_CHERRY_WOOD_FENCE_GATE_CLOSE, Sound.BLOCK_CHERRY_WOOD_FENCE_GATE_OPEN),
        BAMBOO_FENCE_GATE(Material.BAMBOO_FENCE_GATE, Sound.BLOCK_BAMBOO_WOOD_FENCE_GATE_CLOSE, Sound.BLOCK_BAMBOO_WOOD_FENCE_GATE_OPEN);

        private final Material material;
        private final Sound closeSound;
        private final Sound openSound;

        OpenableSound(@NotNull Material material) {
            this.material = material;
            if (Tag.DOORS.isTagged(material)) {
                this.closeSound = Sound.BLOCK_WOODEN_DOOR_CLOSE;
                this.openSound = Sound.BLOCK_WOODEN_DOOR_OPEN;
            } else if (Tag.TRAPDOORS.isTagged(material)) {
                this.closeSound = Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
                this.openSound = Sound.BLOCK_WOODEN_TRAPDOOR_OPEN;
            } else if (Tag.FENCE_GATES.isTagged(material)) {
                this.closeSound = Sound.BLOCK_FENCE_GATE_CLOSE;
                this.openSound = Sound.BLOCK_FENCE_GATE_OPEN;
            } else { // I have no idea. Should never happen...
                this.closeSound = Sound.ENTITY_VILLAGER_NO;
                this.openSound = Sound.ENTITY_VILLAGER_YES;
            }
        }

        OpenableSound(@NotNull Material material, @NotNull Sound closeSound, @NotNull Sound openSound) {
            this.material = material;
            this.closeSound = closeSound;
            this.openSound = openSound;
        }

        public static @NotNull Sound getCloseSound(@NotNull Material material) {
            for (OpenableSound doorSound : OpenableSound.values()) {
                if (doorSound.material.equals(material)) {
                    return doorSound.closeSound;
                }
            }

            // fallback in case a new door wasn't implemented yet
            if (Tag.DOORS.isTagged(material)) {
                return Sound.BLOCK_WOODEN_DOOR_CLOSE;
            } else if (Tag.TRAPDOORS.isTagged(material)) {
                return Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
            } else if (Tag.FENCE_GATES.isTagged(material)) {
                return Sound.BLOCK_FENCE_GATE_CLOSE;
            } else { // I have no idea. Should never happen...
                return Sound.ENTITY_VILLAGER_NO;
            }
        }

        public static @NotNull Sound getOpenSound(@NotNull Material material) {
            for (OpenableSound doorSound : OpenableSound.values()) {
                if (doorSound.material.equals(material)) {
                    return doorSound.openSound;
                }
            }

            // fallback in case a new door wasn't implemented yet
            if (Tag.DOORS.isTagged(material)) {
                return Sound.BLOCK_WOODEN_DOOR_OPEN;
            } else if (Tag.TRAPDOORS.isTagged(material)) {
                return Sound.BLOCK_WOODEN_TRAPDOOR_OPEN;
            } else if (Tag.FENCE_GATES.isTagged(material)) {
                return Sound.BLOCK_FENCE_GATE_OPEN;
            } else { // I have no idea. Should never happen...
                return Sound.ENTITY_VILLAGER_YES;
            }
        }
    }
}
