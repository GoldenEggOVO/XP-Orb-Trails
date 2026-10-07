package dev.goldeneggovo.xporbtrails.test;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "xporbtrails_client_test", dist = Dist.CLIENT)
public final class NeoForgeClientProbe {
    public NeoForgeClientProbe() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var container = ModList.get().getModContainerById("xporbtrails").orElseThrow();
            TrailClientProbe.configure(FMLPaths.CONFIGDIR.get(), parent ->
                    IConfigScreenFactory.getForMod(container.getModInfo()).orElseThrow().createScreen(container, parent));
            TrailClientProbe.tick();
        });
    }
}
