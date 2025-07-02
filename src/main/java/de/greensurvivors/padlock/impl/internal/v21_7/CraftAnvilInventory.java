package de.greensurvivors.padlock.impl.internal.v21_7;

import de.greensurvivors.padlock.impl.internal.InputAnvilMenu;
import net.minecraft.world.Container;
import org.bukkit.Location;
import org.bukkit.craftbukkit.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.inventory.view.CraftAnvilView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

public final class CraftAnvilInventory extends CraftInventoryAnvil implements InputAnvilMenu {

    public CraftAnvilInventory(Location location, Container inventory, Container resultInventory) {
        super(location, inventory, resultInventory);
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
        if (!this.getViewers().isEmpty()) { // pretty much syncWithArbitraryViewValue
            HumanEntity entity = this.getViewers().getFirst();
            if (entity != null) {
                InventoryView var4 = entity.getOpenInventory();
                if (var4 instanceof CraftAnvilView cav && cav.getHandle() instanceof NMSInputAnvilMenu nmsInputAnvilMenu) {
                    return nmsInputAnvilMenu.getLastInput();
                }
            }
        }

        return null;
    }
}
