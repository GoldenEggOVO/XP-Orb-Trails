package dev.goldeneggovo.xporbtrails;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XpOrbTrailsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("XP Orb Trails");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static TrailConfig CONFIG = new TrailConfig();
    private static Path configPath;

    private XpOrbTrailsClient() { }

    public static void initialize(Path configDirectory) {
        configPath = configDirectory.resolve("xp-orb-trails.json");
        CONFIG = new TrailConfig();
        if (Files.exists(configPath)) {
            try {
                try (Reader reader = Files.newBufferedReader(configPath)) {
                    TrailConfig loaded = GSON.fromJson(reader, TrailConfig.class);
                    CONFIG = loaded == null ? new TrailConfig() : loaded.sanitized();
                }
                saveConfig();
            } catch (IOException | JsonParseException exception) {
                LOGGER.warn("Could not read XP Orb Trails config; backing it up and restoring defaults", exception);
                backupBrokenConfig();
                CONFIG = new TrailConfig();
                saveConfig();
            }
        } else {
            saveConfig();
        }

    }

    public static Screen createConfigScreen(Screen parent) {
        return new TrailConfigScreen(parent);
    }

    public static void saveConfig() {
        if (configPath == null) throw new IllegalStateException("XP Orb Trails has not been initialized");
        Path temporaryPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryPath)) {
                GSON.toJson(CONFIG.sanitized(), writer);
            }
            try {
                Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save XP Orb Trails config to {}", configPath, exception);
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupException) {
                LOGGER.debug("Could not remove temporary XP Orb Trails config {}", temporaryPath, cleanupException);
            }
        }
    }

    private static void backupBrokenConfig() {
        for (int suffix = 0; suffix < 100; suffix++) {
            String name = "xp-orb-trails.json.broken" + (suffix == 0 ? "" : "-" + suffix);
            Path backupPath = configPath.resolveSibling(name);
            if (Files.exists(backupPath)) continue;
            try {
                Files.move(configPath, backupPath);
                LOGGER.warn("Saved the unreadable config as {}", backupPath);
            } catch (IOException exception) {
                LOGGER.warn("Could not back up unreadable config {}", configPath, exception);
            }
            return;
        }
        LOGGER.warn("Could not back up unreadable config because all backup names are in use");
    }
}
