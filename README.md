# PocketDice

Lightweight proximity dice rolls for **Paper/Folia 1.21.x**.

## What it does
- `/roll [NdM]` - roll N dice with M faces. Examples: `/roll` (default `1d100`), `/roll 1d6`, `/roll 2d20`, `/roll d8` (if shorthand enabled).
- Announces the result only to players in the same world within `radius` blocks.
- Shows player name, notation (e.g., `2d20`), individual rolls (e.g., `[7, 13]`), and total (e.g., `20`).
- Permissions: `pocketdice.roll` (true), `pocketdice.reload` (op), `pocketdice.update.notify` (op).
- Admin: `/pocketdice reload` - reloads config & messages/locales.

## Localization / Translations
- Default locale: `plugins/PocketDice/locale/en_US.yml` (copied from the JAR on first run).
- Add translations by copying `en_US.yml` to a new file (e.g., `tr_TR.yml`) in `plugins/PocketDice/locale/`, then translating values.
- The plugin automatically picks the player's Minecraft language (e.g., `tr_TR`) if a matching file exists; otherwise it falls back to `en_US`.
- Reload locales with `/pocketdice reload` after editing/adding locale files.

## Compatibility

### Minecraft versions
| MC Version | Status | Notes |
|---|---|---|
| 1.21.x | Supported | Built against the 1.21 API (`api-version: "1.21"`). |
| < 1.21 | Not supported | Requires MC 1.21.x. |

### Server software
| Server | 1.21.x | Notes |
|---|---|---|
| **Paper** | Supported | Supported and tested. |
| **Folia** | Supported | Declared folia-supported; main thread logic only. |
| **Spigot** | Not supported | Targeted at Paper/Purpur/Folia only. |
| **Fabric / Forge / NeoForge / Quilt** | Not applicable | These are mod loaders, not Bukkit/Paper. |

**Java:** Use Java 21 (required for MC 1.21.x servers).

## Config (`plugins/PocketDice/config.yml`)
```yml
config-version: 5
radius: 16
default_notation: "1d100"
max_dice: 50
max_faces: 1000
allow_shorthand: true
updates:
  enabled: true
  check_on_startup: true
  check_interval_hours: 24             # 0 or negative = startup-only
  notify_console: true
  notify_admins_on_join: true
sounds:
  roll:
    enabled: true
    sound_key: "minecraft:block.stem.step"
    volume: 0.7
    pitch: 1.2
```

## Update checking
- Uses the Modrinth API to look up the latest PocketDice release and compare it to the running version.
- Checks on startup and, optionally, every `check_interval_hours`; both are controlled under the `updates` block.
- Set `updates.enabled: false` to disable all HTTP calls.
- Console messages and in-game admin notifications are localized via `locale/en_US.yml` (`messages.update.*`).
- Permission for in-game notifications: `pocketdice.update.notify` (default: op).

## Sounds
- A roll sound can be played for the roller after a successful roll; configure under `sounds.roll` in `config.yml`.
- To disable, set `sounds.roll.enabled: false`. Invalid sound keys are logged once and sound is skipped for the session.
