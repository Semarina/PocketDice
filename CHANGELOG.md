# Changelog

## 0.6.0
- Targets Minecraft 26.3 on Fabric/Quilt and Paper/Purpur/Folia.
- Compiles the plugin against Paper API 26.3.build.177-stable.
- Generates mod/plugin versions and Minecraft requirements from the selected Gradle release profile.
- Builds both Minecraft release lines in GitHub Actions.

## 0.5.0
- Targets Minecraft 26.2 on Fabric/Quilt and Paper/Purpur/Folia.
- Builds with `-Prelease_target=26.2`, Fabric API 0.154.0+26.2, and Paper API 26.2.build.48-alpha.
- Includes the current dice behavior, including always-enabled `d8` shorthand.
- Removes obsolete shorthand configuration keys and migrates Paper configuration to version 7.
- Filters Fabric-only Modrinth versions out of Paper update checks.

## 0.4.0
- Added support for Minecraft 26.1.x (26.1, 26.1.1, and 26.1.2).
- Updated the Fabric/Quilt build for the Fabric 26.1.2 toolchain, Fabric Loader 0.19.2, Fabric API 0.150.0+26.1.2, and Java 25.
- Updated the Paper/Purpur/Folia build to compile against the Paper 26.1.2 stable API.
- Updated platform metadata, GitHub Actions, and release documentation for the 26.1.x Modrinth release.

## 0.3.0
- Added Fabric/Quilt mod support alongside the Paper/Purpur/Folia plugin.
- Added Modrinth update checks and 1.21.11 compatibility.
