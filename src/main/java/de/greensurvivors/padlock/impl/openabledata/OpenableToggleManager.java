package de.greensurvivors.padlock.impl.openabledata;

import de.greensurvivors.padlock.Padlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OpenableToggleManager {
    private final @NotNull Padlock plugin;
    private final @NotNull Map<@NotNull Location, @NotNull BukkitTask> toggleTasks = new HashMap<>();

    public OpenableToggleManager(@NotNull Padlock plugin) {
        this.plugin = plugin;
    }

    public void cancelAllTasks() {
        Set<BukkitTask> canceledTasks = new HashSet<>();

        for (Iterator<Map.Entry<Location, BukkitTask>> it = toggleTasks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Location, BukkitTask> entry = it.next();
            BukkitTask task = entry.getValue();

            if (!canceledTasks.contains(task)) {
                task.cancel();
                canceledTasks.add(task);
            }

            it.remove();
        }
    }

    public void toggleCancelRunning(final @NotNull Set<@NotNull Block> blocksToToggle, final long timeUntilToggle) {
        Set<Location> locationsToToggle = blocksToToggle.stream().map(Block::getLocation).collect(Collectors.toSet());

        // remove running tasks
        boolean found = false;
        for (Location locationCheck : locationsToToggle) {
            BukkitTask task = toggleTasks.get(locationCheck);

            if (task != null) {
                found = true;
                toggleTasks.entrySet().removeIf(entry -> entry.getValue().equals(task));
                task.cancel();
            }
        }

        if (!found) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                BukkitTask taskToClearUp = null;

                for (Location location : locationsToToggle) {
                    //get this very task
                    if (taskToClearUp == null) {
                        taskToClearUp = toggleTasks.get(location);
                    }

                    Block openableBlock = location.getBlock();

                    if (Tag.DOORS.isTagged(openableBlock.getType())) {
                        DoorParts doorParts = Openables.getDoorParts(openableBlock);

                        if (doorParts != null) {
                            openableBlock = doorParts.downPart();
                        }
                    }

                    Openables.toggleOpenable(openableBlock);

                    if (taskToClearUp != null) {
                        final BukkitTask finalTaskToClearUp = taskToClearUp; // just Java being Java and needing a final variable, even if this can't change at this point.
                        toggleTasks.entrySet().removeIf(entry -> entry.getValue().equals(finalTaskToClearUp));
                    }
                }
            }, TimeUnit.MILLISECONDS.toSeconds(timeUntilToggle) * 20L);

            for (Location location : locationsToToggle) {
                toggleTasks.put(location, task);
            }
        }
    }
}
