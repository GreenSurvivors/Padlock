package me.crafter.mc.lockettepro.api;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public record DoorParts(@NotNull Block upPart, @NotNull Block downPart) {
}
