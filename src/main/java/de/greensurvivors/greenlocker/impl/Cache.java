package de.greensurvivors.greenlocker.impl;

import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.GreenLockerAPI;
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
        block.removeMetadata("expires", GreenLocker.getPlugin());
        block.removeMetadata("locked", GreenLocker.getPlugin());
        block.setMetadata("expires", new FixedMetadataValue(GreenLocker.getPlugin(), System.currentTimeMillis() + GreenLocker.getPlugin().getConfigManager().getCacheTimeMillis()));
        block.setMetadata("locked", new FixedMetadataValue(GreenLocker.getPlugin(), access));
    }

    public static void resetCache(Block block) {
        block.removeMetadata("expires", GreenLocker.getPlugin());
        block.removeMetadata("locked", GreenLocker.getPlugin());
        for (BlockFace blockface : GreenLockerAPI.cardinalFaces) {
            Block relative = block.getRelative(blockface);
            if (relative.getType() == block.getType()) {
                relative.removeMetadata("expires", GreenLocker.getPlugin());
                relative.removeMetadata("locked", GreenLocker.getPlugin());
            }
        }
    }
}
