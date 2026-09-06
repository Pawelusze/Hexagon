package io.github.pawelusze.hexagon.api;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Static access point to the running Hexagon instance.
 *
 * <p>Hexagon registers its {@link HexagonApi} with the Bukkit services manager while enabling.
 * Plugins that hard-depend on Hexagon can call {@link #api()} directly; soft-dependents should use
 * {@link #find()} and handle the empty case.
 */
public final class Hexagon {

    private Hexagon() {}

    /**
     * Returns the running Hexagon API.
     *
     * @return the registered API instance
     * @throws IllegalStateException if Hexagon is not enabled
     */
    public static @NotNull HexagonApi api() {
        return find().orElseThrow(() -> new IllegalStateException(
                "Hexagon is not enabled. Declare it as a dependency in your paper-plugin.yml."));
    }

    /**
     * Looks up the running Hexagon API without failing when it is absent.
     *
     * @return the registered API instance, or empty if Hexagon is not enabled
     */
    public static @NotNull Optional<HexagonApi> find() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(HexagonApi.class));
    }
}
