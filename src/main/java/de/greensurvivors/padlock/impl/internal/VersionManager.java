package de.greensurvivors.padlock.impl.internal;

import de.greensurvivors.padlock.impl.internal.v20_6.NMSInputAnvilMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VersionManager {
    public static InventoryView openInputAnvil(final @NotNull Player player, @NotNull Component title, @Nullable Component emptyText) {
        return NMSInputAnvilMenu.openInputAnvil(player, title, emptyText);
    }
}
