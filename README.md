# PocketDice

Lightweight proximity dice rolls for **Paper/Purpur/Folia** and **Fabric/Quilt**. PocketDice **0.6.0** targets Minecraft **26.3**; PocketDice **0.5.0** targets Minecraft **26.2**.

## What it does
- `/roll [NdM]` - roll N dice with M faces. Examples: `/roll` (default `1d100`), `/roll 1d6`, `/roll 2d20`, `/roll d8` (one eight-sided die).
- `/groll` - global roll (visible to everyone).
- `/proll` - private roll (visible only to you).
- Announces proximity results only to players in the same world within `radius` blocks.
- **Anti-Spam Guard**: Configurable per-player cooldowns and sliding-window rate limits.
- **Multi-Platform**: Native support for Paper/Purpur/Folia and Fabric.
- **Fabric/Quilt**: The Fabric build is fully compatible with **Quilt** loader.
- **Localization**: Automatically picks the player's language if a translation exists.

## Commands & Permissions

| Command | Description | Permission | Default |
|---|---|---|---|
| `/roll [NdM]` | Proximity roll | `pocketdice.roll` | true |
| `/groll [NdM]` | Global roll | `pocketdice.groll` | true |
| `/proll [NdM]` | Private roll | `pocketdice.proll` | true |
| `/pd reload` | Reload configuration | `pocketdice.reload` | op |
| `/pd version` | Display version | `pocketdice.reload` | op |

### Bypass Permissions (Paper)
- `pocketdice.cooldown.bypass` - Bypass the rolling cooldown.
- `pocketdice.ratelimit.bypass` - Bypass the rolling rate limit.
- *Note: On Fabric, Operators (OP) bypass these by default.*

## Compatibility

| Platform | PocketDice 0.6.0 | PocketDice 0.5.0 |
|---|---|---|
| Paper/Purpur/Folia | Minecraft 26.3 | Minecraft 26.2 |
| Fabric/Quilt | Minecraft 26.3 | Minecraft 26.2 |
| Spigot/Bukkit | Not supported | Not supported |

Java 25 or newer is required.

## Installation

### Paper/Purpur/Folia

1. Use `PocketDice-0.6.0-Paper.jar` for Minecraft 26.3 or `PocketDice-0.5.0-Paper.jar` for Minecraft 26.2.
2. Place it in your server's `plugins/` folder.
3. Restart the server.

### Fabric/Quilt

1. Install [Fabric Loader](https://fabricmc.net/) 0.19.5 or newer and [Fabric API](https://modrinth.com/mod/fabric-api) matching your Minecraft version (built with 0.161.0+26.3 or 0.154.0+26.2).
2. Use `PocketDice-0.6.0-Fabric.jar` for Minecraft 26.3 or `PocketDice-0.5.0-Fabric.jar` for Minecraft 26.2.
3. Place it in your `mods/` folder.
4. Launch the server or world.

## Config (`config.yml` or `pocketdice.yml`)

```yml
config-version: 6
radius: 16
default_notation: "1d100"
max_dice: 50
max_faces: 1000

# Anti-Spam
cooldowns:
  enabled: true
  seconds: 3
rate_limit:
  enabled: true
  window_seconds: 60
  max_rolls: 12

sounds:
  roll:
    enabled: true
    sound_key: "minecraft:block.lodestone.place"
    volume: 0.7
    pitch: 1.2

updates:
  enabled: true
  check_on_startup: true
  check_interval_hours: 24
```

## Localization
- Locales are stored in the `locale/` directory.
- English (`en_US.yml`) is provided by default.
- To add a new language, copy `en_US.yml` and rename it to your locale code (e.g., `de_DE.yml`).

## Update Checking
- Uses the Modrinth API to check for updates.
- Can be disabled entirely via `updates.enabled: false`.

## Building

PocketDice uses Gradle and Java 25. To build the Fabric and Paper jars for Minecraft 26.3 (PocketDice 0.6.0):

```powershell
.\gradlew.bat build --console=plain
```

To build the Fabric and Paper jars for Minecraft 26.2 (PocketDice 0.5.0):

```powershell
.\gradlew.bat build '-Prelease_target=26.2' --console=plain
```

Each build generates platform metadata for its selected release and copies only that release's installable jars to `build/libs/`, preserving the other release's jars. Running both commands produces:

- `PocketDice-0.6.0-Paper.jar` and `PocketDice-0.6.0-Fabric.jar` - Minecraft 26.3.
- `PocketDice-0.5.0-Paper.jar` and `PocketDice-0.5.0-Fabric.jar` - Minecraft 26.2.

`clean` removes build outputs, so use it only before the first build if you want to keep both releases together.
