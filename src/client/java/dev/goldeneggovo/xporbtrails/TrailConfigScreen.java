package dev.goldeneggovo.xporbtrails;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.DoubleConsumer;

public final class TrailConfigScreen extends Screen {
    private enum Page { APPEARANCE, ANIMATION, PICKUP, PERFORMANCE, PROFILES }
    private record Preset(String name, boolean translated, int start, int end) { }
    private record ShapePreset(String key, double tail, double middle, double head) { }
    private record ProfileChoice(String label, boolean translated, TrailConfig.SavedProfile profile, boolean saved) { }
    private static final List<Preset> PRESETS = List.of(
            new Preset("screen.xporbtrails.preset.custom", true, -1, -1),
            new Preset("screen.xporbtrails.preset.classic_green", true, 0xFFF23A, 0x45FF00),
            new Preset("screen.xporbtrails.preset.gold", true, 0xFFF4A0, 0xFF9D00),
            new Preset("screen.xporbtrails.preset.blue_purple", true, 0x74F4FF, 0xA45CFF));
    private static final List<ShapePreset> SHAPES = List.of(
            new ShapePreset("custom", -1, -1, -1),
            new ShapePreset("taper", 0.0, 0.62, 1.0),
            new ShapePreset("spindle", 0.12, 1.25, 0.42),
            new ShapePreset("uniform", 1.0, 1.0, 1.0),
            new ShapePreset("hourglass", 0.78, 0.25, 1.0));

    private final Screen parent;
    private Page page = Page.APPEARANCE;
    private int presetIndex;
    private int shapeIndex;
    private int profileIndex;
    private int deleteArmedIndex = -1;
    private EditBox startColor;
    private EditBox endColor;

