package io.github.pawelusze.hexagon.api.flag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Immutable, type-safe collection of flag values. */
public final class FlagMap {

    private static final FlagMap EMPTY = new FlagMap(Map.of());

    private final Map<Flag<?>, Object> values;

    private FlagMap(Map<Flag<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    /**
     * Returns the map with no flags set.
     *
     * @return the empty map
     */
    public static @NotNull FlagMap empty() {
        return EMPTY;
    }

    /**
     * Returns the value of a flag.
     *
     * @param flag the flag
     * @param <T> the value type
     * @return the value, or empty if the flag is not set
     */
    public <T> @NotNull Optional<T> get(@NotNull Flag<T> flag) {
        return Optional.ofNullable(cast(this.values.get(flag)));
    }

    /**
     * Returns the textual form of a flag's value, as produced by its {@link FlagType}.
     *
     * @param flag the flag
     * @return the serialized value, or empty if the flag is not set
     */
    public @NotNull Optional<String> serialized(@NotNull Flag<?> flag) {
        return Optional.ofNullable(serialize(flag, this.values.get(flag)));
    }

    /**
     * Tells whether a flag is set.
     *
     * @param flag the flag
     * @return true if the flag has a value
     */
    public boolean contains(@NotNull Flag<?> flag) {
        return this.values.containsKey(flag);
    }

    /**
     * Returns a copy with a flag set, replacing any previous value.
     *
     * @param flag the flag
     * @param value the value
     * @param <T> the value type
     * @return the copy
     */
    public <T> @NotNull FlagMap with(@NotNull Flag<T> flag, @NotNull T value) {
        Map<Flag<?>, Object> updated = new HashMap<>(values);
        updated.put(flag, value);
        return new FlagMap(updated);
    }

    /**
     * Returns a copy with a flag cleared.
     *
     * @param flag the flag
     * @return the copy, or this map if the flag was not set
     */
    public @NotNull FlagMap without(@NotNull Flag<?> flag) {
        if (!this.values.containsKey(flag)) {
            return this;
        }
        Map<Flag<?>, Object> updated = new HashMap<>(values);
        updated.remove(flag);
        return new FlagMap(updated);
    }

    /**
     * Returns the flags that have a value.
     *
     * @return an unmodifiable set of flags
     */
    public @NotNull Set<Flag<?>> flags() {
        return this.values.keySet();
    }

    /**
     * Tells whether no flag is set.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    /**
     * Returns the number of flags set.
     *
     * @return the size
     */
    public int size() {
        return this.values.size();
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    private static <T> String serialize(Flag<T> flag, Object value) {
        if (value == null) {
            return null;
        }
        return flag.type().serialize(cast(value));
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof FlagMap that && this.values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return this.values.toString();
    }
}
