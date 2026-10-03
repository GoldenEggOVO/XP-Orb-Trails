package dev.goldeneggovo.xporbtrails;

import com.terraformersmc.modmenu.ModMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
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
        context.runOnClient(client -> {
            boolean commonPage = client.gui.screen().children().stream().anyMatch(child ->
                    child instanceof AbstractWidget widget && widget.getMessage().getString().equals(
                            net.minecraft.network.chat.Component.translatable("screen.xporbtrails.common").getString()));
            if (!commonPage) throw new AssertionError("Settings must open with a Common tab");
        });
        context.waitTicks(3);
        context.takeScreenshot("menu-common-small");
        context.runOnClient(client -> {
            long sliders = widgets(client.gui.screen()).stream().filter(w -> w instanceof AbstractSliderButton).count();
            check(sliders == 3, "Common page must have only width, retention, and effect strength sliders");
        });
        context.clickScreenButton("screen.xporbtrails.appearance");
        context.clickScreenButton("screen.xporbtrails.reset");
        context.runOnClient(client -> check("soft".equals(XpOrbTrailsClient.CONFIG.pickupFlashStyle),
                "Reset pickup page must restore the flash style"));
        context.waitTicks(3);
        context.takeScreenshot("menu-appearance-small");
        slideCrossSection(context, 1.0);
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.crossSectionSides == 32,
                "Round selection must use 32 sides"));
        slideCrossSection(context, 0.37);
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.crossSectionSides == 13,
                "Cross-section slider must round to integer sides"));
        slideCrossSection(context, 0.0);
        context.runOnClient(client -> {
            check(XpOrbTrailsClient.CONFIG.crossSectionSides == 2, "Flat selection must use a billboard");
            verifyCrossSectionGeometry();
        });
        context.clickScreenButton("screen.xporbtrails.reset");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.crossSectionSides == 2,
                "Reset appearance must restore the camera-facing cross-section"));
        context.takeScreenshot("cross-section-slider");
        context.runOnClient(client -> {
            for (AbstractWidget widget : widgets(client.gui.screen())) {
                String label = widget.getMessage().getString();
                if (label.contains("#")) check(label.chars().filter(c -> c == '■').count() == 1,
                        "Individual color controls must show exactly one swatch");
            }
            for (int i = 0; i < 12; i++) XpOrbTrailsClient.CONFIG.savedPresets.add(
                    new TrailConfig.SavedPreset(net.minecraft.network.chat.Component.translatable(
                            "screen.xporbtrails.saved_preset_name", i + 1).getString(), i, i + 1));
        });
        context.clickScreenButton("screen.xporbtrails.common");
        context.clickScreenButton("screen.xporbtrails.appearance");
        context.runOnClient(client -> check(widgets(client.gui.screen()).stream().anyMatch(w ->
                w.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable(
                        "screen.xporbtrails.save_preset").getString()) && !w.active), "Saving must stop at twelve colors"));
        var beforeFold = configModifiedTime();
        click(context, "screen.xporbtrails.expand");
        check(configModifiedTime().equals(beforeFold), "Expanding a settings section must not write the config");
        click(context, "screen.xporbtrails.delete_color");
        check(configModifiedTime().equals(beforeFold), "Arming deletion must not write the config");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.savedPresets.size() == 12,
                "Deleting a color must require confirmation"));
        click(context, "screen.xporbtrails.confirm_delete_color");
        context.runOnClient(client -> {
            check(XpOrbTrailsClient.CONFIG.savedPresets.size() == 11, "Confirmed deletion must free a color slot");
            check(net.minecraft.network.chat.Component.translatable("screen.xporbtrails.saved_preset_name", 2)
                            .getString().equals(XpOrbTrailsClient.CONFIG.savedPresets.getFirst().name),
                    "Deletion must preserve the other saved colors");
        });
        click(context, "screen.xporbtrails.save_preset");
        context.runOnClient(client -> {
            check(XpOrbTrailsClient.CONFIG.savedPresets.size() == 12, "A freed color slot must be reusable");
            check(XpOrbTrailsClient.CONFIG.savedPresets.stream().map(p -> p.name).distinct().count() == 12,
                    "Saving after deletion must not duplicate an existing color name");
        });
        context.takeScreenshot("saved-colors-management");
        click(context, "screen.xporbtrails.mode.gradient");
        context.waitForScreen(TrailChoiceScreen.class);
        click(context, "screen.xporbtrails.mode.rainbow");
        context.waitForScreen(TrailConfigScreen.class);
        context.runOnClient(client -> {
            check("rainbow".equals(XpOrbTrailsClient.CONFIG.colorMode), "List selection must apply the selected color mode");
            check(widgets(client.gui.screen()).stream().noneMatch(w -> w.getMessage().getString().contains("#")),
                    "Rainbow mode must hide fixed color controls");
        });
        click(context, "screen.xporbtrails.mode.rainbow");
        click(context, "screen.xporbtrails.mode.solid");
        context.runOnClient(client -> check(widgets(client.gui.screen()).stream()
                .filter(w -> w.getMessage().getString().contains("#")).count() == 1,
                "Solid mode must show only one color control"));
        context.clickScreenButton("screen.xporbtrails.advanced");
        context.runOnClient(client -> check(widgets(client.gui.screen()).stream()
                .filter(w -> w instanceof AbstractSliderButton).count() == 3,
                "Position detail sliders must be collapsed initially"));
        click(context, "screen.xporbtrails.expand");
        context.runOnClient(client -> check(widgets(client.gui.screen()).stream()
                .filter(w -> w instanceof AbstractSliderButton).count() == 5,
                "Expanding position details must expose both offsets"));
        context.runOnClient(client -> {
            XpOrbTrailsClient.CONFIG.savedProfiles.add(new TrailConfig.SavedProfile("Regression", new TrailConfig()));
            XpOrbTrailsClient.CONFIG.width = 0.33;
            XpOrbTrailsClient.CONFIG.effectStrength = 0.91;
        });
        context.clickScreenButton("screen.xporbtrails.profiles");
        click(context, "screen.xporbtrails.manage_profile");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.width == 0.33,
                "Managing a profile must not overwrite unsaved settings"));
        click(context, "screen.xporbtrails.overwrite_profile");
        context.runOnClient(client -> {
            check(XpOrbTrailsClient.CONFIG.savedProfiles.getFirst().effectStrength == 0.91,
                    "Overwriting a profile must capture the merged strength");
            widgets(client.gui.screen()).stream().filter(w -> w instanceof net.minecraft.client.gui.components.EditBox)
                    .map(w -> (net.minecraft.client.gui.components.EditBox) w).findFirst().orElseThrow().setValue("Renamed");
        });
        click(context, "screen.xporbtrails.rename_profile");
        context.runOnClient(client -> check("Renamed".equals(XpOrbTrailsClient.CONFIG.savedProfiles.getFirst().name),
                "Renaming a saved profile must persist its new name"));
        click(context, "screen.xporbtrails.delete_profile");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.savedProfiles.size() == 1, "Delete must require confirmation"));
        click(context, "screen.xporbtrails.confirm_delete_profile");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.savedProfiles.isEmpty(), "Confirmed deletion must remove the profile"));
        context.runOnClient(client -> XpOrbTrailsClient.CONFIG.savedProfiles.add(new TrailConfig.SavedProfile(
                net.minecraft.network.chat.Component.translatable("screen.xporbtrails.saved_profile_name", 2).getString(), new TrailConfig())));
        context.clickScreenButton("screen.xporbtrails.common");
        context.clickScreenButton("screen.xporbtrails.profiles");
        click(context, "screen.xporbtrails.save_profile");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.savedProfiles.stream().map(p -> p.name).distinct().count() == 2,
                "Saving a profile must choose an unused name"));
        context.clickScreenButton("screen.xporbtrails.common");
        context.runOnClient(client -> {
            var list = (TrailSettingsList) client.gui.screen().children().stream().filter(w -> w instanceof TrailSettingsList).findFirst().orElseThrow();
            list.setScrollAmount(0);
            client.setLastInputType(net.minecraft.client.InputType.KEYBOARD_TAB);
            for (int i = 0; i < 30 && list.scrollAmount() == 0; i++) {
                client.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(
                        com.mojang.blaze3d.platform.InputConstants.KEY_TAB, '\t', 0));
            }
            check(list.scrollAmount() > 0, "Keyboard navigation must reveal controls below the viewport");
            client.options.guiScale().set(1);
            client.resizeGui();
            client.gui.screen().setFocused(null);
            client.setLastInputType(net.minecraft.client.InputType.MOUSE);
        });
        context.waitTicks(3);
        context.takeScreenshot("menu-common-wide");
        context.runOnClient(client -> { client.options.guiScale().set(2); client.resizeGui(); });
        var languageReload = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
        context.runOnClient(client -> {
            client.getLanguageManager().setSelected("zh_cn");
            client.options.languageCode = "zh_cn";
            languageReload.set(client.reloadResourcePacks());
        });
        context.waitFor(client -> languageReload.get().isDone() && client.gui.overlay() == null);
        context.waitTicks(5);
        context.takeScreenshot("menu-common-chinese");
        context.runOnClient(client -> {
            check("常用".equals(net.minecraft.network.chat.Component.translatable("screen.xporbtrails.common").getString()),
                    "Menu must follow the Minecraft language");
            client.getLanguageManager().setSelected("en_us");
            client.options.languageCode = "en_us";
            languageReload.set(client.reloadResourcePacks());
        });
        context.waitFor(client -> languageReload.get().isDone() && client.gui.overlay() == null);
        context.waitTicks(3);
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
            for (int sides : new int[]{2, 8, 32}) {
                context.runOnClient(client -> { XpOrbTrailsClient.CONFIG.crossSectionSides = sides; trails().clear(); });
                world.getServer().runCommand("execute at @p run summon minecraft:experience_orb ~ ~2 ~3 {Value:1,Motion:[0.1d,0.1d,0.0d]}");
                context.waitFor(client -> !snapshot().isEmpty());
                context.waitTicks(5);
                context.takeScreenshot("cross-section-" + sides);
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

    private static java.nio.file.attribute.FileTime configModifiedTime() {
        try {
            return java.nio.file.Files.getLastModifiedTime(net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getConfigDir().resolve("xp-orb-trails.json"));
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
    }

    private void verifyCrossSectionGeometry() {
        var tangent = new net.minecraft.world.phys.Vec3(1, 0, 0);
        for (var view : List.of(new net.minecraft.world.phys.Vec3(0, 0, 1),
                new net.minecraft.world.phys.Vec3(0, 1, 0), tangent)) {
            var right = TrailRenderer.billboardRight(tangent, view, null);
            check(Math.abs(right.lengthSqr() - 1) < 1e-9 && Math.abs(right.dot(tangent)) < 1e-9,
                    "Billboard must retain finite unit width even when viewed end-on");
            check(Math.abs(right.dot(view)) < 1e-9, "Billboard width must face the current camera");
        }
        try {
            Class<?> sampleType = Class.forName(TrailRenderer.class.getName() + "$Sample");
            var constructor = sampleType.getDeclaredConstructors()[0]; constructor.setAccessible(true);
            var samples = new ArrayList<>();
            for (int i = 0; i < 3; i++) samples.add(constructor.newInstance(new net.minecraft.world.phys.Vec3(i, 0, 0), 0L));
            var append = java.util.Arrays.stream(TrailRenderer.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("appendTube")).findFirst().orElseThrow();
            append.setAccessible(true);
            for (int sides : new int[]{2, 3, 8, 32}) {
                int[] vertices = {0};
                long[] fingerprint = {1};
                var out = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[]{com.mojang.blaze3d.vertex.VertexConsumer.class}, (proxy, method, args) -> {
                            if (method.getName().equals("addVertex") && args.length == 3) {
                                vertices[0]++;
                                for (Object coordinate : args) {
                                    float value = ((Number) coordinate).floatValue();
                                    check(Float.isFinite(value), "Cross-section vertices must be finite");
                                    fingerprint[0] = fingerprint[0] * 31 + Float.floatToIntBits(value);
                                }
                            }
                            if (method.getName().equals("setColor") && args.length == 1)
                                fingerprint[0] = fingerprint[0] * 31 + ((Number) args[0]).intValue();
                            return proxy;
                        });
                append.invoke(null, out, samples, new net.minecraft.world.phys.Vec3(0, 0, 3),
                        0L, 100L, 0.24, 0.78, 0xFFFFFF, 0xFFFFFF, "solid", 0.2, 1.0, 1.0, 1.0, sides);
                check(vertices[0] == 8 * (sides == 2 ? 1 : sides),
                        "Mesh must emit the selected face count without doubling the billboard");
                long expected = switch (sides) {
                    case 2 -> 6052789791297900145L;
                    case 3 -> 61044112680327065L;
                    case 8 -> 4513098808163761609L;
                    default -> -2688908051212465271L;
                };
                check(fingerprint[0] == expected, "Optimized vertices and colors must match the original mesh for " + sides + " sides");
            }
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static Map<?, ?> trails() { return (Map<?, ?>) field(null, "TRAILS"); }
    private static List<?> snapshot() { return (List<?>) field(null, "renderTrails"); }

    private static List<AbstractWidget> widgets(Screen screen) {
        List<AbstractWidget> result = new ArrayList<>();
        collect(screen, result);
        return result;
    }

    private static void click(ClientGameTestContext context, String key) {
        String label = net.minecraft.network.chat.Component.translatable(key).getString();
        context.runOnClient(client -> {
            AbstractWidget target = widgets(client.gui.screen()).stream()
                    .filter(w -> w.getMessage().getString().equals(label)).findFirst().orElseThrow();
            for (var child : client.gui.screen().children()) {
                if (child instanceof TrailSettingsList list) {
                    for (var row : list.children()) {
                        if (row.children().contains(target)) list.setScrollAmount(list.scrollAmount() + row.getY() - list.getY() - 8);
                    }
                }
            }
        });
        context.waitTicks(2);
        context.runOnClient(client -> {
            Screen screen = client.gui.screen();
            AbstractWidget target = widgets(screen).stream().filter(w -> w.getMessage().getString().equals(label)).findFirst().orElseThrow();
            var event = new net.minecraft.client.input.MouseButtonEvent(target.getX() + target.getWidth() / 2.0,
                    target.getY() + target.getHeight() / 2.0,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
            if (!screen.mouseClicked(event, false)) throw new AssertionError("Control did not accept click: " + key);
            screen.mouseReleased(event);
            System.out.println("Menu click " + key + " at " + event.x() + "," + event.y()
                    + " -> " + client.gui.screen().getClass().getSimpleName());
        });
        context.waitTicks(2);
    }

    private static void slideCrossSection(ClientGameTestContext context, double fraction) {
        context.runOnClient(client -> {
            var slider = widgets(client.gui.screen()).stream().filter(w -> w instanceof AbstractSliderButton
                    && w.getMessage().getString().equals(TrailConfigScreen.crossSectionLabel(
                            XpOrbTrailsClient.CONFIG.crossSectionSides).getString())).findFirst().orElseThrow();
            for (var child : client.gui.screen().children()) if (child instanceof TrailSettingsList list) {
                for (var row : list.children()) if (row.children().contains(slider))
                    list.setScrollAmount(list.scrollAmount() + row.getY() - list.getY() - 8);
            }
        });
        context.waitTicks(2);
        context.runOnClient(client -> {
            var slider = widgets(client.gui.screen()).stream().filter(w -> w instanceof AbstractSliderButton
                    && w.getMessage().getString().equals(TrailConfigScreen.crossSectionLabel(
                            XpOrbTrailsClient.CONFIG.crossSectionSides).getString())).findFirst().orElseThrow();
            var event = new net.minecraft.client.input.MouseButtonEvent(slider.getX() + 4 + (slider.getWidth() - 8) * fraction,
                    slider.getY() + 10, new net.minecraft.client.input.MouseButtonInfo(
                            com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
            if (!client.gui.screen().mouseClicked(event, false)) throw new AssertionError("Slider did not accept click");
            client.gui.screen().mouseReleased(event);
        });
        context.waitTicks(2);
    }

    private static void collect(ContainerEventHandler parent, List<AbstractWidget> result) {
        for (GuiEventListener child : parent.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler container) collect(container, result);
        }
    }

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
