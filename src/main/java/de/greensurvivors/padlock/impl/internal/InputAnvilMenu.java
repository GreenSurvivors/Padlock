package de.greensurvivors.padlock.impl.internal;

import org.jetbrains.annotations.Nullable;

public interface InputAnvilMenu {
    /**
     * use {@link #getRenameChars()} instead!
     */
    @Deprecated
    String getRenameText();

    char @Nullable [] getRenameChars();
}
