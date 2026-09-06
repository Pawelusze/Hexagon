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
[`benchmark/`](benchmark) is a plugin that asks Hexagon and WorldGuard that question **on the same
running Paper server**, with the same regions and the same blocks, and checks that both give the
same answer. Regions are spread so that a block sits in about two of them; queries land in loaded
chunks, where events happen and where WorldGuard has warmed its chunk cache, as in play.

Paper 1.21.9, Apple M2, JDK 25, WorldGuard 7.0.14, 100 000 queries per round, median of 5:

| Regions | Regions at a block | | PvP flag at a block | | Creating the regions | |
| --- | --- | --- | --- | --- | --- | --- |
| | **Hexagon** | WorldGuard | **Hexagon** | WorldGuard | **Hexagon** | WorldGuard |
| 100 | **67 ns** | 340 ns | **56 ns** | 666 ns | **14 ms** | 55 ms |
| 1 000 | **79 ns** | 331 ns | **56 ns** | 502 ns | **52 ms** | 1 045 ms |
| 10 000 | **78 ns** | 512 ns | **63 ns** | 412 ns | **191 ms** | 67 288 ms |

Hexagon keeps its chunks in a table with primitive `long` keys, reads it without locks, resolves a
world by id instead of asking Bukkit for its key (which allocates), and stops at the first region
that sets the flag. Creating a region through the API costs it a table insert; WorldGuard rebuilds
its cache for every loaded chunk the region touches.

Run it yourself — the server starts with both plugins, then type in its console:

```bash
./gradlew :hexagon-benchmark:runServer
hexbench 10000 100000
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
