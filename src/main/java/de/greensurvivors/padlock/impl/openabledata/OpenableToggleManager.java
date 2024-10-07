package de.greensurvivors.padlock.impl.openabledata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.impl.dataTypes.DoubleBlockParts;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all the toggle tasks for openables with timer on their locks
 */
public class OpenableToggleManager {
    private final @NotNull Padlock plugin;
    /**
     * every location maps to the task that will toggle the block there
     */
    private final @NotNull Map<@NotNull Location, @NotNull BukkitTask> toggleTasks = new HashMap<>();

    public OpenableToggleManager(@NotNull Padlock plugin) {
        this.plugin = plugin;
    }

    /**
     * cancels all running tasks and cleans up
     */
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

    /**
     * creates a new toggle task for all given blocks together.
     * If at least one of the blocks already as a task for it running the task will be canceled and no new
     * task will get created. That's because the block was already closed, and we would open it running any task at this point.
     *
     * @param timeUntilToggle time to wait until all the given blocks will get toggled in milliseconds
     */
    public void toggleCancelRunning(final @NotNull Set<@NotNull Block> blocksToToggle, Duration timeUntilToggle) {
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
                        DoubleBlockParts doorParts = Openables.getDoubleBlockParts(openableBlock);

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
                // the reason why we multiply with 20 before converting to seconds, not after (millis -> seconds --> ticks),
                // is that with time durations smaller than one second it would always result in 0 instead of an amount of ticks.
            }, timeUntilToggle.toSeconds() * 20L);

            for (Location location : locationsToToggle) {
                toggleTasks.put(location, task);
            }
        }
    }
}
