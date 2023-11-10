package de.greensurvivors.padlock.impl;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utilitys that don't fit anywhere else
 */
public class MiscUtils {
    // the point at the beginning is for bedrock player if the proxy supports them. //todo make this configurable as it should be in the proxy
    private static final Pattern usernamePattern = Pattern.compile("^.?[a-zA-Z0-9_]{3,16}$");
    /**
     * Set containing all players that have been notified about being able to lock things
     */
    private static final Set<UUID> notified = new HashSet<>();

    /**
     * removes one item of the players main hand.
     */
    public static void removeAItemMainHand(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (player.getInventory().getItemInMainHand().getAmount() == 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        }
    }

    /**
     * checks if a player has not been notified before and adds them to the notified list if not
     */
    public static boolean shouldNotify(Player player) {
        if (notified.contains(player.getUniqueId())) {
            return false;
        } else {
            notified.add(player.getUniqueId());
            return true;
        }
    }

    /**
     * check if a string is a valid username
     */
    public static boolean isUserName(String text) {
        return usernamePattern.matcher(text).matches();
    }

    /**
     * get the direction a block is facing or null if not directional
     */
    private static @Nullable BlockFace getFacing(@NotNull Block block) {
        if (block.getBlockData() instanceof Directional directional) {
            return directional.getFacing();
        } else {
            return null;
        }
    }

    /**
     * get a sign relative in direction of the blockface facing the same direction.
     * or null if not found / wrong state
     */
    public static @Nullable Sign getFacingSign(@NotNull Block block, @NotNull BlockFace blockface) {
        Block relativeblock = block.getRelative(blockface);

        if (relativeblock.getState() instanceof Sign sign && getFacing(relativeblock) == blockface) {
            return sign;
        } else {
            return null;
        }
    }
}
