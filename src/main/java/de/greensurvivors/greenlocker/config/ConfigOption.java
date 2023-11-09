package de.greensurvivors.greenlocker.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * represents a configurable option, with its path pointing to
 * please note: While fallback values are defined here, these are in fact NOT the default options.
 * They are just used in the unfortunate case loading them goes wrong.
 *
 * @param <T> Type of the config value
 */
public class ConfigOption<T> {
    private final @NotNull String path;
    private final @NotNull T fallbackValue;
    private @Nullable T value = null;

    protected ConfigOption(@NotNull String path, @NotNull T fallbackValue) {
        this.path = path;
        this.fallbackValue = fallbackValue;
    }

    protected @NotNull String getPath() {
        return path;
    }

    protected @NotNull T getFallbackValue() {
        return fallbackValue;
    }

    /**
     * returns the value if not null, else the fallback
     */
    public @NotNull T getValueOrFallback() {
        return Objects.requireNonNullElse(this.value, fallbackValue);
    }

    protected void setValue(@NotNull T value) {
        this.value = value;
    }
}
