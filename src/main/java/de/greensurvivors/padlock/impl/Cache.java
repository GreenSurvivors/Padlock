package de.greensurvivors.padlock.impl;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
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
        block.removeMetadata("expires", Padlock.getPlugin());
        block.removeMetadata("locked", Padlock.getPlugin());
        block.setMetadata("expires", new FixedMetadataValue(Padlock.getPlugin(), System.currentTimeMillis() + Padlock.getPlugin().getConfigManager().getCacheTimeMillis()));
        block.setMetadata("locked", new FixedMetadataValue(Padlock.getPlugin(), access));
    }

    public static void resetCache(Block block) {
        block.removeMetadata("expires", Padlock.getPlugin());
        block.removeMetadata("locked", Padlock.getPlugin());
        for (BlockFace blockface : PadlockAPI.cardinalFaces) {
            Block relative = block.getRelative(blockface);
            if (relative.getType() == block.getType()) {
                relative.removeMetadata("expires", Padlock.getPlugin());
                relative.removeMetadata("locked", Padlock.getPlugin());
            }
        }
    }
}
