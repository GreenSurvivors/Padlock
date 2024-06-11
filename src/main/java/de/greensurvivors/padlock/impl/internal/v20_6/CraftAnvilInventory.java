package de.greensurvivors.padlock.impl.internal.v20_6;

import de.greensurvivors.padlock.impl.internal.InputAnvilMenu;
import net.minecraft.world.Container;
import org.bukkit.Location;
import org.bukkit.craftbukkit.inventory.CraftInventoryAnvil;
import org.jetbrains.annotations.Nullable;

public final class CraftAnvilInventory extends CraftInventoryAnvil implements InputAnvilMenu {
    private final NMSInputAnvilMenu menu;

    public CraftAnvilInventory(Location location, Container inventory, Container resultInventory, NMSInputAnvilMenu container) {
        super(location, inventory, resultInventory, container);

        menu = container;
    }

    /**
     * use {@link #getRenameChars()} instead!
     *
     * @return
     */
    @Override
    @Deprecated
    public String getRenameText() {
        return super.getRenameText();
    }

    public char @Nullable [] getRenameChars() {
        return menu.getLastInput();
    }
}
