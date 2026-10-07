package dev.goldeneggovo.xporbtrails.test;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("xporbtrails_client_test")
public final class ForgeClientProbe {
    public ForgeClientProbe() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            var info = ModList.getModContainerById("xporbtrails").orElseThrow().getModInfo();
            TrailClientProbe.configure(FMLPaths.CONFIGDIR.get(), parent ->
                    ConfigScreenHandler.getScreenFactoryFor(info).orElseThrow().apply(net.minecraft.client.Minecraft.getInstance(), parent));
            TrailClientProbe.tick();
        });
    }
}
