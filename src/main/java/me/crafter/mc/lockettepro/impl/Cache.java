package me.crafter.mc.lockettepro.impl;

import me.crafter.mc.lockettepro.LockettePro;
import me.crafter.mc.lockettepro.LocketteProAPI;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class Cache {
    public static boolean hasValidCache(Block block) {
        List<MetadataValue> metadatas = block.getMetadata("expires");
        if (!metadatas.isEmpty()) {
            long expires = metadatas.get(0).asLong();
            return expires > System.currentTimeMillis();
        }
        return false;
    }

    public static void setCache(Block block, boolean access) {
        block.removeMetadata("expires", LockettePro.getPlugin());
        block.removeMetadata("locked", LockettePro.getPlugin());
        block.setMetadata("expires", new FixedMetadataValue(LockettePro.getPlugin(), System.currentTimeMillis() + LockettePro.getPlugin().getConfigManager().getCacheTimeMillis()));
        block.setMetadata("locked", new FixedMetadataValue(LockettePro.getPlugin(), access));
    }

    public static void resetCache(Block block) {
        block.removeMetadata("expires", LockettePro.getPlugin());
        block.removeMetadata("locked", LockettePro.getPlugin());
        for (BlockFace blockface : LocketteProAPI.cardinalFaces) {
            Block relative = block.getRelative(blockface);
            if (relative.getType() == block.getType()) {
                relative.removeMetadata("expires", LockettePro.getPlugin());
                relative.removeMetadata("locked", LockettePro.getPlugin());
            }
        }
    }
}
