# Changelog

## 1.0.1 — Minecraft 26.3

- Update Minecraft compatibility to 26.3, with Fabric API 0.161.0+26.3 and
  Mod Menu 21.0.0 integration.
- Adapt trail rendering to RenderPearl and draw after the terrain render pass closes.
- Keep the existing trail features, settings, profiles, and translations.
- Organize source branches by Minecraft version and remove GitHub Actions workflows.

## 1.0.1 — Minecraft 26.2

- Restore the complete settings UI in the release JAR. Trail settings can be
  edited using Mod Menu's Config button or the Open Trail Settings key binding.
- Make built-in color presets follow Minecraft's selected language, with English
  as the fallback. New preset and profile names use the language selected when
  they are created.
- Standardize author credits and the Java package namespace on GoldenEggOVO.
- Verify settings classes, Mod Menu integration, and translations in the release JAR.

## 1.0.0 - 2026-08-31

First public release for Minecraft 26.2.

- Add smooth, interpolated three-dimensional trails to moving experience orbs.
- Add solid, gradient, and animated rainbow color modes with an in-game color picker.
- Add separate head, middle, and tail widths with ready-made shape presets.
- Add adjustable lifetime, smoothing, opacity, glow, motion shift, camera push,
  render distance, pickup fading, and performance limits.
- Add optional soft, star, and ring pickup flashes with conservative defaults.
- Add built-in presets and named custom profiles with rename, overwrite, and
  guarded deletion controls.
- Add a standalone animated preview window and immediate in-game feedback.
- Add complete Simplified Chinese and English translations.
- Add safe configuration recovery, malformed-file backups, and atomic saving.
- Add Mod Menu integration and a configurable settings keybind.
