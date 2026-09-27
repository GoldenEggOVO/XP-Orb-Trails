# Changelog

## 1.0.2 — Minecraft 26.2

- Add a cross-section slider from a camera-facing ribbon (2 sides) to a round approximation (32 sides).
- Default new configurations to a 2-sided ribbon with a trail width of 0.1; preserve existing settings.
- Save cross-section choices in custom profiles and restore them when applying a profile.
- Restore saved color deletion with confirmation, including when all 12 slots are full.
- Show a single swatch on individual color buttons.

- Reorganize settings into scrollable Common, Appearance, Advanced, and My Profiles pages.
- Replace cycling presets with selection lists and collapse advanced detail controls.
- Merge opacity and glow strength into Effect Strength, preserving existing configurations and profiles.
- Make pickup flash size independent of trail width and show a live preview in wide windows.

- Keep trails continuous when the number of experience orbs exceeds the trail limit.
- Skip distant orbs before tracking and release their trail slots when they leave range.
- Clear trails when disabled, when changing worlds, and when disconnecting.
- Restore the pickup flash style when resetting the pickup settings page.
- Fit the color picker controls within smaller windows and larger GUI scales.

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