    public TrailConfigScreen(Screen parent) {
        super(Component.translatable("screen.xporbtrails.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.appearance"), b -> switchPage(Page.APPEARANCE)).bounds(center - 159, 28, 62, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.animation"), b -> switchPage(Page.ANIMATION)).bounds(center - 95, 28, 62, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.pickup"), b -> switchPage(Page.PICKUP)).bounds(center - 31, 28, 62, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.performance"), b -> switchPage(Page.PERFORMANCE)).bounds(center + 33, 28, 62, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.profiles"), b -> switchPage(Page.PROFILES)).bounds(center + 97, 28, 62, 20).build());
        if (page == Page.APPEARANCE) buildAppearance(center);
        else if (page == Page.ANIMATION) buildAnimation(center);
        else if (page == Page.PICKUP) buildPickup(center);
        else if (page == Page.PERFORMANCE) buildPerformance(center);
        else buildProfiles(center);
        int footerY = height - 28;
        Button reset = Button.builder(Component.translatable("screen.xporbtrails.reset"), b -> resetCurrentPage())
                .bounds(center - 154, footerY, 98, 20).build();
        reset.active = page != Page.PROFILES;
        addRenderableWidget(reset);
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.open_preview"), b -> {
            XpOrbTrailsClient.saveConfig();
            minecraft.gui.setScreen(new TrailPreviewScreen(this));
        }).bounds(center - 49, footerY, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(center + 56, footerY, 98, 20).build());
    }

    private void buildAppearance(int center) {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        int left = center - 154, right = center + 4;
        addRenderableWidget(withTip(CycleButton.onOffBuilder(c.enabled).create(left, 58, 150, 20, Component.translatable("screen.xporbtrails.enabled"), (b, v) -> c.enabled = v), "screen.xporbtrails.enabled.tip"));
        addRenderableWidget(withTip(CycleButton.onOffBuilder(c.additiveGlow).create(right, 58, 150, 20, Component.translatable("screen.xporbtrails.glow"), (b, v) -> c.additiveGlow = v), "screen.xporbtrails.glow.tip"));
        startColor = colorBox(left, 90, "screen.xporbtrails.start_color", c.startColor, v -> c.startColor = v);
        endColor = colorBox(right, 90, "screen.xporbtrails.end_color", c.endColor, v -> c.endColor = v);
        addRenderableWidget(startColor);
        addRenderableWidget(endColor);
        addSlider(left, 122, "screen.xporbtrails.width", c.width, 0.05, 0.65, 2, v -> c.width = v);
        addSlider(right, 122, "screen.xporbtrails.opacity", c.opacity, 0.05, 1.0, 2, v -> c.opacity = v);

        List<String> modes = List.of("solid", "gradient", "rainbow");
        addRenderableWidget(withTip(CycleButton.<String>builder(v -> Component.translatable("screen.xporbtrails.mode." + v), c.colorMode)
                .withValues(modes).create(left, 154, 150, 20, Component.translatable("screen.xporbtrails.color_mode"),
                        (b, value) -> { c.colorMode = value; rebuildWidgets(); }), "screen.xporbtrails.color_mode.tip"));
        ConfigSlider rainbow = addSlider(right, 154, "screen.xporbtrails.rainbow_speed", c.rainbowSpeed, 0.02, 1.0, 2, v -> c.rainbowSpeed = v);
        rainbow.active = "rainbow".equals(c.colorMode);

        List<Preset> allPresets = new ArrayList<>(PRESETS);
        for (TrailConfig.SavedPreset saved : c.savedPresets) allPresets.add(new Preset(saved.name, false, saved.startColor, saved.endColor));
        if (presetIndex >= allPresets.size()) presetIndex = 0;
        addRenderableWidget(CycleButton.<Preset>builder(p -> p.translated() ? Component.translatable(p.name()) : Component.literal(p.name()), allPresets.get(presetIndex)).withValues(allPresets)
                .create(left, 186, 218, 20, Component.translatable("screen.xporbtrails.preset"), (b, preset) -> {
                    presetIndex = allPresets.indexOf(preset);
                    if (preset.start() >= 0) {
                        c.startColor = preset.start(); c.endColor = preset.end();
                        startColor.setValue(hex(c.startColor)); endColor.setValue(hex(c.endColor));
                    }
                }));
        addRenderableWidget(withTip(Button.builder(Component.translatable("screen.xporbtrails.save_preset"), b -> {
            if (c.savedPresets.size() < 12) {
                c.savedPresets.add(new TrailConfig.SavedPreset(Component.translatable("screen.xporbtrails.saved_preset_name", c.savedPresets.size() + 1).getString(), c.startColor, c.endColor));
                presetIndex = PRESETS.size() + c.savedPresets.size() - 1;
                XpOrbTrailsClient.saveConfig();
                rebuildWidgets();
            }
        }).bounds(left + 222, 186, 86, 20).build(), "screen.xporbtrails.save_preset.tip"));
    }

    private void buildAnimation(int center) {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        int left = center - 154, right = center + 4;
        addSlider(left, 58, "screen.xporbtrails.lifetime", c.lifetimeSeconds, 0.3, 6.0, 1, v -> c.lifetimeSeconds = v);
        addSlider(right, 58, "screen.xporbtrails.pickup_fade", c.pickupFadeSeconds, 0.05, 1.5, 2, v -> c.pickupFadeSeconds = v);
        addSlider(left, 90, "screen.xporbtrails.smoothness", c.smoothFlow, 0.0, 1.0, 2, v -> c.smoothFlow = v);
        addSlider(right, 90, "screen.xporbtrails.motion_shift", c.motionShift, 0.0, 0.5, 2, v -> c.motionShift = v);
        addSlider(left, 122, "screen.xporbtrails.camera_push", c.cameraPush, 0.0, 0.35, 2, v -> c.cameraPush = v);
        addSlider(right, 122, "screen.xporbtrails.glow_strength", c.glowStrength, 0.1, 2.0, 2, v -> c.glowStrength = v);
        addSlider(left, 154, "screen.xporbtrails.tail_width", c.tailWidthScale, 0.0, 1.0, 2, v -> c.tailWidthScale = v);
        addSlider(right, 154, "screen.xporbtrails.middle_width", c.middleWidthScale, 0.0, 2.0, 2, v -> c.middleWidthScale = v);
        addSlider(left, 186, "screen.xporbtrails.head_width", c.headWidthScale, 0.1, 2.0, 2, v -> c.headWidthScale = v);
        if (shapeIndex >= SHAPES.size()) shapeIndex = 0;
        addRenderableWidget(withTip(CycleButton.<ShapePreset>builder(
                shape -> Component.translatable("screen.xporbtrails.shape." + shape.key()), SHAPES.get(shapeIndex))
                .withValues(SHAPES).create(right, 186, 150, 20, Component.translatable("screen.xporbtrails.shape_preset"), (b, shape) -> {
                    shapeIndex = SHAPES.indexOf(shape);
                    if (shape.tail() >= 0.0) {
                        c.tailWidthScale = shape.tail();
                        c.middleWidthScale = shape.middle();
                        c.headWidthScale = shape.head();
                    }
                    rebuildWidgets();
                }), "screen.xporbtrails.shape_preset.tip"));
    }

    private void buildPickup(int center) {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        int left = center - 154, right = center + 4;
        addRenderableWidget(withTip(CycleButton.onOffBuilder(c.pickupFlash).create(left, 72, 150, 20,
                Component.translatable("screen.xporbtrails.pickup_flash"), (b, v) -> { c.pickupFlash = v; rebuildWidgets(); }),
                "screen.xporbtrails.pickup_flash.tip"));
        ConfigSlider strength = addSlider(right, 72, "screen.xporbtrails.pickup_flash_strength", c.pickupFlashStrength, 0.1, 2.0, 2, v -> c.pickupFlashStrength = v);
        ConfigSlider duration = addSlider(left, 104, "screen.xporbtrails.pickup_flash_duration", c.pickupFlashSeconds, 0.08, 1.0, 2, v -> c.pickupFlashSeconds = v);
        ConfigSlider size = addSlider(right, 104, "screen.xporbtrails.pickup_flash_size", c.pickupFlashSize, 0.25, 2.0, 2, v -> c.pickupFlashSize = v);
        List<String> styles = List.of("soft", "star", "ring");
        CycleButton<String> style = withTip(CycleButton.<String>builder(v -> Component.translatable("screen.xporbtrails.flash_style." + v), c.pickupFlashStyle)
                .withValues(styles).create(left, 136, 150, 20, Component.translatable("screen.xporbtrails.flash_style"),
                        (b, value) -> c.pickupFlashStyle = value), "screen.xporbtrails.flash_style.tip");
        addRenderableWidget(style);
        strength.active = c.pickupFlash;
        duration.active = c.pickupFlash;
        size.active = c.pickupFlash;
        style.active = c.pickupFlash;
    }

    private void buildProfiles(int center) {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        int left = center - 154;
        List<ProfileChoice> profiles = allProfiles(c);
        if (profileIndex >= profiles.size()) profileIndex = 0;
        CycleButton<ProfileChoice> selector = CycleButton.<ProfileChoice>builder(this::profileName, profiles.get(profileIndex))
                .withValues(profiles).create(left, 72, 218, 20, Component.translatable("screen.xporbtrails.profile"),
                        (b, selected) -> { profileIndex = profiles.indexOf(selected); deleteArmedIndex = -1; rebuildWidgets(); });
        addRenderableWidget(selector);
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.apply_profile"), b -> {
            List<ProfileChoice> current = allProfiles(c);
            if (profileIndex < current.size()) current.get(profileIndex).profile().applyTo(c);
            XpOrbTrailsClient.saveConfig();
            rebuildWidgets();
        }).bounds(left + 222, 72, 86, 20).build());
        Button save = withTip(Button.builder(Component.translatable("screen.xporbtrails.save_profile"), b -> {
            if (c.savedProfiles.size() < 8) {
                c.savedProfiles.add(new TrailConfig.SavedProfile(Component.translatable("screen.xporbtrails.saved_profile_name", c.savedProfiles.size() + 1).getString(), c));
                profileIndex = 4 + c.savedProfiles.size() - 1;
                XpOrbTrailsClient.saveConfig();
                rebuildWidgets();
            }
        }).bounds(left, 104, 98, 20).build(), "screen.xporbtrails.save_profile.tip");
        save.active = c.savedProfiles.size() < 8;
        addRenderableWidget(save);
        Button overwrite = withTip(Button.builder(Component.translatable("screen.xporbtrails.overwrite_profile"), b -> {
            int savedIndex = profileIndex - 4;
            if (savedIndex >= 0 && savedIndex < c.savedProfiles.size()) {
                String name = c.savedProfiles.get(savedIndex).name;
                c.savedProfiles.set(savedIndex, new TrailConfig.SavedProfile(name, c));
                XpOrbTrailsClient.saveConfig();
                rebuildWidgets();
            }
        }).bounds(left + 105, 104, 98, 20).build(), "screen.xporbtrails.overwrite_profile.tip");
        overwrite.active = profileIndex >= 4;
        addRenderableWidget(overwrite);
        Component deleteLabel = deleteArmedIndex == profileIndex
                ? Component.translatable("screen.xporbtrails.confirm_delete_profile")
                : Component.translatable("screen.xporbtrails.delete_profile");
        Button delete = withTip(Button.builder(deleteLabel, b -> {
            int savedIndex = profileIndex - 4;
            if (savedIndex >= 0 && savedIndex < c.savedProfiles.size()) {
                if (deleteArmedIndex != profileIndex) {
                    deleteArmedIndex = profileIndex;
                    rebuildWidgets();
                    return;
                }
                c.savedProfiles.remove(savedIndex);
                profileIndex = 0;
                deleteArmedIndex = -1;
                XpOrbTrailsClient.saveConfig();
                rebuildWidgets();
            }
        }).bounds(left + 210, 104, 98, 20).build(), "screen.xporbtrails.delete_profile.tip");
        delete.active = profileIndex >= 4;
        addRenderableWidget(delete);

        int savedIndex = profileIndex - 4;
        EditBox rename = new EditBox(font, left, 136, 218, 20, Component.translatable("screen.xporbtrails.profile_name"));
        rename.setMaxLength(32);
        if (savedIndex >= 0 && savedIndex < c.savedProfiles.size()) rename.setValue(c.savedProfiles.get(savedIndex).name);
        rename.active = savedIndex >= 0;
        addRenderableWidget(rename);
        Button renameButton = withTip(Button.builder(Component.translatable("screen.xporbtrails.rename_profile"), b -> {
            int index = profileIndex - 4;
            String value = rename.getValue().trim();
            if (index >= 0 && index < c.savedProfiles.size() && !value.isEmpty()) {
                c.savedProfiles.get(index).name = value;
                XpOrbTrailsClient.saveConfig();
                rebuildWidgets();
            }
        }).bounds(left + 222, 136, 86, 20).build(), "screen.xporbtrails.rename_profile.tip");
        renameButton.active = savedIndex >= 0;
        addRenderableWidget(renameButton);
    }

    private List<ProfileChoice> allProfiles(TrailConfig c) {
        List<ProfileChoice> result = new ArrayList<>();
        TrailConfig standard = new TrailConfig();
        result.add(new ProfileChoice("screen.xporbtrails.profile.standard", true, new TrailConfig.SavedProfile("Standard", standard), false));
        TrailConfig soft = new TrailConfig();
        soft.additiveGlow = false; soft.width = 0.18; soft.opacity = 0.58; soft.glowStrength = 0.75;
        soft.pickupFlashStrength = 0.35; soft.pickupFlashSize = 0.48;
        result.add(new ProfileChoice("screen.xporbtrails.profile.soft", true, new TrailConfig.SavedProfile("Soft", soft), false));
        TrailConfig neon = new TrailConfig();
        neon.startColor = 0x62F4FF; neon.endColor = 0xB45CFF; neon.width = 0.27; neon.opacity = 0.9; neon.glowStrength = 1.3;
        neon.tailWidthScale = 0.08; neon.middleWidthScale = 1.2; neon.headWidthScale = 0.55; neon.pickupFlashStyle = "star"; neon.pickupFlashStrength = 0.9;
        result.add(new ProfileChoice("screen.xporbtrails.profile.neon", true, new TrailConfig.SavedProfile("Neon", neon), false));
        TrailConfig rainbow = new TrailConfig();
        rainbow.colorMode = "rainbow"; rainbow.rainbowSpeed = 0.22; rainbow.tailWidthScale = 0.1; rainbow.middleWidthScale = 1.1; rainbow.headWidthScale = 0.5;
        rainbow.pickupFlashStyle = "ring"; rainbow.pickupFlashStrength = 0.7;
        result.add(new ProfileChoice("screen.xporbtrails.profile.rainbow", true, new TrailConfig.SavedProfile("Rainbow", rainbow), false));
        for (TrailConfig.SavedProfile saved : c.savedProfiles) result.add(new ProfileChoice(saved.name, false, saved, true));
        return result;
    }

    private Component profileName(ProfileChoice choice) {
        return choice.translated() ? Component.translatable(choice.label()) : Component.literal(choice.label());
    }

    private void resetCurrentPage() {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        TrailConfig defaults = new TrailConfig();
        switch (page) {
            case APPEARANCE -> {
                c.enabled = defaults.enabled;
                c.additiveGlow = defaults.additiveGlow;
                c.startColor = defaults.startColor;
                c.endColor = defaults.endColor;
                c.width = defaults.width;
                c.opacity = defaults.opacity;
                c.colorMode = defaults.colorMode;
                c.rainbowSpeed = defaults.rainbowSpeed;
                presetIndex = 0;
            }
            case ANIMATION -> {
                c.lifetimeSeconds = defaults.lifetimeSeconds;
                c.pickupFadeSeconds = defaults.pickupFadeSeconds;
                c.smoothFlow = defaults.smoothFlow;
                c.motionShift = defaults.motionShift;
                c.cameraPush = defaults.cameraPush;
                c.glowStrength = defaults.glowStrength;
                c.tailWidthScale = defaults.tailWidthScale;
                c.middleWidthScale = defaults.middleWidthScale;
                c.headWidthScale = defaults.headWidthScale;
                shapeIndex = 0;
            }
            case PICKUP -> {
                c.pickupFlash = defaults.pickupFlash;
                c.pickupFlashStrength = defaults.pickupFlashStrength;
                c.pickupFlashSeconds = defaults.pickupFlashSeconds;
                c.pickupFlashSize = defaults.pickupFlashSize;
            }
            case PERFORMANCE -> {
                c.renderRange = defaults.renderRange;
                c.trailCap = defaults.trailCap;
            }
            case PROFILES -> { }
        }
        XpOrbTrailsClient.saveConfig();
        rebuildWidgets();
    }

    private void buildPerformance(int center) {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        int left = center - 154, right = center + 4;
        addSlider(left, 72, "screen.xporbtrails.range", c.renderRange, 8.0, 96.0, 0, v -> c.renderRange = v);
        addSlider(right, 72, "screen.xporbtrails.cap", c.trailCap, 8.0, 256.0, 0, v -> c.trailCap = (int) Math.round(v));
    }

    private EditBox colorBox(int x, int y, String label, int color, java.util.function.IntConsumer setter) {
        EditBox box = new EditBox(font, x, y, 150, 20, Component.translatable(label));
        box.setMaxLength(7);
        box.setValue(hex(color));
        box.setResponder(text -> {
            String value = text.startsWith("#") ? text.substring(1) : text;
            if (value.matches("[0-9a-fA-F]{6}")) setter.accept(Integer.parseInt(value, 16));
        });
        return box;
    }

    private ConfigSlider addSlider(int x, int y, String key, double current, double min, double max, int decimals, DoubleConsumer setter) {
        ConfigSlider slider = new ConfigSlider(x, y, 150, 20, key, current, min, max, decimals, setter);
        slider.setTooltip(Tooltip.create(Component.translatable(key + ".tip")));
        addRenderableWidget(slider);
        return slider;
    }

    private static <T extends AbstractWidget> T withTip(T widget, String key) {
        widget.setTooltip(Tooltip.create(Component.translatable(key)));
        return widget;
    }

    private void switchPage(Page next) { if (page != next) { page = next; rebuildWidgets(); } }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (page == Page.APPEARANCE) {
            int center = width / 2;
            if (inside(event.x(), event.y(), center - 178, 90, 20, 20)) {
                minecraft.gui.setScreen(new ColorPickerScreen(this, XpOrbTrailsClient.CONFIG.startColor,
                        value -> XpOrbTrailsClient.CONFIG.startColor = value));
                return true;
            }
            if (inside(event.x(), event.y(), center + 162, 90, 20, 20)) {
                minecraft.gui.setScreen(new ColorPickerScreen(this, XpOrbTrailsClient.CONFIG.endColor,
                        value -> XpOrbTrailsClient.CONFIG.endColor = value));
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        if (page == Page.APPEARANCE) {
            int center = width / 2;
            graphics.text(font, Component.translatable("screen.xporbtrails.start_color"), center - 154, 80, 0xFFA0A0A0);
            graphics.text(font, Component.translatable("screen.xporbtrails.end_color"), center + 4, 80, 0xFFA0A0A0);
            graphics.fill(center - 178, 90, center - 158, 110, 0xFF000000 | XpOrbTrailsClient.CONFIG.startColor);
            graphics.outline(center - 178, 90, 20, 20, 0xFFFFFFFF);
            graphics.fill(center + 162, 90, center + 182, 110, 0xFF000000 | XpOrbTrailsClient.CONFIG.endColor);
            graphics.outline(center + 162, 90, 20, 20, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        XpOrbTrailsClient.saveConfig();
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
    private static boolean inside(double x, double y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }
    private static String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF); }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final String key; private final double min, max; private final int decimals; private final DoubleConsumer setter;
        ConfigSlider(int x, int y, int width, int height, String key, double current, double min, double max, int decimals, DoubleConsumer setter) {
            super(x, y, width, height, Component.empty(), (current - min) / (max - min));
            this.key = key; this.min = min; this.max = max; this.decimals = decimals; this.setter = setter; updateMessage();
        }
        private double actual() { return min + value * (max - min); }
        @Override protected void updateMessage() { setMessage(Component.translatable(key).append(": " + String.format(Locale.ROOT, "%." + decimals + "f", actual()))); }
        @Override protected void applyValue() { setter.accept(actual()); }
    }
}
