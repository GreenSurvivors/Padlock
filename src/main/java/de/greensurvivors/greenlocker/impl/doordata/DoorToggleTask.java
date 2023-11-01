package de.greensurvivors.greenlocker.impl.doordata;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class DoorToggleTask implements Runnable {
    private final List<Block> doors;
    private final Plugin plugin;

    public DoorToggleTask(Plugin plugin, List<Block> doors) {
        this.doors = doors;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Block door : doors) {
            door.removeMetadata("greenlocker.toggle", plugin);
        }
        for (Block door : doors) {
            if (Doors.isDoubleDoorBlock(door)) {
                Block doorbottom = Doors.getBottomDoorBlock(door);
                //GreenLockerAPI.toggleDoor(doorbottom, open);
                Doors.toggleDoor(doorbottom);
            }
        }
    }

}
