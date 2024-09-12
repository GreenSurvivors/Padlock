package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.MiscUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class SignAccessType {
    private final static NamespacedKey accessTypeKey = new NamespacedKey(Padlock.getPlugin(), "LockAccessType");

    public static void setAccessType(@NotNull Sign sign, @NotNull AccessType accessType, boolean shouldUpdateDisplay) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            Padlock.getPlugin().getLockCacheManager().removeFromCache(sign);
        }

        PersistentDataContainer container = sign.getPersistentDataContainer();

        container.set(accessTypeKey, PersistentDataType.STRING, accessType.name());
        sign.update();

        if (shouldUpdateDisplay) {
            SignDisplay.updateDisplay(sign);
        }
    }

    /**
     * unless you work with player input use {@link #getAccessType(Sign, boolean)} instead!
     */
    public static AccessType getAccessTypeFromComp(@NotNull Component line) {
        MessageManager manager = Padlock.getPlugin().getMessageManager();

        if (manager.isSignComp(line, MessageManager.LangPath.SIGN_LINE_PRIVATE)) {
            return AccessType.PRIVATE;
        } else if (manager.isSignComp(line, MessageManager.LangPath.SIGN_LINE_PUBLIC)) {
            return AccessType.PUBLIC;
        } else if (manager.isSignComp(line, MessageManager.LangPath.SIGN_LINE_DONATION)) {
            return AccessType.DONATION;
        } else if (manager.isSignComp(line, MessageManager.LangPath.SIGN_LINE_DISPLAY)) {
            return AccessType.DISPLAY;
        } else if (manager.isSignComp(line, MessageManager.LangPath.SIGN_LINE_SUPPLY_SIGN)) {
            return AccessType.SUPPLY;
        } else {
            return null;
        }
    }

    /**
     * will start an update process, if the sign is a legacy sign
     */
    public static @Nullable AccessType getAccessType(@NotNull Sign sign, boolean ignoreCache) {
        if (!ignoreCache && Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            return Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(sign.getLocation()).getAccessType();
        } else {
            String accessTypeStr = sign.getPersistentDataContainer().get(accessTypeKey, PersistentDataType.STRING);

            AccessType accessType;
            if (accessTypeStr == null) {
                return null;
            } else {
                accessType = MiscUtils.getEnum(AccessType.class, accessTypeStr);

                if (accessType == null) {
                    accessType = AccessType.PRIVATE;
                }

            }

            return accessType;
        }
    }

    public enum AccessType {
        PRIVATE(false),
        PUBLIC(false), // everyone is member
        DONATION(true),
        DISPLAY(true),
        SUPPLY(true);

        private final boolean isInventoryHolderOnly;

        AccessType(boolean isInventoryHolderOnly) {
            this.isInventoryHolderOnly = isInventoryHolderOnly;
        }

        public boolean doesQualifyAs(@NotNull Block block) { //todo
            return !isInventoryHolderOnly || block.getState() instanceof InventoryHolder;
        }
    }
}
