# PocketDice

Lightweight proximity dice rolls for **Paper/Purpur/Folia** and **Fabric/Quilt** (26.1.x).

## What it does
- `/roll [NdM]` - roll N dice with M faces. Examples: `/roll` (default `1d100`), `/roll 1d6`, `/roll 2d20`, `/roll d8`.
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

| Platform | Minecraft Version | Status |
|---|---|---|
| Paper | 26.1.x | Supported |
| Purpur | 26.1.x | Supported |
| Folia | 26.1.x | Supported |
| Fabric | 26.1.x | Supported |
| Quilt | 26.1.x | Supported |
| Spigot/Bukkit | - | Not supported |

Java 25 or newer is required.

## Installation

### Paper/Purpur/Folia

1. Download `PocketDice-0.4.0-Paper.jar` from [Modrinth](https://modrinth.com/plugin/pocketdice).
2. Place it in your server's `plugins/` folder.
3. Restart the server.

### Fabric/Quilt

1. Install [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download `PocketDice-0.4.0-Fabric.jar` from [Modrinth](https://modrinth.com/plugin/pocketdice).
3. Place it in your `mods/` folder.
4. Launch the server or world.

## Config (`config.yml` or `pocketdice.yml`)

```yml
config-version: 6
radius: 16
default_notation: "1d100"
max_dice: 50
max_faces: 1000
allow_shorthand: true

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

PocketDice uses Gradle. To build both release jars:

```powershell
.\gradlew.bat build
```

The release jars are copied to `build/libs/`:

- `PocketDice-0.4.0-Paper.jar` - For Paper/Purpur/Folia.
- `PocketDice-0.4.0-Fabric.jar` - For Fabric/Quilt.
