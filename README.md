# Hexagon

Modern region protection for [Paper](https://papermc.io) 1.21.9+. A clean, typed alternative to
WorldGuard with a small public API, LuckPerms-aware roles, and commands built for speed.

- **Java 25**, Paper API only, no Bukkit legacy paths.
- **Immutable regions** stored one-per-file in YAML (`plugins/Hexagon/regions/<id>.yml`).
- **Typed flags**: `allow`/`deny` states, material and potion-effect rules with up to 64 targets,
  and MiniMessage texts for greetings and farewells.
- **Roles** instead of owners/members: `guest` (may enter), `member` (may build), `owner` (may
  manage). Roles are granted to players or to permission groups (`group:vip`).
- **MiniMessage first**, legacy `&` codes second. Every message lives in `messages.yml`.
- **WorldEdit selections**: no second wand to learn, no parallel selection state.
- Libraries are resolved at startup through Paper's library loader, so the jar stays small.

## Requirements

Paper 1.21.9 or newer, and **WorldEdit or FastAsyncWorldEdit**. Hexagon uses their selection for
every command that needs an area, so it refuses to load without one of them. LuckPerms is
optional: with it, roles can be granted to permission groups.

## Commands

`/hexagon` and `/hx` are interchangeable. Areas come from your WorldEdit selection, so mark one
with `//wand` (left click, right click) or `//pos1` and `//pos2` before creating or redefining a
region.

| Command | Purpose |
| --- | --- |
| `/hx help` (or plain `/hx`) | What every subcommand does. |
| `/hx create <id>` (`define`) | Create a region from your WorldEdit selection. |
| `/hx delete <id>` | Delete a region. |
| `/hx resize <id>` (`redefine`) | Replace the bounds with your current selection. |
| `/hx select <id>` | Load a region's bounds back into your WorldEdit selection. |
| `/hx priority <id> <priority>` | Change the priority; on overlap the higher number decides. New regions start at 0. |
| `/hx flag <id> <flag> [value]` | Show or set a flag; tab completion offers the values that flag accepts. |
| `/hx unflag <id> <flag>` | Clear a flag. |
| `/hx flags` | List every known flag and its scope. |
| `/hx gamerule <id> <rule> [value]` | Show or set a vanilla game rule for the region. |
| `/hx ungamerule <id> <rule>` | Let the region follow the world's game rule again. |
| `/hx gamerules` | List the game rules that can be set per region. |
| `/hx trust <id> <player\|group> [guest\|member\|owner]` | Grant a role (defaults to `member`). |
| `/hx untrust <id> <player\|group>` | Revoke a role. |
| `/hx info [id]` | Details of a region, or of the region you stand in. |
| `/hx list [world] [page]` | List regions. |
| `/hx teleport <id>` (`tp`) | Teleport on top of a region's centre. |
| `/hx reload` | Reload `config.yml` and `messages.yml`. |

### Flag values

```
/hx flag spawn pvp deny
/hx flag spawn block-place deny             # nobody but members places blocks
/hx flag spawn block-place allow torch      # non-members may place only torches
/hx flag spawn block-break deny tnt,lava    # non-members may break everything except TNT and lava
/hx flag arena potion-effects deny strength,speed
/hx flag spawn greeting <green>Welcome to <region>, <player>!
```

Rules read as `<allow|deny> [targets]`: without targets the state applies to everything; with
targets it applies to the listed elements and the opposite applies to the rest.

## Game rules

A region can override the vanilla game rules that Hexagon can enforce inside a cuboid:

```
/hx gamerule spawn keepInventory true
/hx gamerule arena naturalRegeneration false
/hx ungamerule spawn keepInventory
```

Hexagon supports every vanilla rule it can actually enforce inside a cuboid:

| Area | Rules |
| --- | --- |
| Death and progress | `keepInventory`, `showDeathMessages`, `announceAdvancements` |
| Damage | `fallDamage`, `fireDamage`, `drowningDamage`, `freezeDamage`, `naturalRegeneration` |
| Blocks | `mobGriefing`, `projectilesCanBreakBlocks`, `doFireTick`, `doVinesSpread`, `doTileDrops` |
| Spawning | `doMobSpawning`, `spawnMonsters`, `doPatrolSpawning`, `doTraderSpawning`, `doInsomnia`, `doWardenSpawning`, `spawnerBlocksEnabled`, `disableRaids` |
| Explosions and loot | `tntExplodes`, `tntExplosionDropDecay`, `mobExplosionDropDecay`, `blockExplosionDropDecay`, `doMobLoot` |
| Travel | `allowEnteringNetherUsingPortals` |

The rest of the vanilla rules are missing for a reason, and the command says which reason applies.
`doDaylightCycle`, `randomTickSpeed` and their kind are answered by the server for a whole world,
so no plugin can scope them to a cuboid. `pvp` is covered by Hexagon's own `pvp` flag, which adds
roles and the bypass permission on top.

Game rules are stored as ordinary flags, so they share priority resolution, storage and the API
with every other flag, and `/hx info` shows them in kebab case as `keep-inventory`.

## Flags

| Flag | Type | Applies to |
| --- | --- | --- |
| `block-break`, `block-place` | material rule | non-members |
| `interact` | state | non-members |
| `entry`, `exit` | state | outsiders (players without any role) |
| `pvp`, `damage`, `elytra`, `totem`, `explosions` | state | everyone |
| `potion-effects` | potion effect rule | everyone |
| `greeting`, `farewell` | MiniMessage text | everyone |

Resolution: among the regions covering a block, ordered by priority, the first one that sets the
flag decides. A player's role in *that* region is checked against the flag's scope. Players with
`hexagon.bypass` ignore every flag.

## Permissions

| Permission | Grants |
| --- | --- |
| `hexagon.manage` | Create regions and manage any region. Owners manage their own regions without it. |
| `hexagon.view` | `/hx info`, `/hx list`, `/hx flags`. |
| `hexagon.teleport` | `/hx teleport`. |
| `hexagon.admin` | `/hx reload`. |
| `hexagon.bypass` | Ignore all protection. |

## API

Add the API module as a compile-only dependency and declare Hexagon in `paper-plugin.yml`.

```kotlin
compileOnly("io.github.pawelusze:hexagon-api:1.0.0")
```

```java
HexagonApi hexagon = Hexagon.api();

// Ask questions
List<Region> here = hexagon.query().at(player.getLocation());
boolean canPvp = hexagon.query().allows(player.getLocation(), Flags.PVP, player);

// Define your own flag and register it while enabling
Flag<State> FLY = Flag.state("fly", FlagScope.NON_MEMBERS);
hexagon.flags().register(FLY);

// Change a region: values are immutable, so update explicitly
hexagon.regions().find(RegionId.of("spawn"))
        .map(region -> region.withFlag(FLY, State.DENY))
        .ifPresent(hexagon.regions()::update);
```

Events: `RegionCreatedEvent`, `RegionUpdatedEvent`, `RegionDeletedEvent`, and the cancellable
`RegionEnterEvent` / `RegionLeaveEvent`.

Register custom flags in `onEnable`; Hexagon loads regions after every plugin has enabled, so
flags registered there are always known when files are read.

## Opening in IntelliJ IDEA

`File > Open` the project folder and pick the Gradle build when IDEA asks. Everything else is
already configured:

- **Gradle JVM**: any JDK 17+ runs the build; Gradle downloads the Java 25 toolchain it compiles
  with, so no manual SDK setup is needed.
- **Run configurations** (shared through `.run/`, they appear in the run widget after import):
  `Run Paper Server`, `Build`, `Tests`, `Format (Palantir)`.
- **Formatting**: the project ships an `.editorconfig`; run `Format (Palantir)` before committing,
  since the build fails on unformatted code.

`Run Paper Server` downloads Paper 1.21.9 on first use, installs the freshly built jar and starts
the server in `plugin/run/`; accept the EULA there once.

## Building

```bash
./gradlew build
```

The plugin jar is written to `plugin/build/libs/Hexagon-<version>.jar`. `./gradlew runServer`
starts a Paper 1.21.9 test server with the plugin and WorldEdit installed. Code is formatted with Palantir Java
Format; run `./gradlew spotlessApply` before committing.

## Project layout

```
api/     io.github.pawelusze.hexagon.api        public API (Javadoc lives here)
plugin/  io.github.pawelusze.hexagon            runtime, packaged by feature:
           command/             one class per command family, argument/ for the resolvers
           config/              Configurate-backed config.yml and messages.yml
           membership/          LuckPerms integration with a permission-node fallback
           protection/          event listeners enforcing flags
           region/              store, spatial index, resolver and YAML repository
           selection/           WorldEdit and FastAsyncWorldEdit selections
           text/                MiniMessage/legacy rendering
```

## License

MIT. See [LICENSE](LICENSE).
