package me.crafter.mc.lockettepro.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SignSelection {
    private static final LoadingCache<UUID, Block> selectedsign = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                public @NotNull Block load(@NotNull UUID key) {
                    return null;
                }
            });

    public static Block getSelectedSign(Player player) {
        Block b = selectedsign.getIfPresent(player.getUniqueId());
        if (b != null && !player.getWorld().getName().equals(b.getWorld().getName())) {
            selectedsign.invalidate(player.getUniqueId());
            return null;
        }
        return b;
    }

    public static void selectSign(Player player, Block block) {
        selectedsign.put(player.getUniqueId(), block);
    }
}
