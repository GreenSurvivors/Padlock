package de.greensurvivors.greenlocker.impl.doordata;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public record DoorParts(@NotNull Block upPart, @NotNull Block downPart) {
}
