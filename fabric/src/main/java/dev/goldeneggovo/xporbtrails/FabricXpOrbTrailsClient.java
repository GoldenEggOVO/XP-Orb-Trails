package dev.goldeneggovo.xporbtrails;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class FabricXpOrbTrailsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        XpOrbTrailsClient.initialize(FabricLoader.getInstance().getConfigDir());
        LevelExtractionEvents.END_EXTRACTION.register(context -> TrailRenderer.extract(
                context.level(), context.camera().position(), context.deltaTracker().getGameTimeDeltaPartialTick(true)));
        LevelRenderEvents.END_MAIN.register(context -> TrailRenderer.render(context.levelState().cameraRenderState.pos));
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("xporbtrails", "settings"));
        KeyMapping openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.xporbtrails.open_settings", InputConstants.UNKNOWN.getValue(), category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TrailRenderer.updateWorld(client.level);
            while (openSettings.consumeClick()) {
                client.gui.setScreen(XpOrbTrailsClient.createConfigScreen(client.gui.screen()));
            }
        });
    }
}
