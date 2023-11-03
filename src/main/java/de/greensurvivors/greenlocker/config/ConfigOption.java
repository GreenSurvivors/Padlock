package de.greensurvivors.greenlocker.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigOption<T> {
    private final @NotNull String path;
    private final T fallbackValue;
    private @Nullable T value = null;

    protected ConfigOption(@NotNull String path, T fallbackValue) {
        this.path = path;
        this.fallbackValue = fallbackValue;
    }

    protected @NotNull String getPath() {
        return path;
    }

    protected @NotNull T getFallbackValue() {
        return fallbackValue;
    }

    public @NotNull T getValueOrFallback() {
        if (this.value == null) {
            return fallbackValue;
        } else {
            return value;
        }
    }

    protected void setValue(@NotNull T value) {
        this.value = value;
    }
}
