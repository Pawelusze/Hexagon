# Hexagon

Region protection for [Paper](https://papermc.io) 1.21.9+, on Java 25. Typed flags, roles instead
of owner lists, WorldEdit selections, and a public API other plugins can build on.

## Requirements

Paper 1.21.9 or newer and **WorldEdit or FastAsyncWorldEdit** — every command that needs an area
reads your WorldEdit selection, so Hexagon refuses to load without one of them. LuckPerms is
optional and lets roles be granted to permission groups.

## Install

Drop `Hexagon-<version>.jar` into `plugins/` and start the server. Configuration lives in
`plugins/Hexagon/config.yml` and `messages.yml`, regions in `plugins/Hexagon/regions/<id>.yml`,
one file each.

## Commands

`/hexagon` and `/hx` are the same command. Mark an area with `//wand` first, then:

| Command | Purpose |
| --- | --- |
| `/hx help` | What every subcommand does. Plain `/hx` shows it too. |
| `/hx create <name>` | Turn your selection into a region. |
| `/hx resize <region>` | Move a region onto your current selection. |
| `/hx delete <region>` | Delete a region. |
| `/hx info [region]` | Size, roles and flags, or of the region you stand in. |
| `/hx list [world] [page]` | List regions. |
| `/hx select <region>` | Load a region back into your WorldEdit selection. |
| `/hx teleport <region>` | Teleport on top of a region. |
| `/hx trust <region> <player\|group> [role]` | Grant `guest`, `member` (default) or `owner`. |
| `/hx untrust <region> <player\|group>` | Revoke a role. |
| `/hx flag <region> <flag> [value]` | Show or set a flag; tab completion offers its values. |
| `/hx unflag <region> <flag>` | Clear a flag. |
| `/hx flags` | Every flag, what it accepts, and who it applies to. |
| `/hx gamerule <region> <rule> [true\|false]` | Set a vanilla game rule inside the region. |
| `/hx ungamerule <region> <rule>` | Follow the world's game rule again. |
| `/hx gamerules` | The game rules that work per region. |
| `/hx priority <region> <number>` | Where regions overlap, the higher number decides. |
| `/hx reload` | Reread `config.yml` and `messages.yml`. |

`create` also answers to `define`, and `resize` to `redefine`.

## Flags

| Flag | Type | Applies to |
| --- | --- | --- |
| `block-break`, `block-place` | material rule | non-members |
| `interact` | `allow` / `deny` | non-members |
| `entry`, `exit` | `allow` / `deny` | players without any role |
| `pvp`, `damage`, `elytra`, `totem`, `explosions` | `allow` / `deny` | everyone |
| `potion-effects` | potion effect rule | everyone |
| `greeting`, `farewell` | MiniMessage text | everyone |

A rule reads as `<allow\|deny> [targets]`. Without targets the state covers everything; with
targets it covers the listed elements and the opposite covers the rest, up to 64 targets.

```
/hx flag spawn pvp deny
/hx flag spawn block-place allow torch        # non-members may place only torches
/hx flag spawn block-break deny tnt,lava      # non-members may break anything but TNT and lava
/hx flag arena potion-effects deny strength,speed
/hx flag spawn greeting <green>Welcome to <region>, <player>!
```

Among the regions covering a block, ordered by priority, the first one that sets a flag decides,
and the player's role *in that region* is checked against the flag's scope. `hexagon.bypass`
ignores every flag.

## Game rules

Vanilla game rules a region can override, stored as ordinary flags and shown by `/hx info` in
kebab case (`keep-inventory`):

| Area | Rules |
| --- | --- |
| Death and progress | `keepInventory`, `showDeathMessages`, `announceAdvancements` |
| Damage | `fallDamage`, `fireDamage`, `drowningDamage`, `freezeDamage`, `naturalRegeneration` |
| Blocks | `mobGriefing`, `projectilesCanBreakBlocks`, `doFireTick`, `doVinesSpread`, `doTileDrops` |
| Spawning | `doMobSpawning`, `spawnMonsters`, `doPatrolSpawning`, `doTraderSpawning`, `doInsomnia`, `doWardenSpawning`, `spawnerBlocksEnabled`, `disableRaids` |
| Explosions and loot | `tntExplodes`, `tntExplosionDropDecay`, `mobExplosionDropDecay`, `blockExplosionDropDecay`, `doMobLoot` |
| Travel | `allowEnteringNetherUsingPortals` |

The rest are absent on purpose, and the command says why: rules like `doDaylightCycle` are
answered by the server for a whole world, and `pvp` is covered by the `pvp` flag, which adds roles
and the bypass permission on top.

## Permissions

| Permission | Grants |
| --- | --- |
| `hexagon.view` | `/hx info`, `/hx list`, `/hx flags`, `/hx help`. |
| `hexagon.manage` | Create and manage any region. Owners manage their own without it. |
| `hexagon.teleport` | `/hx teleport`. |
| `hexagon.admin` | `/hx reload`. |
| `hexagon.bypass` | Ignore all protection. |

## Performance

Protection costs one question, asked for every block a player breaks, places, walks over or hits:
*which regions cover this block?* [`benchmark/`](benchmark) puts that question to Hexagon and to
both of WorldGuard's own indexes, with the same regions and the same blocks in the same JVM.

Average time per lookup. Apple M2, JDK 25, WorldGuard 7.0.14, 2 forks × 5 × 2 s:

| Regions in the world | Hexagon | WorldGuard, chunk cache | WorldGuard, R-tree |
| --- | --- | --- | --- |
| 100 | **20 ns** | 25 ns | 104 ns |
| 10 000 | 103 ns | **28 ns** | 921 ns |
| 50 000 | 131 ns | **57 ns** | 2 549 ns |

Read honestly: Hexagon beats WorldGuard's R-tree by 5× to 19×, and that R-tree is what answers
every block in a chunk WorldGuard has not cached. Once WorldGuard has cached a chunk — it does so
on chunk load, on a background thread — its table answers faster than Hexagon, roughly 2× from
10 000 regions up. Hexagon reaches its numbers with no cache, no background thread, and no memory
that grows with the number of loaded chunks.

Deciding a flag is cheaper still, because Hexagon stops at the first region that sets it instead
of collecting them all: 24 ns at 100 regions, 91 ns at 10 000, 105 ns at 50 000.

Run it yourself:

```bash
./gradlew :hexagon-benchmark:jmh
```

Results land in `benchmark/build/results/jmh/results.txt`. The numbers above come from that file;
region counts, world size and iteration counts live in
[`RegionLookupBenchmark`](benchmark/src/jmh/java/io/github/pawelusze/hexagon/region/RegionLookupBenchmark.java)
and [`benchmark/build.gradle.kts`](benchmark/build.gradle.kts).

## API

```kotlin
compileOnly("io.github.pawelusze:hexagon-api:1.0.0")
```

```java
HexagonApi hexagon = Hexagon.api();

List<Region> here = hexagon.query().at(player.getLocation());
boolean canPvp = hexagon.query().allows(player.getLocation(), Flags.PVP, player);

// Your own flag, registered while your plugin enables
Flag<State> FLY = Flag.state("fly", FlagScope.NON_MEMBERS);
hexagon.flags().register(FLY);

// Regions are immutable, so changes are explicit
hexagon.regions().find(RegionId.of("spawn"))
        .map(region -> region.withFlag(FLY, State.DENY))
        .ifPresent(hexagon.regions()::update);
```

Events: `RegionCreatedEvent`, `RegionUpdatedEvent`, `RegionDeletedEvent`, and the cancellable
`RegionEnterEvent` and `RegionLeaveEvent`. Hexagon reads region files after every plugin has
enabled, so flags you register in `onEnable` are known by then.

## Building

```bash
./gradlew build            # jar in plugin/build/libs/
./gradlew runServer        # Paper 1.21.9 with the plugin and WorldEdit
./gradlew spotlessApply    # Palantir format; the build fails on unformatted code
```

Open the folder in IntelliJ IDEA and pick the Gradle build; the run configurations in `.run/`
show up in the run widget. Gradle downloads the Java 25 toolchain itself.

## License

MIT. See [LICENSE](LICENSE).
