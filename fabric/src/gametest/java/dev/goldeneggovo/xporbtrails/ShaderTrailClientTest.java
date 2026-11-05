package dev.goldeneggovo.xporbtrails;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

final class ShaderTrailClientTest {
    static void run(ClientGameTestContext context) {
        var failures = new ArrayList<String>();
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode spectator @p");
            world.getServer().runCommand("tp @p 0 80 0 0 0");
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("weather clear");
            world.getClientLevel().waitForChunksRender();
            context.waitTicks(30);
            context.runOnClient(client -> {
                try {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    Object instance = api.getMethod("getInstance").invoke(null);
                    boolean active = (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
                    if (active != Boolean.getBoolean("xporb.expectShaders"))
                        throw new AssertionError("Unexpected Iris shader state: " + active);
                    System.out.println("XPORB_SHADER_PROBE iris_active=" + active);
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Iris must be loaded for this regression", error);
                }
            });
            for (boolean glow : new boolean[] {false, true}) {
                for (boolean occluded : new boolean[] {false, true}) {
                    world.getServer().runCommand("fill -4 78 2 4 85 2 " + (occluded ? "stone" : "air"));
                    context.waitTicks(12);
                    context.runOnClient(client -> seedTrail(client, glow));
                    context.waitTicks(3);
                    String name = "shader-trail-" + (glow ? "glow" : "alpha") + (occluded ? "-hidden" : "-visible");
                    var captured = new CompletableFuture<Integer>();
                    context.runOnClient(client -> Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> {
                        try {
                            var directory = client.gameDirectory.toPath().resolve("screenshots");
                            Files.createDirectories(directory);
                            image.writeToFile(directory.resolve(name + ".png"));
                            int pixels = 0;
                            for (int y = 0; y < image.getHeight(); y++) {
                                for (int x = 0; x < image.getWidth(); x++) {
                                    int color = image.getPixel(x, y);
                                    int red = (color >> 16) & 255, green = (color >> 8) & 255, blue = color & 255;
                                    if (red > 140 && blue > 140 && Math.min(red, blue) - green > 25)
                                        pixels++;
                                }
                            }
                            captured.complete(pixels);
                        } catch (Throwable error) {
                            captured.completeExceptionally(error);
                        } finally {
                            image.close();
                        }
                    }));
                    context.waitFor(client -> captured.isDone());
                    int pixels = captured.join();
                    System.out.println("XPORB_SHADER_PROBE " + name + " magenta_pixels=" + pixels);
                    if (occluded ? pixels > 2 : pixels < 8) failures.add(name + ": magenta pixels=" + pixels);
                }
            }
        }
        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }

    private static void seedTrail(Minecraft client, boolean glow) {
        TrailRenderer.updateWorld(null);
        XpOrbTrailsClient.CONFIG = new TrailConfig();
        var config = XpOrbTrailsClient.CONFIG;
        config.startColor = config.endColor = 0xff00ff;
        config.colorMode = "solid";
        config.width = .3;
        config.lifetimeSeconds = 8;
        config.smoothFlow = 0;
        config.additiveGlow = glow;
        config.orbYOffset = config.motionShift = config.cameraPush = 0;
        var camera = client.gameRenderer.getMainCamera().position();
        client.level.removeEntity(100300, Entity.RemovalReason.DISCARDED);
        var orb = new ExperienceOrb(client.level, camera.x - 1.2, camera.y + .2, camera.z + 4, 1);
        orb.setId(100300);
        orb.setNoGravity(true);
        orb.setDeltaMovement(Vec3.ZERO);
        client.level.addEntity(orb);
        for (int i = 0; i < 16; i++) {
            orb.setPos(camera.x - 1.2 + i * .16, camera.y + .2, camera.z + 4);
            TrailRenderer.track(orb);
        }
    }
}
