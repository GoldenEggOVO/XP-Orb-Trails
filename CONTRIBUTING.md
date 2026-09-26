# Contributing

XP Orb Trails is a client-only Fabric mod. Choose the branch matching the
Minecraft version you are working on: `26.3` for current development or `26.2`
for maintenance of that release. Keep version-specific changes on that branch.

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

Before releasing, start a compatible Fabric client and check settings from
both Mod Menu and the key binding, English and Chinese translations, the
preview screen, moving orb trails, pickup flashes, and configuration persistence.
Compilation and unit tests do not replace these client checks.

## Releases

1. Update the mod version, Minecraft dependencies, README, and changelog on the
   matching Minecraft branch.
2. Run a clean build, inspect the packaged metadata, and complete client checks.
3. Commit and push the verified source, then tag that commit as `v<mod-version>`.
   Add `+mc<minecraft-version>` only when the same mod version is released
   separately for multiple Minecraft versions.
4. Publish a GitHub Release titled `v<mod-version> for <minecraft-version>` with
   concise English change notes and the installable JAR. Keep the Minecraft
   version in its filename, for example `xp-orb-trails-1.1.0+mc26.3.jar`.
5. Download the published JAR and compare its SHA-256 with the local build.

Use Git and GitHub CLI for publishing. There are no GitHub Actions workflows.
Keep build outputs, runtime files, credentials, and local verification reports
out of source control.
