package de.greensurvivors.padlock.impl.internal;

import org.bukkit.craftbukkit.inventory.view.CraftAnvilView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NMSAnvilMenu {

    @NotNull
    CraftAnvilView getBukkitView();

    char @Nullable [] getLastInput();
}
