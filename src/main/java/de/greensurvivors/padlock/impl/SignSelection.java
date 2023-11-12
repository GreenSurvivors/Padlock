package de.greensurvivors.padlock.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Caches
 */
public class SignSelection {
    private final static @NotNull Cache<UUID, Block> selectedSign =
            Caffeine.newBuilder().expireAfterAccess(40, TimeUnit.SECONDS).build();

    public static @Nullable Sign getSelectedSign(@NotNull Player player) {
        Block signBlock = selectedSign.getIfPresent(player.getUniqueId());

        if (signBlock != null && player.getWorld().getName().equals(signBlock.getWorld().getName())) {
            if (signBlock.getState() instanceof Sign sign) {
                return sign;
            }
        } else {
            selectedSign.invalidate(player.getUniqueId());
        }

        return null;
    }

    public static void selectSign(@NotNull Player player, @NotNull Block signBlock) {
        selectedSign.put(player.getUniqueId(), signBlock);
    }
}
