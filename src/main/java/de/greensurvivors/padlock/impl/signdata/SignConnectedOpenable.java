package de.greensurvivors.padlock.impl.signdata;

import de.greensurvivors.padlock.Padlock;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class SignConnectedOpenable {
    private final static NamespacedKey connectedOpenableKey = new NamespacedKey(Padlock.getPlugin(), "isConnectedOpenable");


    public static boolean isConnected(@NotNull Sign sign) {
        Boolean result = sign.getPersistentDataContainer().get(connectedOpenableKey, PersistentDataType.BOOLEAN);

        return result != null && result;
    }

    public static void setConnected(@NotNull Sign sign, boolean isConnected) {
        sign.getPersistentDataContainer().set(connectedOpenableKey, PersistentDataType.BOOLEAN, isConnected);
        sign.update();
    }
}
