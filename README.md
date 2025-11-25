# PocketDice

Lightweight proximity dice rolls for **Paper/Purpur 1.21.x**.

## What it does
- `/roll [NdM]` — roll N dice with M faces. Examples: `/roll` (default `1d100`), `/roll 1d6`, `/roll 2d20`, `/roll d8` (if shorthand enabled).
- Announces the result only to players in the same world within `radius` blocks.
- Shows player name, notation (e.g., `2d20`), individual rolls (e.g., `[7, 13]`), and total (e.g., `20`).
- Permissions: `pocketdice.roll` (true), `pocketdice.reload` (op), `pocketdice.update.notify` (op).
- Admin: `/pocketdice reload` — reloads config & messages.

## Localization / Translations
- Default locale: `plugins/PocketDice/locale/en_US.yml` (copied from the JAR on first run).
- Add translations by copying `en_US.yml` to a new file (e.g., `tr_TR.yml`) in `plugins/PocketDice/locale/`, then translating values.
- The plugin automatically picks the player’s Minecraft language (e.g., `tr_TR`) if a matching file exists; otherwise it falls back to `en_US`.
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
| **Purpur** | Supported | Primary target; tested. |
| **Paper** | Supported | Supported; no Paper-only APIs used. |
| **Spigot** | Supported | Supported via Bukkit API. |
| **Folia** | Untested | Should work if everything runs on the main thread. |
| **Fabric / Forge / NeoForge / Quilt** | Not applicable | These are mod loaders, not Bukkit/Paper. |

**Java:** Use Java 21 (required for MC 1.21.x servers).

## Config (`plugins/PocketDice/config.yml`)
```yml
config-version: 2
radius: 16
default_notation: "1d100"
max_dice: 50
max_faces: 1000
message_format: "[PocketDice] {player} rolled {notation}: {results} (total {total})"
error_format: "[PocketDice] {message}"
allow_shorthand_d: true
updates:
  enabled: true
  modrinth_project_slug: "pocketdice"  # Modrinth project slug (or ID)
  check_on_startup: true
  check_interval_hours: 24             # 0 or negative = startup-only
  notify_console: true
  notify_admins_on_join: true
  admin_notify_permission: "pocketdice.update.notify"
  messages:
    up_to_date_console: "[PocketDice] You are running the latest version: {current}."
    update_available_console: "[PocketDice] A new version is available: {latest} (current: {current}). Download: {url}"
    update_available_admin: "<yellow>[PocketDice]</yellow> <gray>New version available:</gray> <gold>{latest}</gold> <gray>(current:</gray> <gold>{current}</gold><gray>)</gray>"
```

## Update checking
- Uses the Modrinth API to look up the latest PocketDice release and compare it to the running version.
- Checks on startup and, optionally, every `check_interval_hours`; both are controlled under the `updates` block.
- Set `updates.enabled: false` to disable all HTTP calls.
- Console messages and in-game admin notifications use the `updates.messages.*` templates.
- Permission for in-game notifications: `pocketdice.update.notify` (default: op).
