package dev.goldeneggovo.xporbtrails;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public final class TrailConfigScreen extends Screen {
    private enum Page { COMMON, APPEARANCE, ADVANCED, PROFILES }
    private record Preset(String key, int start, int end) { }
    private record Shape(String key, double tail, double middle, double head) { }
    private record Profile(Component name, TrailConfig.SavedProfile settings, int savedIndex) { }
    private static final List<Preset> COLORS = List.of(
            new Preset("classic_green", 0xFFF23A, 0x45FF00),
            new Preset("gold", 0xFFF4A0, 0xFF9D00),
            new Preset("blue_purple", 0x74F4FF, 0xA45CFF));
    private static final List<Shape> SHAPES = List.of(
            new Shape("taper", 0.0, 0.62, 1.0), new Shape("spindle", 0.12, 1.25, 0.42),
            new Shape("uniform", 1.0, 1.0, 1.0), new Shape("hourglass", 0.78, 0.25, 1.0));
    private final Screen parent;
    private final TrailPreviewScreen preview;
    private Page page = Page.COMMON;
    private TrailSettingsList list;
    private double scroll;
    private boolean customShape, pickupDetails, positionDetails;
    private boolean savedColorsExpanded;
    private int deleteColorIndex = -1;
    private int selectedProfile = -1;
    private int deleteArmedIndex = -1;
    private Component appliedProfile;
    private Button profileButton;
    private Button shapeButton;
    private boolean wide;
    private int panelX, panelWidth, previewX, previewWidth;

    public TrailConfigScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
        preview = new TrailPreviewScreen(this);
    }
    private static MutableComponent text(String key) { return Component.translatable("screen.xporbtrails." + key); }

    @Override protected void init() {
        profileButton = null;
        shapeButton = null;
        wide = width >= 680;
        int total = Math.min(width - 24, wide ? 850 : 460);
        panelX = (width - total) / 2;
        panelWidth = wide ? total * 3 / 5 : total;
        previewX = panelX + panelWidth + 12;
        previewWidth = total - panelWidth - 12;
        int tabWidth = (panelWidth - 12) / 4;
        for (Page tab : Page.values()) {
            Button button = Button.builder(text(tab.name().toLowerCase(Locale.ROOT)), b -> {
                page = tab; scroll = 0; deleteColorIndex = -1; rebuildWidgets();
            }).bounds(panelX + tab.ordinal() * (tabWidth + 4), 28, tabWidth, 20).build();
            button.active = tab != page;
            addRenderableWidget(button);
        }
        list = addRenderableWidget(new TrailSettingsList(panelX, 56, panelWidth, height - 96));
        switch (page) {
            case COMMON -> common();
            case APPEARANCE -> appearance();
            case ADVANCED -> advanced();
            case PROFILES -> profiles();
        }
        list.setScrollAmount(scroll);
        int fw = (panelWidth - 12) / 3;
        Button reset = Button.builder(text("reset"), b -> resetPage()).bounds(panelX, height - 28, fw, 20).build();
        reset.active = page != Page.PROFILES;
        addRenderableWidget(reset);
        addRenderableWidget(Button.builder(text("open_preview"), b -> {
            rememberScroll(); XpOrbTrailsClient.saveConfig(); minecraft.gui.setScreen(preview);
        }).bounds(panelX + fw + 6, height - 28, fw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(panelX + 2 * (fw + 6), height - 28, fw, 20).build());
        if (wide) preview.init(width, height);
    }

    private void common() {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        toggle("enabled", c.enabled, v -> c.enabled = v);
        profileButton = button(appliedProfile == null ? text("preset.custom") : appliedProfile, () -> {
            List<TrailChoiceScreen.Choice> choices = new ArrayList<>();
            for (Profile p : allProfiles()) choices.add(new TrailChoiceScreen.Choice(p.name(), () -> apply(p)));
            choose("profile", choices);
        });
        row("profile", profileButton);
        section("trail");
        slider("width", c.width, 0.02, 1.0, 2, v -> c.width = v);
        slider("lifetime", c.lifetimeSeconds, 0.25, 10.0, 2, v -> c.lifetimeSeconds = v);
        slider("effect_strength", c.effectStrength, 0.005, 2.0, 2, v -> c.effectStrength = v);
        toggle("pickup_flash", c.pickupFlash, v -> c.pickupFlash = v);
    }

    private void appearance() {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        section("colors");
        select("color_mode", "mode.", c.colorMode, List.of("solid", "gradient", "rainbow"), v -> c.colorMode = v);
        if ("rainbow".equals(c.colorMode)) {
            slider("rainbow_speed", c.rainbowSpeed, 0.02, 2.0, 2, v -> c.rainbowSpeed = v);
        } else {
            color("start_color", c.startColor, v -> c.startColor = v);
            if ("gradient".equals(c.colorMode)) color("end_color", c.endColor, v -> c.endColor = v);
            row("preset", button(text("choose"), () -> {
                List<TrailChoiceScreen.Choice> choices = new ArrayList<>();
                for (Preset p : COLORS) choices.add(new TrailChoiceScreen.Choice(
                        colorLabel(text("preset." + p.key()), p.start(), p.end()), () -> setColors(p.start(), p.end())));
                for (TrailConfig.SavedPreset p : c.savedPresets) choices.add(new TrailChoiceScreen.Choice(
                        colorLabel(Component.literal(p.name), p.startColor, p.endColor), () -> setColors(p.startColor, p.endColor)));
                choose("preset", choices);
            }));
            Button save = button(text("save_preset"), () -> {
                if (c.savedPresets.size() >= 12) return;
                c.savedPresets.add(new TrailConfig.SavedPreset(
                        Component.translatable("screen.xporbtrails.saved_preset_name", c.savedPresets.size() + 1).getString(), c.startColor, c.endColor));
                refresh();
            });
            save.active = c.savedPresets.size() < 12;
            list.row(Component.empty(), save);
        }
        fold("saved_colors", savedColorsExpanded, () -> {
            savedColorsExpanded = !savedColorsExpanded; deleteColorIndex = -1; refresh();
        });
        if (savedColorsExpanded) {
            for (int i = 0; i < c.savedPresets.size(); i++) {
                int index = i;
                TrailConfig.SavedPreset preset = c.savedPresets.get(i);
                list.row(colorLabel(Component.literal(preset.name), preset.startColor, preset.endColor),
                        button(text(deleteColorIndex == index ? "confirm_delete_color" : "delete_color"), () -> {
                            if (deleteColorIndex != index) { deleteColorIndex = index; refresh(); return; }
                            c.savedPresets.remove(index); deleteColorIndex = -1; refresh();
                        }));
            }
        }
        toggle("glow", c.additiveGlow, v -> c.additiveGlow = v);
        section("shape");
        shapeButton = button(shapeName(), () -> {
            List<TrailChoiceScreen.Choice> choices = new ArrayList<>();
            for (Shape s : SHAPES) choices.add(new TrailChoiceScreen.Choice(text("shape." + s.key()), () -> {
                c.tailWidthScale = s.tail(); c.middleWidthScale = s.middle(); c.headWidthScale = s.head(); changed();
            }));
            choose("shape_preset", choices);
        });
        row("shape_preset", shapeButton);
        fold("custom_shape", customShape, () -> { customShape = !customShape; refresh(); });
        if (customShape) {
            slider("tail_width", c.tailWidthScale, 0.0, 1.5, 2, v -> c.tailWidthScale = v);
            slider("middle_width", c.middleWidthScale, 0.0, 2.0, 2, v -> c.middleWidthScale = v);
            slider("head_width", c.headWidthScale, 0.05, 2.0, 2, v -> c.headWidthScale = v);
        }
        section("pickup");
        if (!c.pickupFlash) {
            row("pickup_flash", button(text("enable"), () -> { c.pickupFlash = true; changed(); refresh(); }));
        } else {
            select("flash_style", "flash_style.", c.pickupFlashStyle, List.of("soft", "star", "ring"), v -> c.pickupFlashStyle = v);
            fold("pickup_details", pickupDetails, () -> { pickupDetails = !pickupDetails; refresh(); });
            if (pickupDetails) {
                slider("pickup_flash_strength", c.pickupFlashStrength, 0.1, 2.0, 2, v -> c.pickupFlashStrength = v);
                slider("pickup_flash_size", c.pickupFlashSize, 0.02, 8.34, 2, v -> c.pickupFlashSize = v);
                slider("pickup_flash_duration", c.pickupFlashSeconds, 0.08, 1.0, 2, v -> c.pickupFlashSeconds = v);
            }
        }
        slider("pickup_fade", c.pickupFadeSeconds, 0.05, 3.0, 2, v -> c.pickupFadeSeconds = v);
    }

    private void advanced() {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        section("animation");
        slider("smoothness", c.smoothFlow, 0.0, 1.0, 2, v -> c.smoothFlow = v);
        fold("position_details", positionDetails, () -> { positionDetails = !positionDetails; refresh(); });
        if (positionDetails) {
            slider("motion_shift", c.motionShift, 0.0, 1.0, 2, v -> c.motionShift = v);
            slider("camera_push", c.cameraPush, 0.0, 1.0, 2, v -> c.cameraPush = v);
        }
        section("performance");
        slider("range", c.renderRange, 4.0, 128.0, 0, v -> c.renderRange = v);
        slider("cap", c.trailCap, 8.0, 256.0, 0, v -> c.trailCap = (int) Math.round(v));
    }

    private void profiles() {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        section("built_in_profiles");
        List<Profile> profiles = allProfiles();
        for (Profile p : profiles.subList(0, 4)) list.row(p.name(), button(text("apply_profile"), () -> { apply(p); refresh(); }));
        section("saved_profiles");
        for (Profile p : profiles.subList(4, profiles.size())) list.row(p.name(), button(text("manage_profile"), () -> {
            selectedProfile = p.savedIndex(); deleteArmedIndex = -1; refresh();
            list.setScrollAmount(list.maxScrollAmount());
        }));
        Button save = button(text("save_profile"), () -> {
            if (c.savedProfiles.size() >= 8) return;
            c.savedProfiles.add(new TrailConfig.SavedProfile(
                    Component.translatable("screen.xporbtrails.saved_profile_name", c.savedProfiles.size() + 1).getString(), c));
            selectedProfile = c.savedProfiles.size() - 1; deleteArmedIndex = -1; refresh();
            list.setScrollAmount(list.maxScrollAmount());
        });
        save.active = c.savedProfiles.size() < 8;
        list.row(Component.empty(), save);
        if (selectedProfile < 0 || selectedProfile >= c.savedProfiles.size()) return;
        TrailConfig.SavedProfile selected = c.savedProfiles.get(selectedProfile);
        section("edit_selected_profile");
        list.row(Component.empty(), button(text("apply_profile"), () -> {
            apply(new Profile(Component.literal(selected.name), selected, selectedProfile)); refresh();
        }));
        EditBox name = new EditBox(font, 0, 0, 150, 20, text("profile_name"));
        name.setMaxLength(32); name.setValue(selected.name);
        row("profile_name", name);
        Button rename = button(text("rename_profile"), () -> {
            String value = name.getValue().trim();
            if (value.isEmpty()) return;
            if (appliedProfile != null && appliedProfile.getString().equals(selected.name)) appliedProfile = Component.literal(value);
            selected.name = value;
            refresh();
        });
        name.setResponder(value -> rename.active = !value.isBlank());
        list.row(Component.empty(), rename);
        list.row(Component.empty(), button(text("overwrite_profile"), () -> {
            c.savedProfiles.set(selectedProfile, new TrailConfig.SavedProfile(selected.name, c)); refresh();
        }));
        list.row(Component.empty(), button(text(deleteArmedIndex == selectedProfile ? "confirm_delete_profile" : "delete_profile"), () -> {
            if (deleteArmedIndex != selectedProfile) { deleteArmedIndex = selectedProfile; refresh(); return; }
            c.savedProfiles.remove(selectedProfile); selectedProfile = -1; deleteArmedIndex = -1; appliedProfile = null; refresh();
        }));
    }

    private List<Profile> allProfiles() {
        List<Profile> result = new ArrayList<>();
        result.add(profile("standard", new TrailConfig()));
        TrailConfig soft = new TrailConfig();
        soft.additiveGlow = false; soft.width = 0.18; soft.effectStrength = 0.58 * 0.75;
        soft.pickupFlashStrength = 0.35; soft.pickupFlashSize = 0.48 * 0.18 / 0.24;
        result.add(profile("soft", soft));
        TrailConfig neon = new TrailConfig();
        neon.startColor = 0x62F4FF; neon.endColor = 0xB45CFF; neon.width = 0.27; neon.effectStrength = 0.9 * 1.3;
        neon.tailWidthScale = 0.08; neon.middleWidthScale = 1.2; neon.headWidthScale = 0.55;
        neon.pickupFlashStyle = "star"; neon.pickupFlashStrength = 0.9; neon.pickupFlashSize *= 0.27 / 0.24;
        result.add(profile("neon", neon));
        TrailConfig rainbow = new TrailConfig();
        rainbow.colorMode = "rainbow"; rainbow.rainbowSpeed = 0.22;
        rainbow.tailWidthScale = 0.1; rainbow.middleWidthScale = 1.1; rainbow.headWidthScale = 0.5;
        rainbow.pickupFlashStyle = "ring"; rainbow.pickupFlashStrength = 0.7;
        result.add(profile("rainbow", rainbow));
        var saved = XpOrbTrailsClient.CONFIG.savedProfiles;
        for (int i = 0; i < saved.size(); i++) result.add(new Profile(Component.literal(saved.get(i).name), saved.get(i), i));
        return result;
    }
    private Profile profile(String key, TrailConfig config) { return new Profile(text("profile." + key), new TrailConfig.SavedProfile(key, config), -1); }
    private void apply(Profile p) {
        p.settings().applyTo(XpOrbTrailsClient.CONFIG); selectedProfile = p.savedIndex(); appliedProfile = p.name(); deleteArmedIndex = -1;
    }

    private void resetPage() {
        TrailConfig c = XpOrbTrailsClient.CONFIG, d = new TrailConfig();
        switch (page) {
            case COMMON -> {
                c.enabled = d.enabled; c.width = d.width; c.lifetimeSeconds = d.lifetimeSeconds;
                c.effectStrength = d.effectStrength; c.pickupFlash = d.pickupFlash;
            }
            case APPEARANCE -> {
                c.colorMode = d.colorMode; c.startColor = d.startColor; c.endColor = d.endColor;
                c.rainbowSpeed = d.rainbowSpeed; c.additiveGlow = d.additiveGlow;
                c.tailWidthScale = d.tailWidthScale; c.middleWidthScale = d.middleWidthScale; c.headWidthScale = d.headWidthScale;
                c.pickupFlashStyle = d.pickupFlashStyle; c.pickupFlashStrength = d.pickupFlashStrength;
                c.pickupFlashSeconds = d.pickupFlashSeconds; c.pickupFlashSize = d.pickupFlashSize; c.pickupFadeSeconds = d.pickupFadeSeconds;
            }
            case ADVANCED -> {
                c.smoothFlow = d.smoothFlow; c.motionShift = d.motionShift; c.cameraPush = d.cameraPush;
                c.renderRange = d.renderRange; c.trailCap = d.trailCap;
            }
            case PROFILES -> { return; }
        }
        changed(); refresh();
    }

    private void row(String key, AbstractWidget widget) {
        widget.setTooltip(Tooltip.create(text(key).append("\n").append(text(key + ".tip"))));
        list.row(text(key), widget);
    }
    private void section(String key) { list.section(text(key)); }
    private Button button(Component label, Runnable action) { return Button.builder(label, b -> action.run()).bounds(0, 0, 150, 20).build(); }
    private void fold(String key, boolean expanded, Runnable action) { row(key, button(text(expanded ? "collapse" : "expand"), action)); }
    private void toggle(String key, boolean value, Consumer<Boolean> setter) {
        row(key, CycleButton.onOffBuilder(value).displayOnlyValue().create(0, 0, 150, 20, text(key), (b, v) -> { setter.accept(v); changed(); }));
    }
    private void slider(String key, double value, double min, double max, int decimals, DoubleConsumer setter) {
        row(key, new ConfigSlider(text(key), value, min, max, decimals, v -> { setter.accept(v); changed(); }));
    }
    private void select(String key, String prefix, String current, List<String> values, Consumer<String> setter) {
        row(key, button(text(prefix + current), () -> {
            List<TrailChoiceScreen.Choice> choices = new ArrayList<>();
            for (String value : values) choices.add(new TrailChoiceScreen.Choice(text(prefix + value), () -> { setter.accept(value); changed(); }));
            choose(key, choices);
        }));
    }
    private void color(String key, int value, IntConsumer setter) {
        row(key, button(Component.literal("■ ").withStyle(s -> s.withColor(value))
                .append(Component.literal(String.format(Locale.ROOT, "#%06X", value & 0xFFFFFF)).withStyle(s -> s.withColor(0xFFFFFF))), () -> {
            rememberScroll();
            minecraft.gui.setScreen(new ColorPickerScreen(this, value, v -> { setter.accept(v); changed(); }));
        }));
    }
    private static Component colorLabel(Component label, int start, int end) {
        return Component.literal("■ ").withStyle(s -> s.withColor(start))
                .append(Component.literal("■ ").withStyle(s -> s.withColor(end)))
                .append(label.copy().withStyle(s -> s.withColor(0xFFFFFF)));
    }
    private void setColors(int start, int end) { XpOrbTrailsClient.CONFIG.startColor = start; XpOrbTrailsClient.CONFIG.endColor = end; changed(); }
    private void choose(String key, List<TrailChoiceScreen.Choice> choices) { rememberScroll(); minecraft.gui.setScreen(new TrailChoiceScreen(this, text(key), choices)); }
    private Component shapeName() {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        return text("shape." + SHAPES.stream().filter(s -> s.tail() == c.tailWidthScale
                && s.middle() == c.middleWidthScale && s.head() == c.headWidthScale)
                .map(Shape::key).findFirst().orElse("custom"));
    }
    private void changed() {
        appliedProfile = null; deleteArmedIndex = -1;
        if (profileButton != null) profileButton.setMessage(text("preset.custom"));
        if (shapeButton != null) shapeButton.setMessage(shapeName());
    }
    private void rememberScroll() { if (list != null) scroll = list.scrollAmount(); }
    private void refresh() { rememberScroll(); XpOrbTrailsClient.saveConfig(); rebuildWidgets(); }
    @Override public void resize(int width, int height) { rememberScroll(); super.resize(width, height); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        if (wide) preview.extractPreview(graphics, previewX, 56, previewWidth, height - 96, true);
    }
    @Override public void onClose() { XpOrbTrailsClient.saveConfig(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final Component label;
        private final double min, max;
        private final int decimals;
        private final DoubleConsumer setter;
        ConfigSlider(Component label, double current, double min, double max, int decimals, DoubleConsumer setter) {
            super(0, 0, 150, 20, Component.empty(), Math.max(0, Math.min(1, (current - min) / (max - min))));
            this.label = label; this.min = min; this.max = max; this.decimals = decimals; this.setter = setter; updateMessage();
        }
        private double actual() { return min + value * (max - min); }
        @Override protected void updateMessage() { setMessage(Component.literal(String.format(Locale.ROOT, "%." + decimals + "f", actual()))); }
        @Override protected void applyValue() { setter.accept(actual()); }
        @Override protected MutableComponent createNarrationMessage() { return label.copy().append(": ").append(getMessage()); }
    }
}
