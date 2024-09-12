package de.greensurvivors.padlock.impl.openabledata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.impl.dataTypes.DoubleBlockParts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for openables
 */
public class Openables {
    // cache found methods, since reflection is expensive
    private static final Map<@NotNull Class<?>, @NotNull Method> cachedMethods = new HashMap<>();

    /**
     * open/closes the openable block (door, fence gate, trapdoor,...)
     * will do nothing if the block can't be opened.
     */
    public static void toggleOpenable(@NotNull org.bukkit.entity.Player player, @NotNull Block block) {
        if (block.getBlockData() instanceof Openable openable) {
            boolean open = !openable.isOpen();

            if (block.getBlockData() instanceof CraftBlockData craftBlockData) {
                net.minecraft.world.level.block.Block nmsBlock = CraftBlockType.bukkitToMinecraft(block.getType());
                Location location = block.getLocation();

                final BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                final ServerLevel level = ((CraftWorld) block.getWorld()).getHandle();
                final ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

                switch (nmsBlock) {
                    case DoorBlock doorBlock -> {
                        // don't use useWithoutItem because of iron door check
                        doorBlock.setOpen(serverPlayer, level, craftBlockData.getState(), blockPos, open);

                        return;
                    }
                    case TrapDoorBlock trapDoorBlock -> {
                        try {
                            Class<?> clazz = trapDoorBlock.getClass();
                            Method method = cachedMethods.get(clazz);

                            if (method == null) {
                                // don't use useWithoutItem because of iron trapdoor check
                                method = clazz.getDeclaredMethod("toggle", BlockState.class, Level.class, BlockPos.class, Player.class);
                                method.setAccessible(true);

                                cachedMethods.put(clazz, method);
                            }

                            method.invoke(trapDoorBlock, craftBlockData.getState(), level, blockPos, serverPlayer);

                            return;
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            Padlock.getPlugin().getComponentLogger().warn("Could use \"useWithoutItem\" of trapdoor, block: " + block, e);
                        }
                    }
                    case FenceGateBlock fenceGateBlock -> {
                        try {
                            Class<?> clazz = fenceGateBlock.getClass();
                            Method method = cachedMethods.get(clazz);

                            if (method == null) {
                                method = clazz.getDeclaredMethod("useWithoutItem", BlockState.class, Level.class, BlockPos.class, Player.class, BlockHitResult.class);
                                method.setAccessible(true);

                                cachedMethods.put(clazz, method);
                            }

                            method.invoke(fenceGateBlock, craftBlockData.getState(), level, blockPos, serverPlayer, null);

                            return;
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            Padlock.getPlugin().getComponentLogger().warn("Could use \"useWithoutItem\" of fence gate, block: " + block, e);
                        }
                    }
                    case null, default -> {
                    }
                }
            }

            // unknown, get fallback
            openable.setOpen(open);
            block.setBlockData(openable);
            block.getWorld().playSound(block.getLocation(), open ? getOpenSound(block.getType()) : getCloseSound(block.getType()), 1, 1);
        }
    }

    /**
     * Get if a material is an openable, that we have to handle.
     * Note: Barrels are openable as well but this is just visual and closes itself.
     *
     * @param material the material to check
     * @return true if it is a gate / trapdoor
     */
    public static boolean isSingleOpenable(@NotNull Material material) {
        return Tag.TRAPDOORS.isTagged(material) || Tag.FENCE_GATES.isTagged(material);
    }

    /**
     * get both parts of a tow block high block like doors *wink wink*
     *
     * @param block part of a two high block
     * @return the block and it's upper / down part or null if not a double high block (1/3+)
     */
    public static @Nullable DoubleBlockParts getDoubleBlockParts(@NotNull Block block) {
        DoubleBlockParts parts = null;
        Block up = block.getRelative(BlockFace.UP), down = block.getRelative(BlockFace.DOWN);

        if (up.getType() == block.getType()) {
            parts = new DoubleBlockParts(up, block);
        }

        if (down.getType() == block.getType()) {
            if (parts != null) { // error 3 high block!
                return null;
            }

            parts = new DoubleBlockParts(block, down);
        }

        return parts;
    }

    /**
     * get surrounding single high blocks of the same type as the block given
     *
     * @param block block to get surrounding blocks for
     * @return mapping from all directions to adjacent to the block of the same type.
     * A direction might be missing if there was no fitting block
     */
    public static @NotNull Map<@NotNull BlockFace, @NotNull Block> getConnectedSingle(@NotNull Block block) {
        Map<BlockFace, Block> adjacent = new HashMap<>();

        for (BlockFace blockFace : PadlockAPI.allFaces) {
            Block relative = block.getRelative(blockFace);

            if (relative.getType() == block.getType()) {
                adjacent.put(blockFace, relative);
            }
        }

        return adjacent;
    }

    /**
     * get surrounding single high blocks of the same type as the block given
     *
     * @param parts double block to get surrounding double blocks for
     * @return get mapping from cardinal directions to adjacent double blocks of the same type.
     * A direction might be missing if there was no fitting block
     */
    public static @NotNull Map<@NotNull BlockFace, @NotNull DoubleBlockParts> getConnectedBiParts(@NotNull DoubleBlockParts parts) {
        Map<BlockFace, DoubleBlockParts> adjacent = new HashMap<>();

        for (BlockFace blockFace : PadlockAPI.cardinalFaces) {
            Block relativeUP = parts.upPart().getRelative(blockFace), relativeDown = parts.downPart().getRelative(blockFace);

            if (relativeDown.getType() == parts.downPart().getType() && relativeUP.getType() == parts.upPart().getType()) {
                adjacent.put(blockFace, new DoubleBlockParts(relativeUP, relativeDown));
            }
        }


        return adjacent;
    }

    /**
     * Returns true if the submitted block OR the one below / above it is a door
     */
    public static boolean isUpDownDoor(@NotNull Block block) {
        return  // is door
                Tag.DOORS.isTagged(block.getType()) ||
                        // Indirectly protecting a door
                        Tag.DOORS.isTagged(block.getRelative(BlockFace.UP).getType()) ||
                        Tag.DOORS.isTagged(block.getRelative(BlockFace.DOWN).getType());
    }

    private static @NotNull Sound getCloseSound(@NotNull Material material) {
        // fallback in case a material wasn't implemented yet
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

    private static @NotNull Sound getOpenSound(@NotNull Material material) {
        // fallback in case a new material wasn't implemented yet
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
