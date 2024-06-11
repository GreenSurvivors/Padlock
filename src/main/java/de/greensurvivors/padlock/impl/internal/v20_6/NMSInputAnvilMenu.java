package de.greensurvivors.padlock.impl.internal.v20_6;

import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>Warning: The code in this class is derived from Paper and therefore a legal grey zone.
 * Even though I changed it wildly, this will remain legally derived from Mojangs code
 * as well as the legal hell that bukkit/Spigot/paper is.
 * I don't claim to own anything of this class. Copyright by mojang (Microsoft) and everybody contributed.
 * </p><p>
 * This is a purely virtual inventory ("menu" in mojang terms)
 * There is no way to link this to a block somewhere in the world.
 * This is an overwrite of Server behavior to just act as an Input of text.
 * This will NOT work as a regular anvil! It will NOT call all Events you would expect of a normal anvil (like PrepareResultEvent)
 * This is purely intentional as we want to keep the user input as much a secret as possible!</p>
 */
// I could use more official Server API,
// but this would still build onto not API code, never could become 100% clean of mojangs code, complicate much code and slow it down.
// so until this becomes a hassle to update I believe this should be a better option.
public final class NMSInputAnvilMenu extends AnvilMenu {
    private static final String BLURRED = "*****";
    private final @Nullable Component emptyText;

    private CraftInventoryView bukkitInvView;
    private char @Nullable [] lastInput = null;

    public static InventoryView openInputAnvil(final @NotNull org.bukkit.entity.Player player, @NotNull net.kyori.adventure.text.Component title, @Nullable net.kyori.adventure.text.Component emptyText) {
        net.minecraft.world.entity.player.Player nmsPlayer = ((CraftHumanEntity) player).getHandle();
        MenuProvider provider = new SimpleMenuProvider((syncId, inventory, player2) -> new NMSInputAnvilMenu(syncId, inventory, ContainerLevelAccess.create(nmsPlayer.level(), nmsPlayer.blockPosition()), emptyText),
                PaperAdventure.asVanilla(title));

        nmsPlayer.openMenu(provider);
        nmsPlayer.containerMenu.checkReachable = false;
        nmsPlayer.containerMenu.getBukkitView();

        return nmsPlayer.containerMenu.getBukkitView();
    }

    public NMSInputAnvilMenu(int syncId, @NotNull Inventory inventory, @NotNull ContainerLevelAccess context, @Nullable net.kyori.adventure.text.Component emptyText) {
        super(syncId, inventory, context);

        if (emptyText == null) {
            this.emptyText = null;
        } else {
            this.emptyText = PaperAdventure.asVanilla(emptyText);
        }

        itemName = BLURRED;
    }

    @Contract(pure = true)
    public char @Nullable [] getLastInput() {
        return lastInput;
    }

    @Override
    protected boolean isValidBlock(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected boolean mayPickup(@NotNull Player player, boolean present) {
        return present;
    }

    @Override
    protected void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        this.inputSlots.setItem(INPUT_SLOT, ItemStack.EMPTY);
        this.inputSlots.setItem(ADDITIONAL_SLOT, ItemStack.EMPTY);
        this.resultSlots.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public void createResult() {
        final ItemStack input = this.inputSlots.getItem(INPUT_SLOT);

        if (!input.isEmpty()) { // sanity check
            ItemStack result;

            if (lastInput != null && lastInput.length > 0) {
                result = input.copy();
                result.set(DataComponents.CUSTOM_NAME, Component.literal(BLURRED));
            } else if (input.has(DataComponents.CUSTOM_NAME)) {
                result = input.copy();
                result.remove(DataComponents.CUSTOM_NAME);
            } else { // todo this should never happen
                result = ItemStack.EMPTY;
            }

            this.resultSlots.setItem(INPUT_SLOT, result);
            this.sendAllDataToRemote(); // CraftBukkit - SPIGOT-6686: Always send completed inventory to stay in sync with client
            this.broadcastChanges(); // I don't know what's the differenz between these two methods is
        }
    }

    @Override
    public boolean setItemName(final @Nullable String newInput) {
        if (newInput != null && newInput.length() <= MAX_NAME_LENGTH) {
            this.lastInput = newInput.toCharArray();
            // somehow we have to set the cost everytime either createResult() or setItemName() is called, or else the client will see a 0 and will assume a 1.
            // (derps out)
            // so in this case we just set it to number of chars to display something usefully
            this.cost.set(newInput.length());

            ItemStack resultItem = this.resultSlots.getItem(0);
            if (!resultItem.isEmpty()) {
                if (newInput.isBlank()) {
                    if (emptyText == null) {
                        resultItem.remove(DataComponents.CUSTOM_NAME);
                    } else {
                        resultItem.set(DataComponents.CUSTOM_NAME, emptyText);
                    }
                } else {
                    resultItem.set(DataComponents.CUSTOM_NAME, Component.literal(BLURRED));
                }
            }

            this.createResult();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public @NotNull CraftInventoryView getBukkitView() {
        if (this.bukkitInvView == null) {
            CraftInventory bukkitInv = new CraftAnvilInventory(this.access.getLocation(), this.inputSlots, this.resultSlots, this);
            this.bukkitInvView = new CraftInventoryView(this.player.getBukkitEntity(), bukkitInv, this);
        }

        return this.bukkitInvView;
    }

}
