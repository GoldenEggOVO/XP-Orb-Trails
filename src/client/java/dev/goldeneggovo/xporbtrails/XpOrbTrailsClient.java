package dev.goldeneggovo.xporbtrails;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XpOrbTrailsClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("XP Orb Trails");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static TrailConfig CONFIG = new TrailConfig();
    private static Path configPath;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("xp-orb-trails.json");
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

        LevelExtractionEvents.END_EXTRACTION.register(TrailRenderer::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(TrailRenderer::render);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("xporbtrails", "settings"));
        KeyMapping openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.xporbtrails.open_settings", InputConstants.UNKNOWN.getValue(), category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TrailRenderer.updateWorld(client.level);
            while (openSettings.consumeClick()) client.gui.setScreen(createConfigScreen(client.gui.screen()));
        });
    }

    public static Screen createConfigScreen(Screen parent) {
        return new TrailConfigScreen(parent);
    }

    public static void saveConfig() {
        if (configPath == null) configPath = FabricLoader.getInstance().getConfigDir().resolve("xp-orb-trails.json");
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
