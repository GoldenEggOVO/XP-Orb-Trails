package dev.goldenegg.xporbtrails;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrailConfigTest {
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

        assertEquals(8, config.configVersion);
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
