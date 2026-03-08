# PocketDice

Lightweight proximity dice rolls for **Paper/Folia** and **Fabric** (1.21.x).

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

- **Minecraft:** 1.21.x (Paper/Folia/Fabric)
- **Java:** 21+

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
