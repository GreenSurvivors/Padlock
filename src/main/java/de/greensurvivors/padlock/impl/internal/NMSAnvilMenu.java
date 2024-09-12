package de.greensurvivors.padlock.impl.internal;

import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NMSAnvilMenu {

    @NotNull
    CraftInventoryView getBukkitView();

    char @Nullable [] getLastInput();
}
