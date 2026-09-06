<h1 align="center">Hexagon</h1>

<p align="center">
  <b>Region protection for Paper, built for servers that outgrew guesswork.</b><br>
  Typed flags, roles instead of owner lists, WorldEdit selections, and an API you can build on.
</p>

<p align="center">
  <img alt="Paper 1.21.9+" src="https://img.shields.io/badge/Paper-1.21.9%2B-b80c00?style=for-the-badge&logo=papermc&logoColor=white">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-b80c00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="MIT" src="https://img.shields.io/badge/License-MIT-b80c00?style=for-the-badge">
</p>

## 🚀 Get going

Drop the jar in `plugins/`, start the server, protect your spawn in four lines:

```
//wand                              # left click one corner, right click the other
/hx create spawn                    # your selection is now a region
/hx flag spawn pvp deny             # no fighting here
/hx trust spawn Notch owner         # someone to look after it
```

WorldEdit or FastAsyncWorldEdit is required — that is where the selection comes from. LuckPerms is
optional and lets you trust whole permission groups: `/hx trust spawn group:vip member`.

## 📋 Commands

`/hexagon` and `/hx` are the same command, and `/hx help` explains all of it in game. Mark an area
with `//wand` first.

| Command | |
| --- | --- |
| `/hx create <name>` · `resize` · `delete` | A region from your selection, moved, removed. |
| `/hx info [region]` · `list [world] [page]` | What a region holds, what a world holds. |
| `/hx select <region>` · `teleport <region>` | Back into your selection, or go there. |
| `/hx trust <region> <player\|group> [role]` · `untrust` | `guest`, `member` (default) or `owner`. |
| `/hx flag <region> <flag> [value]` · `unflag` · `flags` | Set, clear, list. |
| `/hx gamerule <region> <rule> [value]` · `ungamerule` · `gamerules` | Vanilla rules, per region. |
| `/hx priority <region> <number>` | Where regions overlap, the higher number decides. |
| `/hx reload` | Reread `config.yml` and `messages.yml`. |

## 🚩 Flags

| Flag | Type | Applies to |
| --- | --- | --- |
| `block-break`, `block-place` | material rule | non-members |
| `interact` | `allow` / `deny` | non-members |
| `entry`, `exit` | `allow` / `deny` | players without any role |
| `pvp`, `damage`, `elytra`, `totem`, `explosions` | `allow` / `deny` | everyone |
| `potion-effects` | potion effect rule | everyone |
| `greeting`, `farewell` | MiniMessage text | everyone |

A rule is `<allow|deny> [targets]`: without targets it covers everything, with targets it covers
those and the opposite covers the rest, up to 64 of them.

```
/hx flag spawn block-place allow torch      # non-members may place only torches
/hx flag spawn block-break deny tnt,lava    # non-members may break anything but TNT and lava
```

Among the regions covering a block, ordered by priority, the first one that sets a flag decides,
and the player's role *in that region* is checked against the flag's scope.

Game rules are ordinary flags, so they share priority, storage and the API. `/hx gamerules` lists
the ones that can be scoped to a cuboid; the command explains why the others cannot.

## 🔑 Permissions

`hexagon.view` for reading, `hexagon.manage` for any region (owners manage their own without it),
`hexagon.teleport`, `hexagon.admin` for reload, `hexagon.bypass` to ignore all protection.

## ⚡ Performance

Every block a player breaks, places, walks over or hits asks one question: which regions cover it?
[`benchmark/`](benchmark) puts that question to Hexagon and to both of WorldGuard's indexes, same
regions, same blocks, one JVM. Apple M2, JDK 25, WorldGuard 7.0.14, 2 forks × 5 × 2 s:

| Regions | Hexagon | WorldGuard, chunk cache | WorldGuard, R-tree |
| --- | --- | --- | --- |
| 100 | **15 ns** | 30 ns | 129 ns |
| 10 000 | **20 ns** | 27 ns | 1 218 ns |
| 50 000 | **47 ns** | 58 ns | 2 365 ns |

Hexagon keeps its chunks in a table with primitive `long` keys and answers without locks, a cache
or a background thread. WorldGuard's R-tree is what answers any chunk it has not cached yet; its
chunk cache, once warm, comes within a few nanoseconds. Deciding a single flag is cheaper still,
because the walk stops at the first region that sets it: 9 ns at 100 regions, 16 ns at 10 000,
29 ns at 50 000.

```bash
./gradlew :hexagon-benchmark:jmh
```

## 🔌 API

```kotlin
compileOnly("io.github.pawelusze:hexagon-api:1.0.0")
```

```java
HexagonApi hexagon = Hexagon.api();
boolean canPvp = hexagon.query().allows(player.getLocation(), Flags.PVP, player);

Flag<State> FLY = Flag.state("fly", FlagScope.NON_MEMBERS);
hexagon.flags().register(FLY);           // in onEnable; regions are read after every plugin enables

hexagon.regions().find(RegionId.of("spawn"))
        .map(region -> region.withFlag(FLY, State.DENY))
        .ifPresent(hexagon.regions()::update);
```

Events: `RegionCreatedEvent`, `RegionUpdatedEvent`, `RegionDeletedEvent`, and the cancellable
`RegionEnterEvent` and `RegionLeaveEvent`.

## 🛠 Building

```bash
./gradlew build            # jar in plugin/build/libs/
./gradlew runServer        # Paper 1.21.9 with the plugin and WorldEdit
./gradlew spotlessApply    # Palantir format; the build fails on unformatted code
```

MIT licensed. See [LICENSE](LICENSE).
