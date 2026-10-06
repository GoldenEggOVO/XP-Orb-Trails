# Contributing

XP Orb Trails is a client-only Fabric mod. Choose the branch matching the
Minecraft version you are working on: `26.3` for current development or `26.2`
for maintenance of that release. Keep version-specific changes on that branch.

Apply shared bug fixes and optimizations to both maintained branches, `26.2`
and `26.3`. Adapt Minecraft-specific APIs separately and run each branch's build
and client regressions before calling the shared change complete.

## Source layout

```text
src/client/java/dev/goldeneggovo/xporbtrails/
  TrailRenderer.java       Trail geometry and rendering
  TrailConfig.java         Settings, profiles, and defaults
  *Screen.java             Configuration, color picker, and preview screens
  XpOrbTrailsClient.java   Client initialization and configuration persistence
  ModMenuIntegration.java Mod Menu configuration entry point
  mixin/                  Minecraft hooks
src/client/resources/     Client mixin configuration
src/main/resources/       Mod metadata, icon, and language JSON files
src/test/java/            Configuration regression tests
src/gametest/             Optional Minecraft client regression tests
gradle/wrapper/           Reproducible Gradle wrapper
```

Runtime code stays in the client source set. User-facing text uses Minecraft
translation keys; add matching entries to `en_us.json` and `zh_cn.json`.

## Build and validation

Use JDK 25 and the checked-in wrapper:

```sh
./gradlew clean build
```

On Windows, use `./gradlew.bat clean build`. The build runs JUnit tests and
`verifyReleaseJar`, which checks that the settings UI, Mod Menu integration,
mixins, and translations are packaged. Test reports are in
`build/reports/tests/test/`; installable JARs are in `build/libs/`.

Run the client regressions with `./gradlew -PclientTests runClientGameTest`
(use `gradlew.bat` on Windows). This launches a test client and temporary world
to check trail limits, range filtering, toggle and disconnect cleanup, small
color-picker layouts, pickup resets, and both rendering modes. A graphical
environment is required. The test mod is not included in the release JAR.
Menu checks cover preset selection, conditional controls, profile management,
keyboard scrolling, and narrow/wide layouts. Unit tests also verify legacy
configuration and profile migration.

Before releasing, start a compatible Fabric client and check settings from
both Mod Menu and the key binding, English and Chinese translations, the
preview screen, moving orb trails, pickup flashes, and configuration persistence.
Compilation and unit tests do not replace these client checks.

## Releases

1. Update the mod version, Minecraft dependencies, README, and changelog on the
   matching Minecraft branch.
2. Run a clean build, inspect the packaged metadata, and complete client checks.
3. Commit and push the verified source, then tag that commit as
   `fabric-<mod-version>+<minecraft-version>`.
4. Publish a GitHub Release titled `fabric-<mod-version>+<minecraft-version>` with
   concise English change notes and the installable JAR, for example
   `xp-orb-trails-fabric-1.2.0+26.2.jar`. Publish a new release for each update;
   preserve existing releases unless replacement is explicitly requested.
5. Download the published JAR and compare its SHA-256 with the local build.

Use Git and GitHub CLI for publishing. There are no GitHub Actions workflows.
Keep build outputs, runtime files, credentials, and local verification reports
out of source control.

Fabric metadata uses the semantic version `1.2.0+mc26.2`; the loader prefix is
reserved for release titles, tags, and filenames. Both Minecraft branches may
share the mod version when they contain the same changes. On Modrinth, also set
the correct game version and loader and upload the verified JAR; filenames alone
do not determine update detection.
