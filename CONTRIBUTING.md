# Contributing

XP Orb Trails supports Fabric, Forge, and NeoForge on the client only.
Choose the Minecraft branch matching your target: `26.3` or `26.2`.
Apply shared fixes to both branches, adapt Minecraft APIs separately, and
verify each loader before calling a shared change complete.

## Source layout

```text
common/src/main/java/       Shared settings, menus, geometry, rendering, and mixins
common/src/main/resources/  Shared mixin configuration, icon, and language files
common/src/test/            Configuration and persistence regression tests
common/src/clientTest/      Packaged client probe shared by Forge and NeoForge
fabric/src/main/           Fabric entry point, Mod Menu integration, metadata
fabric/src/gametest/       Fabric client regressions
forge/src/main/            Forge entry point, render hooks, metadata
forge/src/clientTest/      Forge test entry point and metadata
neoforge/src/main/         NeoForge entry point and metadata
neoforge/src/clientTest/   NeoForge test entry point and metadata
gradle/wrapper/            Reproducible Gradle wrapper
```

Each loader compiles the common sources against its Minecraft environment.
Common runtime code must not import loader-specific APIs. Platform entry points
supply the configuration directory, events, key binding, and settings-screen
factory. User-facing text uses matching keys in `en_us.json` and `zh_cn.json`.
The 26.2 and 26.3 render backends retain their own Minecraft API types.

## Build and validation

Use JDK 25 and the checked-in wrapper (`gradlew.bat` on Windows):

```sh
./gradlew build
./gradlew -Ploaders=fabric build
./gradlew -Ploaders=forge build
./gradlew -Ploaders=neoforge build
```

The default build includes all loaders. Each platform's `verifyReleaseJar`
checks shared classes, mixins, translations, license, expanded metadata,
platform entry points, and exclusion of test classes and other loaders.
JUnit results are in `common/build/reports/tests/test/`; installable JARs are
in `<loader>/build/libs/`. Do not install source or client-test JARs.
Forge's launcher may provision an additional Java toolchain; the mod and
Minecraft use Java 25.

Run the graphical client checks separately:

```sh
./gradlew -Ploaders=fabric -PclientTests :fabric:runClientGameTest
./gradlew -Ploaders=forge -PclientTests :forge:runClientTestClient
./gradlew -Ploaders=neoforge -PclientTests :neoforge:runClientProbe
```

Forge/NeoForge probes load the packaged mod JAR. Prepare a disposable world at
`<loader>/build/run/clientProbe/saves/Probe World`, disable `pauseOnLostFocus`
in that run's options, and seed `config/xp-orb-trails.json` with width `0.42`
and a saved color named `Existing`. The initial probe checks loader settings
factories, the key binding, English/Chinese menus, page resets, real pickup
packets, disappearance without pickup, cleanup, and rendered trail pixels
for 2/3/8/32 sides in both blend modes. It saves a color and profile before exit.
Run again with `-PprobeRestart` to verify persisted settings. Inspect
`probe-result.json` and `probe-restart.json`: a successful process exit alone
does not prove the probe passed. Test code is excluded from installable JARs.

Fabric checks additionally cover color/profile management, keyboard scrolling,
small and wide layouts, trail limits, and geometry fingerprints. Screenshots
and automation do not replace human in-game acceptance of appearance and feel.
Use a separate Minecraft instance for testing; keep player data out of Git.

## Releases

1. Update version, dependencies, README, and changelog on the Minecraft branch.
2. Build all loaders, inspect metadata, and complete client acceptance.
3. Commit and push verified source only when authorized.
4. Publish each loader with tag/title `<loader>-<mod-version>+<minecraft-version>`
   and JAR `xp-orb-trails-<loader>-<mod-version>+<minecraft-version>.jar`.
   Preserve existing releases unless replacement is explicitly requested.
5. Download published JARs and compare SHA-256 with local builds.

Metadata uses the semantic version `1.3.0+mc26.2`; the loader prefix belongs
in release names, tags, and filenames. Both Minecraft branches may share a
mod version. On Modrinth, select the correct game version and loader as well.
Use Git and GitHub CLI for publishing. This repository has no Actions workflows.
Keep build output, runtime files, credentials, and local reports out of Git.
