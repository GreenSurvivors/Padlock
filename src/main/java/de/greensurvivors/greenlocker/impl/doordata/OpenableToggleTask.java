package de.greensurvivors.greenlocker.impl.doordata;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class OpenableToggleTask implements Runnable {
    private final List<Block> openables;
    private final Plugin plugin;

    public OpenableToggleTask(Plugin plugin, List<Block> openables) {
        this.openables = openables;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Block openable : openables) {
            openable.removeMetadata("greenlocker.toggle", plugin);

            if (Tag.DOORS.isTagged(openable.getType())) {
                openable = Doors.getBottomDoorBlock(openable);
            }

            Doors.toggleOpenable(openable);
        }
    }

}
