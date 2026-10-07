package dev.goldeneggovo.xporbtrails;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "xporbtrails", dist = Dist.CLIENT)
public final class NeoForgeXpOrbTrails {
    public NeoForgeXpOrbTrails(IEventBus modBus, ModContainer container) {
        XpOrbTrailsClient.initialize(FMLPaths.CONFIGDIR.get());
        IConfigScreenFactory configScreen = (mod, parent) -> XpOrbTrailsClient.createConfigScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, configScreen);
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("xporbtrails", "settings"));
        KeyMapping openSettings = new KeyMapping("key.xporbtrails.open_settings",
                InputConstants.UNKNOWN.getValue(), category);
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(openSettings));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft client = Minecraft.getInstance();
            TrailRenderer.updateWorld(client.level);
            while (openSettings.consumeClick()) {
                client.gui.setScreen(XpOrbTrailsClient.createConfigScreen(client.gui.screen()));
            }
        });
        NeoForge.EVENT_BUS.addListener((ExtractLevelRenderStateEvent event) -> TrailRenderer.extract(
                event.getLevel(), event.getCamera().position(), event.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterLevel event) -> {
            var camera = event.getLevelRenderState().cameraRenderState;
            var modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix().mul(camera.viewRotationMatrix);
            try {
                TrailRenderer.render(camera.pos);
            } finally {
                modelView.popMatrix();
            }
        });
    }
}
