package io.github.pawelusze.hexagon;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Lets Paper download Hexagon's libraries at startup instead of shading them into the jar, which
 * keeps the jar small and the libraries shared between plugins.
 *
 * <p>The coordinates live in {@code libraries.list}, written by Gradle from the very dependencies
 * the plugin compiles against, so the compiled and the downloaded versions cannot drift apart.
 */
@SuppressWarnings("UnstableApiUsage")
public final class HexagonLoader implements PluginLoader {

    private static final String LIBRARY_LIST = "/libraries.list";
    private static final String PANDA_REPOSITORY = "https://repo.panda-lang.org/releases";

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpath) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(repository("central", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR));
        resolver.addRepository(repository("panda", PANDA_REPOSITORY));
        for (String coordinates : readLibraryList()) {
            resolver.addDependency(new Dependency(new DefaultArtifact(coordinates), null));
        }
        classpath.addLibrary(resolver);
    }

    private static RemoteRepository repository(String name, String url) {
        return new RemoteRepository.Builder(name, "default", url).build();
    }

    private static List<String> readLibraryList() {
        try (InputStream stream = HexagonLoader.class.getResourceAsStream(LIBRARY_LIST)) {
            if (stream == null) {
                throw new IllegalStateException(LIBRARY_LIST + " is missing from the plugin jar");
            }
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + LIBRARY_LIST, exception);
        }
    }
}
