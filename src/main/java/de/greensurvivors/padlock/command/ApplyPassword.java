package de.greensurvivors.padlock.command;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.config.PermissionManager;
import de.greensurvivors.padlock.impl.SignSelection;
import de.greensurvivors.padlock.impl.internal.InputAnvilMenu;
import de.greensurvivors.padlock.impl.internal.VersionManager;
import de.greensurvivors.padlock.impl.signdata.SignPasswords;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Please note: This is a main command as well as a subcommand.
 * But since we are handling passwords, we want to leak them as less as possible,
 * the input will get handled by an AnvilGUI.
 * <p>Also, {@link MainCommand} as well as {@link Padlock} itself has a separate instance of this,</p>
 */
public class ApplyPassword extends SubCommand implements TabCompleter, CommandExecutor, Listener {
    private final WeakHashMap<InventoryView, Sign> openInventories = new WeakHashMap<>();

    public ApplyPassword(@NotNull Padlock plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = false)
    private void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (openInventories.containsKey(event.getView())) {
            event.getInventory().clear();
        }
    }

    @EventHandler(ignoreCancelled = false)
    private void onInventoryClick(@NotNull InventoryClickEvent event) {
        final InventoryView view = event.getView();

        if (openInventories.containsKey(view)) {
            if (event.getView().getTopInventory() instanceof InputAnvilMenu anvilInventory) {
                switch (event.getAction()) {
                    case NOTHING, DROP_ALL_CURSOR, DROP_ONE_CURSOR -> {
                    } // nothing
                    case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR, HOTBAR_SWAP, DROP_ALL_SLOT, DROP_ONE_SLOT,
                         COLLECT_TO_CURSOR, UNKNOWN -> {
                        if (event.getClickedInventory() == anvilInventory) {
                            event.setResult(Event.Result.DENY);
                        }
                    }
                    case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE -> {
                        if (event.getClickedInventory() == anvilInventory) {
                            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                                char[] password = anvilInventory.getRenameChars();

                                if (password != null) {
                                    //this will communicate if access was granted or not
                                    SignPasswords.checkPasswordAndGrandAccess(openInventories.get(view), event.getWhoClicked(), password);
                                    Arrays.fill(password, '*');
                                    Bukkit.getScheduler().runTaskLater(plugin, view::close, 1L); // delay closing one tick, so the event can correctly run
                                } else {
                                    plugin.getMessageManager().sendLang(event.getWhoClicked(), MessageManager.LangPath.PASSWORD_ERROR_EMPTY);
                                }
                            }

                            event.setResult(Event.Result.DENY);
                        }
                    }
                    case MOVE_TO_OTHER_INVENTORY -> {
                        if (event.getClickedInventory() == anvilInventory && event.getSlotType() == InventoryType.SlotType.RESULT) {
                            char[] password = anvilInventory.getRenameChars();

                            if (password != null) {
                                //this will communicate if access was granted or not
                                SignPasswords.checkPasswordAndGrandAccess(openInventories.get(view), event.getWhoClicked(), password);
                                Bukkit.getScheduler().runTaskLater(plugin, view::close, 1L); // delay closing one tick, so the event can correctly run
                            } else {
                                plugin.getMessageManager().sendLang(event.getWhoClicked(), MessageManager.LangPath.PASSWORD_ERROR_EMPTY);
                            }
                        }

                        event.setResult(Event.Result.DENY);
                    }
                }
            } else { // error;
                plugin.getLogger().warning("Got wrong Inventory type while setting password. How did this happen?");
                event.setResult(Event.Result.DENY);
                view.close();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (openInventories.containsKey(event.getView())) {
            for (int rawSlotId : event.getRawSlots()) {
                if (rawSlotId < event.getView().getTopInventory().getSize()) {
                    event.setResult(Event.Result.DENY);
                }
            }
        }
    }

    @Override
    protected boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PermissionManager.CMD_APPLY_PASSWORD.getPerm());
    }

    @Override
    public @NotNull Set<String> getAliases() {
        return Set.of("password", "pw", "applypassword", "usepassword");
    }

    @Override
    protected @NotNull Component getHelpText() {
        return plugin.getMessageManager().getLang(MessageManager.LangPath.HELP_PASSWORD);
    }

    @Override
    protected boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (Bukkit.isPrimaryThread()) {
                // check permission for passwords
                if (player.hasPermission(PermissionManager.CMD_APPLY_PASSWORD.getPerm())) {

                    //get and check selected sign
                    Sign sign = SignSelection.getSelectedSign(player);

                    if (sign != null) {
                        //check for old Lockett(Pro) signs and try to update them
                        sign = MainCommand.checkAndUpdateLegacySign(sign, player);
                        if (sign == null) {
                            Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SIGN_NEED_RESELECT);
                            return true;
                        }

                        if (PadlockAPI.isLockSign(sign)) {
                            if (!SignPasswords.isOnCooldown(player.getUniqueId(), sign.getLocation())) {
                                plugin.getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_START_PROCESSING);

                                InventoryView view = VersionManager.openInputAnvil(player, plugin.getMessageManager().getLang(MessageManager.LangPath.PASSWORD_INPUT_APPLY_TITLE), Component.empty());

                                AnvilInventory inventory = (AnvilInventory) view.getTopInventory();

                                ItemStack stack = new ItemStack(Material.NAME_TAG);
                                ItemMeta meta = stack.getItemMeta();
                                meta.displayName(plugin.getMessageManager().getLang(MessageManager.LangPath.PASSWORD_DEFAULT_PASSWORD));
                                stack.setItemMeta(meta);

                                inventory.setFirstItem(stack);

                                openInventories.put(view, sign);
                            } else {
                                Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.PASSWORD_ON_COOLDOWN);
                            }
                        } else {
                            Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SIGN_NEED_RESELECT);
                        }
                    } else {
                        Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.SIGN_NOT_SELECTED);
                    }
                } else {
                    Padlock.getPlugin().getMessageManager().sendLang(player, MessageManager.LangPath.NO_PERMISSION);
                }
            } else {
                plugin.getLogger().warning("Command apply password was run async and failed!");
            }
            return true;
        } else {
            plugin.getMessageManager().sendLang(sender, MessageManager.LangPath.NOT_A_PLAYER);
            return false;
        }
    }

    @Override
    protected @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return onTabComplete(sender, args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return onCommand(sender, args);
    }
}
