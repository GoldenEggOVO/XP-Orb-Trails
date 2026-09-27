# XP Orb Trails

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://github.com/GoldenEggOVO/XP-Orb-Trails/tree/26.2)
[![Fabric](https://img.shields.io/badge/Mod%20loader-Fabric-DBD0B4)](https://fabricmc.net/use/installer/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[Download](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases) ·
[Changelog](CHANGELOG.md) ·
[Report an issue](https://github.com/GoldenEggOVO/XP-Orb-Trails/issues) ·
[Contributing](CONTRIBUTING.md)

XP Orb Trails is a client-only Fabric mod for Minecraft 26.2. It adds smooth,
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
| Fabric Loader | 0.19.5 or newer |
| Fabric API | 0.158.0+26.2 or newer for Minecraft 26.2 |
| Java | 25 or newer |
| Mod Menu | Optional; adds a Config button |

## Installation

1. Download `xp-orb-trails-1.0.2+mc26.2.jar` from the
   [releases page](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases/tag/v1.0.2).
2. Put the JAR and a compatible Fabric API JAR in the client `mods` folder.
3. Start Minecraft with Fabric Loader. The server does not need this mod.

The mod follows Minecraft's selected language. English is used when a
translation is unavailable; English and Simplified Chinese are included.

## Configuration

Open the settings screen through Mod Menu's Config button or bind
**Open Trail Settings** in Minecraft's Controls screen. Changes apply
immediately and are saved to `config/xp-orb-trails.json` when the screen closes.
Settings are grouped into **Common**, **Appearance**, **Advanced**, and **My Profiles**.
Lists support scrolling and keyboard navigation; detailed shape, flash, and position
controls expand only when needed. Wide windows show a live preview beside the settings.
Each editable settings page has its own reset button, and hovering over a control
shows a short description.

**Effect Strength** replaces the separate opacity and glow-strength sliders.
Flash size is independent of trail width. Existing configuration files and saved
profiles migrate automatically while preserving their previous strength and flash size.

If the configuration file is unreadable, the mod attempts to preserve it as
`xp-orb-trails.json.broken` (or a numbered variant), restores defaults, and
continues loading.

## Building from source

Use Java 25 and the included Gradle wrapper:

```sh
./gradlew build
```

On Windows, run `./gradlew.bat build` instead. The build runs the configuration
tests and checks that the release JAR contains the settings UI, Mod Menu
integration, and translations. Install
`build/libs/xp-orb-trails-1.0.2+mc26.2.jar`; the `-sources.jar` file is for source
inspection.

## Minecraft versions and branches

Each supported Minecraft version has its own branch and release. Use the JAR
matching your Minecraft version.

| Minecraft | Source branch | Release |
| --- | --- | --- |
| 26.3 | [`26.3`](https://github.com/GoldenEggOVO/XP-Orb-Trails/tree/26.3) (default) | [`v1.1.1 for 26.3`](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases/tag/v1.1.1) |
| 26.2 | [`26.2`](https://github.com/GoldenEggOVO/XP-Orb-Trails/tree/26.2) | [`v1.0.2 for 26.2`](https://github.com/GoldenEggOVO/XP-Orb-Trails/releases/tag/v1.0.2) |

See [CONTRIBUTING.md](CONTRIBUTING.md) for the source layout, validation, and
release process. This repository does not use GitHub Actions workflows;
builds and release verification are run locally.

## License

MIT License. See [LICENSE](LICENSE) for the copyright notice and terms.

This is an independent implementation and contains no Shine source code or
assets.
