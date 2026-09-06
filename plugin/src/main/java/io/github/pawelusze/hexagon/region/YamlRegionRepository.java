package io.github.pawelusze.hexagon.region;

import io.github.pawelusze.hexagon.api.flag.FlagRegistry;
import io.github.pawelusze.hexagon.api.region.Region;
import io.github.pawelusze.hexagon.api.region.RegionId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Stores each region as {@code regions/<id>.yml}. Writes run on a single background thread in
 * submission order, so a save followed by a delete always ends with the file gone.
 *
 * <p>A save writes a temporary file and moves it over the old one, so a crash halfway through
 * leaves the previous version intact rather than half a region.
 */
public final class YamlRegionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(YamlRegionRepository.class);

    private static final String FILE_SUFFIX = ".yml";
    private static final String PARTIAL_SUFFIX = ".writing";
    private static final int SHUTDOWN_SECONDS = 10;

    private final Path directory;
    private final RegionCodec codec;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("hexagon-storage").daemon().factory());

    public YamlRegionRepository(@NotNull Path directory, @NotNull FlagRegistry flags) {
        this.directory = directory;
        this.codec = new RegionCodec(flags);
    }

    /**
     * Reads every region file.
     *
     * <p>The files are parsed in parallel because reading and parsing YAML is where the time goes,
     * and both are free of shared state. The caller still gets one finished list on its own thread,
     * so nothing is published to the server half-loaded.
     */
    public @NotNull List<Region> loadAll() {
        if (!Files.isDirectory(this.directory)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(this.directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(FILE_SUFFIX))
                    .sorted()
                    .parallel()
                    .map(this::load)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException exception) {
            LOG.error("Could not list region files in {}", this.directory, exception);
            return List.of();
        }
    }

    public void save(@NotNull Region region) {
        this.writer.execute(() -> this.write(region));
    }

    public void delete(@NotNull RegionId id) {
        this.writer.execute(() -> {
            try {
                Files.deleteIfExists(this.fileOf(id));
            } catch (IOException exception) {
                LOG.error("Could not delete region file for '{}'", id, exception);
            }
        });
    }

    /** Waits for queued writes so nothing is lost when the server stops. */
    public void close() {
        this.writer.shutdown();
        try {
            if (!this.writer.awaitTermination(SHUTDOWN_SECONDS, TimeUnit.SECONDS)) {
                LOG.warn("Region writes did not finish within {} seconds", SHUTDOWN_SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private Optional<Region> load(Path file) {
        try {
            return Optional.of(this.codec.decode(loader(file).load()));
        } catch (ConfigurateException | IllegalArgumentException exception) {
            LOG.error("Skipping malformed region file {}: {}", file.getFileName(), exception.getMessage());
            return Optional.empty();
        }
    }

    private void write(Region region) {
        Path target = this.fileOf(region.id());
        Path partial = target.resolveSibling(target.getFileName() + PARTIAL_SUFFIX);
        try {
            Files.createDirectories(this.directory);

            YamlConfigurationLoader loader = loader(partial);
            CommentedConfigurationNode node = loader.createNode();
            this.codec.encode(region, node);
            loader.save(node);

            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            LOG.error("Could not save region '{}'", region.id(), exception);
        }
    }

    private Path fileOf(RegionId id) {
        return this.directory.resolve(id.value() + FILE_SUFFIX);
    }

    private static YamlConfigurationLoader loader(Path file) {
        return YamlConfigurationLoader.builder()
                .path(file)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();
    }
}
