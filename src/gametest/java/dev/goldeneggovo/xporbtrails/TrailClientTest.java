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
        context.runOnClient(client -> {
            for (AbstractWidget widget : widgets(client.gui.screen())) {
                String label = widget.getMessage().getString();
                if (label.contains("#")) check(label.chars().filter(c -> c == '■').count() == 1,
                        "Individual color controls must show exactly one swatch");
            }
            for (int i = 0; i < 12; i++) XpOrbTrailsClient.CONFIG.savedPresets.add(
                    new TrailConfig.SavedPreset("Color " + i, i, i + 1));
        });
        context.clickScreenButton("screen.xporbtrails.common");
        context.clickScreenButton("screen.xporbtrails.appearance");
        context.runOnClient(client -> check(widgets(client.gui.screen()).stream().anyMatch(w ->
                w.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable(
                        "screen.xporbtrails.save_preset").getString()) && !w.active), "Saving must stop at twelve colors"));
        click(context, "screen.xporbtrails.expand");
        click(context, "screen.xporbtrails.delete_color");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.savedPresets.size() == 12,
                "Deleting a color must require confirmation"));
        click(context, "screen.xporbtrails.confirm_delete_color");
        context.runOnClient(client -> {
            check(XpOrbTrailsClient.CONFIG.savedPresets.size() == 11, "Confirmed deletion must free a color slot");
            check("Color 1".equals(XpOrbTrailsClient.CONFIG.savedPresets.getFirst().name),
                    "Deletion must preserve the other saved colors");
        });
        click(context, "screen.xporbtrails.save_preset");
        context.runOnClient(client -> check(XpOrbTrailsClient.CONFIG.savedPresets.size() == 12,
                "A freed color slot must be reusable"));
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
