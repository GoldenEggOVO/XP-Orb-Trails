package dev.goldeneggovo.xporbtrails;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("xporbtrails")
public final class ForgeXpOrbTrails {
    public ForgeXpOrbTrails(FMLJavaModLoadingContext context) {
        XpOrbTrailsClient.initialize(FMLPaths.CONFIGDIR.get());
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(XpOrbTrailsClient::createConfigScreen));
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("xporbtrails", "settings"));
        KeyMapping openSettings = new KeyMapping("key.xporbtrails.open_settings",
                InputConstants.UNKNOWN.getValue(), category);
        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(openSettings));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            Minecraft client = Minecraft.getInstance();
            TrailRenderer.updateWorld(client.level);
            while (openSettings.consumeClick()) {
                client.gui.setScreen(XpOrbTrailsClient.createConfigScreen(client.gui.screen()));
            }
        });
    }
}
