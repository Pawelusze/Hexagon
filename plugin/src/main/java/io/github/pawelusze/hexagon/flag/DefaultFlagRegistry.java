package io.github.pawelusze.hexagon.flag;

import io.github.pawelusze.hexagon.api.flag.Flag;
import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import org.jetbrains.annotations.NotNull;

/** Thread-safe registry keyed by flag name. */
public final class DefaultFlagRegistry implements FlagRegistry {

    private final ConcurrentSkipListMap<String, Flag<?>> flags = new ConcurrentSkipListMap<>();

    @Override
    public void register(@NotNull Flag<?> flag) {
        Flag<?> existing = flags.putIfAbsent(flag.name(), flag);
        if (existing != null) {
            throw new IllegalArgumentException("Flag '" + flag.name() + "' is already registered");
        }
    }

    @Override
    public @NotNull Optional<Flag<?>> find(@NotNull String name) {
        return Optional.ofNullable(flags.get(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    public @NotNull Collection<Flag<?>> all() {
        return Collections.unmodifiableCollection(flags.values());
    }
}
