package dev.goldeneggovo.xporbtrails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ClientConfigStorageTest {
    @TempDir Path directory;

    private void initialize() throws Exception {
        try {
            XpOrbTrailsClient.class.getMethod("initialize", Path.class).invoke(null, directory);
        } catch (NoSuchMethodException exception) {
            fail("Shared client initialization must accept the loader's config directory", exception);
        }
    }

    @Test
    void loadsAndSavesExistingSettingsInTheSuppliedDirectory() throws Exception {
        Path config = directory.resolve("xp-orb-trails.json");
        Files.writeString(config, "{\"width\":0.42,\"savedPresets\":[{\"name\":\"Existing\",\"startColor\":1193046,\"endColor\":11259375}]}");
        initialize();
        assertEquals(0.42, XpOrbTrailsClient.CONFIG.width);
        assertEquals("Existing", XpOrbTrailsClient.CONFIG.savedPresets.getFirst().name);
        XpOrbTrailsClient.CONFIG.width = 0.37;
        XpOrbTrailsClient.saveConfig();
        initialize();
        assertEquals(0.37, XpOrbTrailsClient.CONFIG.width);
        assertEquals(1193046, XpOrbTrailsClient.CONFIG.savedPresets.getFirst().startColor);
        assertFalse(Files.exists(directory.resolve("xp-orb-trails.json.tmp")));
    }

    @Test
    void backsUpBrokenConfigBeforeRestoringDefaults() throws Exception {
        Path config = directory.resolve("xp-orb-trails.json");
        Files.writeString(config, "{broken");
        initialize();
        assertEquals("{broken", Files.readString(directory.resolve("xp-orb-trails.json.broken")));
        assertEquals(0.1, XpOrbTrailsClient.CONFIG.width);
        assertTrue(Files.readString(config).contains("\"width\": 0.1"));
    }
}
