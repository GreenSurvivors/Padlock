package de.greensurvivors.padlock.impl.internal;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface InputAnvilMenu {
    /**
     * use {@link #getRenameChars()} instead!
     */
    @Deprecated
    @Contract(value = " -> null", pure = true)
    default @Nullable String getRenameText() {
        return null;
    }

    char @Nullable [] getRenameChars();
}
