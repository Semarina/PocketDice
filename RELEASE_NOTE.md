# PocketDice 0.6.0 and 0.5.0

PocketDice 0.6.0 targets Minecraft 26.3. PocketDice 0.5.0 targets Minecraft 26.2. Both releases provide Fabric/Quilt mods and Paper/Purpur/Folia plugins and require Java 25.

## GitHub Release Changelogs

- [PocketDice v0.5.0](release-notes/0.5.0.md) - Minecraft 26.2.
- [PocketDice v0.6.0](release-notes/0.6.0.md) - Minecraft 26.3.

These changelogs follow the section format used by the previous GitHub releases.

## Changes

- Release 0.6.0 uses Fabric API 0.161.0+26.3 and Paper API 26.3.build.177-stable.
- Release 0.5.0 uses Fabric API 0.154.0+26.2 and Paper API 26.2.build.48-alpha.
- Both Fabric builds require Fabric Loader 0.19.5 or newer.
- Both releases include always-enabled `/roll d8` shorthand and the current configuration migration.
- Build profiles generate the correct version and Minecraft requirements in each jar.

## Modrinth Upload

Upload each artifact with the matching PocketDice version and Minecraft version:

| Artifact | Version number | Game version | Loaders |
|---|---|---|---|
| `PocketDice-0.6.0-Fabric.jar` | `0.6.0` | 26.3 | Fabric, Quilt |
| `PocketDice-0.6.0-Paper.jar` | `0.6.0` | 26.3 | Folia, Paper, Purpur |
| `PocketDice-0.5.0-Fabric.jar` | `0.5.0` | 26.2 | Fabric, Quilt |
| `PocketDice-0.5.0-Paper.jar` | `0.5.0` | 26.2 | Folia, Paper, Purpur |

Use release type `Release`.
