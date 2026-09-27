package dev.goldeneggovo.xporbtrails;

import com.terraformersmc.modmenu.ModMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.entity.ExperienceOrb;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TrailClientTest implements FabricClientGameTest {
    private final List<String> failures = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            XpOrbTrailsClient.CONFIG = new TrailConfig();
            XpOrbTrailsClient.CONFIG.pickupFlashStyle = "ring";
            client.gui.setScreen(ModMenu.getConfigScreen("xporbtrails", client.gui.screen()));
        });
        context.waitForScreen(TrailConfigScreen.class);
        context.clickScreenButton("screen.xporbtrails.pickup");
        context.clickScreenButton("screen.xporbtrails.reset");
        context.runOnClient(client -> check("soft".equals(XpOrbTrailsClient.CONFIG.pickupFlashStyle),
                "Reset pickup page must restore the flash style"));
        context.runOnClient(client -> {
            var screen = new ColorPickerScreen(client.gui.screen(), 0x00FF00, color -> { });
            client.gui.setScreen(screen);
            screen.resize(427, 240);
            for (var child : screen.children()) {
                if (child instanceof AbstractWidget widget) {
                    check(widget.getY() >= 0 && widget.getBottom() <= 240,
                            "Color picker controls must fit a 240-pixel GUI height");
                }
            }
        });
        context.takeScreenshot("color-picker-small");
        context.runOnClient(client -> client.gui.screen().onClose());
        context.runOnClient(client -> client.gui.screen().onClose());

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(client -> {
                trails().clear();
                XpOrbTrailsClient.CONFIG.trailCap = 8;
                var camera = client.gameRenderer.mainCamera().position();
                var orbs = new ArrayList<ExperienceOrb>();
                for (int i = 0; i < 9; i++) {
                    var orb = new ExperienceOrb(client.level, camera.x, camera.y, camera.z + 3, 1);
                    orb.setId(100000 + i);
                    orbs.add(orb);
                }
                for (int tick = 0; tick < 5; tick++) {
                    for (var orb : orbs) {
                        orb.setPos(orb.getX() + 0.1, orb.getY(), orb.getZ());
                        TrailRenderer.track(orb);
                    }
                }
                long continuous = trails().values().stream()
                        .filter(trail -> ((List<?>) field(trail, "points")).size() >= 2).count();
                check(continuous == 8, "Over-cap orbs must retain eight continuous trails; got " + continuous);
                trails().clear();
                var farOrb = new ExperienceOrb(client.level, camera.x + 100, camera.y, camera.z, 1);
                farOrb.setId(100100);
                TrailRenderer.track(farOrb);
                check(trails().isEmpty(), "Out-of-range orbs must not consume trail slots");
                trails().clear();
                XpOrbTrailsClient.CONFIG = new TrailConfig();
            });
            for (boolean glow : new boolean[]{true, false}) {
                context.runOnClient(client -> XpOrbTrailsClient.CONFIG.additiveGlow = glow);
                world.getServer().runCommand("execute at @p run summon minecraft:experience_orb ~ ~2 ~3 {Value:1,Motion:[0.1d,0.1d,0.0d]}");
                context.waitFor(client -> !snapshot().isEmpty());
                context.waitTicks(3);
                context.takeScreenshot(glow ? "trail-glow" : "trail-alpha");
            }
            context.runOnClient(client -> XpOrbTrailsClient.CONFIG.enabled = false);
            context.waitTicks(2);
            context.runOnClient(client -> {
                check(snapshot().isEmpty() && trails().isEmpty(), "Disabling must clear visible and tracked trails immediately");
                XpOrbTrailsClient.CONFIG.enabled = true;
            });
            world.getServer().runCommand("execute at @p run summon minecraft:experience_orb ~ ~2 ~3 {Value:1,Motion:[0.1d,0.1d,0.0d]}");
            context.waitFor(client -> !snapshot().isEmpty());
        }
        context.waitTicks(2);
        context.runOnClient(client -> {
            check(trails().isEmpty() && snapshot().isEmpty() && field(null, "lastLevel") == null,
                    "Disconnect must release the world and all trail state");
            XpOrbTrailsClient.CONFIG = new TrailConfig();
        });
        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }

    private void check(boolean success, String message) {
        if (!success) failures.add(message);
    }

    private static Map<?, ?> trails() { return (Map<?, ?>) field(null, "TRAILS"); }
    private static List<?> snapshot() { return (List<?>) field(null, "renderTrails"); }

    private static Object field(Object target, String name) {
        try {
            var field = (target == null ? TrailRenderer.class : target.getClass()).getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
