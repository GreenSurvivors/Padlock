package de.greensurvivors.padlock.impl.openabledata;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * holds both parts of a door
 */
public record DoorParts(@NotNull Block upPart, @NotNull Block downPart) {
}
