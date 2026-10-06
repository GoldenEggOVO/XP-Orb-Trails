package dev.goldeneggovo.xporbtrails;

import org.junit.jupiter.api.Test;
import com.google.gson.Gson;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrailConfigTest {
    @Test
    void repairsNonFiniteNumbersWithoutDiscardingOtherSettings() throws ReflectiveOperationException {
        Gson gson = new Gson();
        TrailConfig defaults = new TrailConfig();
        for (var field : TrailConfig.class.getFields()) {
            if (field.getType() != double.class) continue;
            for (String value : new String[]{"NaN", "Infinity", "-Infinity"}) {
                var json = gson.toJsonTree(defaults).getAsJsonObject();
                json.addProperty(field.getName(), value);
                json.addProperty("startColor", 0x123456);
                TrailConfig repaired = gson.fromJson(json, TrailConfig.class).sanitized();
                assertEquals(field.getDouble(defaults), field.getDouble(repaired), field.getName() + "=" + value);
                assertEquals(0x123456, repaired.startColor);
                gson.toJson(repaired);
            }
        }
    }

    @Test
    void repairsNonFiniteSavedProfilesBeforeSavingAndApplying() throws ReflectiveOperationException {
        Gson gson = new Gson();
        TrailConfig defaults = new TrailConfig();
        for (var field : TrailConfig.SavedProfile.class.getFields()) {
            if (field.getType() != double.class) continue;
            for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                TrailConfig config = new TrailConfig();
                var profile = new TrailConfig.SavedProfile("Keep my name", defaults);
                field.setDouble(profile, value);
                config.savedProfiles.add(profile);
                config.sanitized();
                assertEquals(TrailConfig.class.getField(field.getName()).getDouble(defaults), field.getDouble(profile), field.getName());
                assertEquals("Keep my name", profile.name);
                profile.applyTo(config);
                gson.toJson(config);
            }
        }
    }

    @Test
    void repairsNonFiniteLegacyValuesBeforeMigration() {
        Gson gson = new Gson();
        TrailConfig config = gson.fromJson("""
                {"configVersion":8,"width":"NaN","opacity":"NaN","glowStrength":"Infinity",
                 "pickupFlashSize":"NaN","savedProfiles":[
                   {"name":"Legacy","width":"NaN","opacity":"NaN","glowStrength":"NaN","pickupFlashSize":"NaN"}
                 ]}
                """, TrailConfig.class).sanitized();
        gson.toJson(config);
        config.savedProfiles.getFirst().applyTo(config);
        assertEquals(true, Double.isFinite(config.width) && Double.isFinite(config.effectStrength)
                && Double.isFinite(config.pickupFlashSize));
    }

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
        config.enabled = false;
        config.renderRange = 71;
        config.trailCap = 24;
        config.width = 0.42;
        config.colorMode = "rainbow";
        config.pickupFlashStyle = "ring";
        TrailConfig.SavedProfile profile = new TrailConfig.SavedProfile("Favorite", config);
        config.enabled = true;
        config.renderRange = 36;
        config.trailCap = 96;
        config.width = 0.12;
        config.colorMode = "solid";
        config.pickupFlashStyle = "soft";

        profile.applyTo(config);

        assertEquals(false, config.enabled);
        assertEquals(71, config.renderRange);
        assertEquals(24, config.trailCap);
        assertEquals(0.42, config.width);
        assertEquals("rainbow", config.colorMode);
        assertEquals("ring", config.pickupFlashStyle);
    }
}
