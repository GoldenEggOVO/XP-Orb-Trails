# XP Orb Trails

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://github.com/GoldenEggOVO/XP-Orb-Trails/tree/26.2)
[![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20Forge%20%7C%20NeoForge-DBD0B4)](#requirements)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[Download](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases) ·
[Changelog](CHANGELOG.md) ·
[Report an issue](https://github.com/GoldenEggOVO/XP-Orb-Trails/issues) ·
[Contributing](CONTRIBUTING.md)

XP Orb Trails is a client-only mod for Minecraft 26.2, available for Fabric,
Forge, and NeoForge. It adds smooth,
colorful trails to moving experience orbs and an optional pickup flash. Shaders
and server-side installation are not required.

## Features

- Interpolated trails with adjustable lifetime, render range, and performance limits.
- Solid, gradient, and animated rainbow colors, with an in-game color picker.
- Separate head, middle, and tail widths, plus shape presets.
- Cross-section slider from a camera-facing ribbon (2 sides) to a round approximation (32 sides).
  New configurations default to 2 sides and a width of 0.1; existing settings are preserved.
- One effect-strength control, enhanced glow, and optional motion and position adjustments.
- Optional soft, star, and ring pickup flashes.
- Built-in presets, named custom profiles, and an animated preview screen.
- English and Simplified Chinese interfaces.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Java | 25 or newer |

Choose one loader:

| Loader | Tested version | Additional mods |
| --- | --- | --- |
| Fabric | Loader 0.19.5 | Fabric API 0.158.0+26.2; optional Mod Menu 20.0.1 |
| Forge | 65.1.3 | None |
| NeoForge | 26.2.0.88 | None |

## Installation

1. Choose the JAR matching both your loader and Minecraft version:
   `xp-orb-trails-<loader>-1.3.0+26.2.jar`.
2. Put that JAR in the client `mods` folder. Fabric also needs Fabric API.
3. Start Minecraft with the matching loader. The server does not need this mod.

Version 1.3.0 is an unreleased local test build. Published versions remain on the
[releases page](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases).
Install only one XP Orb Trails JAR per instance.

The mod follows Minecraft's selected language. English is used when a
translation is unavailable; English and Simplified Chinese are included.

## Configuration

Open the settings screen through Mod Menu's Config button (Fabric), the loader's
Mods configuration button (Forge/NeoForge), or bind
**Open Trail Settings** in Minecraft's Controls screen. Changes apply
immediately and are saved to `config/xp-orb-trails.json` when the screen closes.
Settings are grouped into **Common**, **Appearance**, **Advanced**, and **Profiles**.
Common contains overall effects and trail width and timing. Appearance contains
colors, shape, and pickup flash controls. Advanced contains motion, position,
and performance settings. Profiles contains built-in and saved configurations.
Lists support scrolling and keyboard navigation; detailed shape, flash, and position
controls expand only when needed. Wide windows show a live preview beside the settings.
Each editable settings page has its own reset button, and hovering over a control
shows a short description.

Built-in appearance presets preserve the enabled state, render range, and trail limit.
Saved custom profiles restore the complete configuration. Saved color pairs can be
renamed, overwritten with the current colors, or deleted, including when all 12 slots
are full. The preview's replay button starts the pickup flash immediately.
The 2D preview includes a cross-section diagram and uses the configured pickup
flash duration. Check the trail's full 3D appearance in-game.

**Effect Strength** replaces the separate opacity and glow-strength sliders.
Flash size is independent of trail width. Existing configuration files and saved
profiles migrate automatically while preserving their previous strength and flash size.

If the configuration file is unreadable, the mod attempts to preserve it as
`xp-orb-trails.json.broken` (or a numbered variant), restores defaults, and
continues loading.
Non-finite numeric values in settings and saved profiles are repaired before
saving, while other settings and profile names are retained.

## Building from source

Use Java 25 and the included Gradle wrapper:

```sh
./gradlew build
```

On Windows, run `./gradlew.bat build` instead. The build runs the configuration
tests and checks all three loader JARs for settings UI, mixins, translations,
and platform metadata. Install the matching JAR from `<loader>/build/libs/`;
the `-sources.jar` file is for source inspection.

To build one loader, run `./gradlew -Ploaders=fabric build` (or `forge` or
`neoforge`). Shared code is in `common/`; loader entry points and build settings
are in `fabric/`, `forge/`, and `neoforge/`.

## Minecraft versions and branches

Each supported Minecraft version has its own branch and release. Use the JAR
matching your Minecraft version.

| Minecraft | Source branch | Release |
| --- | --- | --- |
| 26.3 | [`26.3`](https://github.com/GoldenEggOVO/XP-Orb-Trails/tree/26.3) (default) | [`fabric-1.2.0+26.3`](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases/tag/fabric-1.2.0+26.3) |
| 26.2 | [`26.2`](https://github.com/GoldenEggOVO/XP-Orb-Trails/tree/26.2) | [`fabric-1.2.0+26.2`](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases/tag/fabric-1.2.0+26.2) |

See [CONTRIBUTING.md](CONTRIBUTING.md) for the source layout, validation, and
release process. This repository does not use GitHub Actions workflows;
builds and release verification are run locally.

## License

MIT License. See [LICENSE](LICENSE) for the copyright notice and terms.

This is an independent implementation and contains no Shine source code or
assets.
