package dev.goldeneggovo.xporbtrails;

import org.junit.jupiter.api.Test;
import com.google.gson.Gson;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrailConfigTest {
    @Test
    void crossSectionSurvivesProfilesAndReloadAndClampsInvalidValues() {
        Gson gson = new Gson();
        TrailConfig config = gson.fromJson("{\"crossSectionSides\":32}", TrailConfig.class).sanitized();
        assertEquals(32, gson.toJsonTree(config).getAsJsonObject().get("crossSectionSides").getAsInt());
        TrailConfig copy = new TrailConfig();
        copy.copyFrom(config);
        TrailConfig.SavedProfile profile = new TrailConfig.SavedProfile("Round", copy);
        profile = gson.fromJson(gson.toJson(new TrailConfig.SavedProfile(profile)), TrailConfig.SavedProfile.class);
        profile.applyTo(copy);
        assertEquals(32, gson.toJsonTree(copy).getAsJsonObject().get("crossSectionSides").getAsInt());
        for (int value : new int[]{-1, 2, 3, 8, 32, 100}) {
            TrailConfig loaded = gson.fromJson("{\"crossSectionSides\":" + value + "}", TrailConfig.class).sanitized();
            assertEquals(Math.max(2, Math.min(32, value)), gson.toJsonTree(loaded).getAsJsonObject().get("crossSectionSides").getAsInt());
        }
        assertEquals(2, gson.fromJson("{}", TrailConfig.class).crossSectionSides);
        assertEquals(0.1, new TrailConfig().width);
        TrailConfig existing = gson.fromJson("{\"width\":0.24,\"crossSectionSides\":8}", TrailConfig.class).sanitized();
        assertEquals(0.24, existing.width);
        assertEquals(8, existing.crossSectionSides);
    }

    @Test
    void migratesLegacyStrengthAndFlashSizeWithoutChangingTheEffect() {
        Gson gson = new Gson();
        TrailConfig config = gson.fromJson("""
                {"configVersion":8,"width":0.48,"opacity":0.6,"glowStrength":1.5,
                 "pickupFlashSize":0.8,"savedProfiles":[
                   {"name":"Legacy","width":0.12,"opacity":0.8,"glowStrength":1.25,"pickupFlashSize":0.6}
                 ]}
                """, TrailConfig.class).sanitized();
        assertEquals(9, config.configVersion);
        assertEquals(0.9, gson.toJsonTree(config).getAsJsonObject().get("effectStrength").getAsDouble(), 1e-9);
        assertEquals(1.6, config.pickupFlashSize, 1e-9);
        config.savedProfiles.getFirst().applyTo(config);
        assertEquals(1.0, gson.toJsonTree(config).getAsJsonObject().get("effectStrength").getAsDouble(), 1e-9);
        assertEquals(0.3, config.pickupFlashSize, 1e-9);
    }

    @Test
    void migrationIsStableAcrossSavingAndChangingTrailWidth() {
        Gson gson = new Gson();
        TrailConfig config = gson.fromJson("""
                {"configVersion":8,"width":0.48,"opacity":0.6,"glowStrength":1.5,"pickupFlashSize":0.8}
                """, TrailConfig.class).sanitized();
        config.width = 0.1;
        config = gson.fromJson(gson.toJson(config), TrailConfig.class).sanitized();
        assertEquals(9, config.configVersion);
        assertEquals(1.6, config.pickupFlashSize, 1e-9);
        assertEquals(0.9, gson.toJsonTree(config).getAsJsonObject().get("effectStrength").getAsDouble(), 1e-9);
    }

    @Test
    void newProfilesAndCopiesPreserveIndependentControlsAfterReload() {
        TrailConfig config = new TrailConfig();
        config.effectStrength = 1.35;
        config.pickupFlashSize = 1.7;
        config.savedProfiles.add(new TrailConfig.SavedProfile("New", config));
        Gson gson = new Gson();
        config = gson.fromJson(gson.toJson(config), TrailConfig.class).sanitized();
        TrailConfig copy = new TrailConfig();
        copy.copyFrom(config);
        copy.width = 0.8;
        copy.effectStrength = 0.5;
        copy.savedProfiles.getFirst().applyTo(copy);
        assertEquals(1.35, copy.effectStrength);
        assertEquals(1.7, copy.pickupFlashSize);
        copy.width = 0.1;
        assertEquals(1.7, copy.sanitized().pickupFlashSize);
    }

    @Test
    void sanitizesInvalidSettingsAndSavedProfiles() {
        TrailConfig config = new TrailConfig();
        config.trailCap = 999;
        config.renderRange = -1;
        config.opacity = -1;
        config.colorMode = "invalid";
        config.pickupFlashStyle = "invalid";
        config.savedProfiles = new ArrayList<>();
        config.savedProfiles.add(new TrailConfig.SavedProfile("", config));
        for (int i = 0; i < 10; i++) {
            config.savedProfiles.add(new TrailConfig.SavedProfile("Profile " + i, config));
        }

        config.sanitized();

        assertEquals(256, config.trailCap);
        assertEquals(4.0, config.renderRange);
        assertEquals(0.05, config.opacity);
        assertEquals("gradient", config.colorMode);
        assertEquals("soft", config.pickupFlashStyle);
        assertEquals(8, config.savedProfiles.size());
        assertEquals("Profile 0", config.savedProfiles.getFirst().name);
    }

    @Test
    void migratesOldConfigToCurrentDefaults() {
        TrailConfig config = new TrailConfig();
        config.configVersion = 1;
        config.motionShift = 0.9;
        config.pickupFadeSeconds = 2.0;
        config.pickupFlashStyle = "star";

        config.sanitized();

        assertEquals(9, config.configVersion);
        assertEquals(0.10, config.motionShift);
        assertEquals(0.35, config.pickupFadeSeconds);
        assertEquals("soft", config.pickupFlashStyle);
    }

    @Test
    void savedProfileRestoresCapturedAppearance() {
        TrailConfig config = new TrailConfig();
        config.width = 0.42;
        config.colorMode = "rainbow";
        config.pickupFlashStyle = "ring";
        TrailConfig.SavedProfile profile = new TrailConfig.SavedProfile("Favorite", config);
        config.width = 0.12;
        config.colorMode = "solid";
        config.pickupFlashStyle = "soft";

        profile.applyTo(config);

        assertEquals(0.42, config.width);
        assertEquals("rainbow", config.colorMode);
        assertEquals("ring", config.pickupFlashStyle);
    }
}
