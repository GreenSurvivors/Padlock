package de.greensurvivors.padlock.impl.dataTypes;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * holds both parts of a two high block
 * It's important to assign the up- and down part correctly,
 * since some code uses relative positions and expects the up part to be above the down one.
 */
public record DoubleBlockParts(@NotNull Block upPart, @NotNull Block downPart) {
}
