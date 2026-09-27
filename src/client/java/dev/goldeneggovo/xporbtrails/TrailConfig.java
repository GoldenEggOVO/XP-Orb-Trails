package dev.goldeneggovo.xporbtrails;

import java.util.ArrayList;
import java.util.List;

public final class TrailConfig {
    public int configVersion = 9;
    public boolean enabled = true;
    public int trailCap = 96;
    public double renderRange = 36.0;
    public double lifetimeSeconds = 2.6;
    public double width = 0.1;
    public int crossSectionSides = 2;
    public double opacity = 0.78;
    public double effectStrength = 0.78;
    public double pointSpacing = 0.18;
    public double groundOffset = 0.025;
    public double lift = 0.18;
    public double groundHug = 0.68;
    public double maxOrbHeight = 1.35;
    public int startColor = 0xE6FF2A;
    public int endColor = 0x66FF00;
    public double orbYOffset = 0.175;
    public double motionShift = 0.10;
    public double cameraPush = 0.10;
    public double smoothFlow = 0.48;
    public double pickupFadeSeconds = 0.35;
    public String colorMode = "gradient";
    public double rainbowSpeed = 0.20;
    public double glowStrength = 1.0;
    public double tailWidthScale = 0.18;
    public double middleWidthScale = 0.70;
    public double headWidthScale = 1.0;
    public boolean pickupFlash = true;
    public double pickupFlashStrength = 0.55;
    public double pickupFlashSeconds = 0.22;
    public double pickupFlashSize = 0.65;
    public String pickupFlashStyle = "soft";
    public List<SavedPreset> savedPresets = new ArrayList<>();
    public List<SavedProfile> savedProfiles = new ArrayList<>();
    public boolean additiveGlow = true;

    public TrailConfig sanitized() {
        if (configVersion < 2) {
            motionShift = 0.10;
            cameraPush = 0.10;
            smoothFlow = 0.48;
            configVersion = 2;
        }
        if (configVersion < 3) {
            pickupFadeSeconds = 0.35;
            configVersion = 3;
        }
        if (configVersion < 4) {
            colorMode = "gradient";
            rainbowSpeed = 0.20;
            glowStrength = 1.0;
            tailWidthScale = 0.18;
            headWidthScale = 1.0;
            savedPresets = new ArrayList<>();
            configVersion = 4;
        }
        if (configVersion < 5) {
            middleWidthScale = 0.70;
            configVersion = 5;
        }
        if (configVersion < 6) {
            pickupFlash = true;
            pickupFlashStrength = 0.55;
            pickupFlashSeconds = 0.22;
            configVersion = 6;
        }
        if (configVersion < 7) {
            pickupFlashSize = 0.65;
            configVersion = 7;
        }
        if (configVersion < 8) {
            pickupFlashStyle = "soft";
            savedProfiles = new ArrayList<>();
            configVersion = 8;
        }
        trailCap = Math.max(8, Math.min(256, trailCap));
        renderRange = Math.max(4.0, Math.min(128.0, renderRange));
        lifetimeSeconds = Math.max(0.25, Math.min(10.0, lifetimeSeconds));
        width = Math.max(0.02, Math.min(1.0, width));
        crossSectionSides = Math.max(2, Math.min(32, crossSectionSides));
        opacity = Math.max(0.05, Math.min(1.0, opacity));
        pointSpacing = Math.max(0.03, Math.min(1.0, pointSpacing));
        groundHug = Math.max(0.0, Math.min(1.0, groundHug));
        maxOrbHeight = Math.max(0.2, Math.min(5.0, maxOrbHeight));
        motionShift = Math.max(0.0, Math.min(1.0, motionShift));
        cameraPush = Math.max(0.0, Math.min(1.0, cameraPush));
        smoothFlow = Math.max(0.0, Math.min(1.0, smoothFlow));
        pickupFadeSeconds = Math.max(0.05, Math.min(3.0, pickupFadeSeconds));
        if (!"solid".equals(colorMode) && !"gradient".equals(colorMode) && !"rainbow".equals(colorMode)) colorMode = "gradient";
        rainbowSpeed = Math.max(0.02, Math.min(2.0, rainbowSpeed));
        glowStrength = Math.max(0.1, Math.min(2.0, glowStrength));
        if (configVersion < 9) {
            // Preserve the old alpha product and flash radius, then decouple their controls.
            effectStrength = opacity * glowStrength;
            pickupFlashSize = Math.max(0.25, Math.min(2.0, pickupFlashSize)) * width / 0.24;
            configVersion = 9;
        }
        effectStrength = Math.max(0.005, Math.min(2.0, effectStrength));
        tailWidthScale = Math.max(0.0, Math.min(1.5, tailWidthScale));
        middleWidthScale = Math.max(0.0, Math.min(2.0, middleWidthScale));
        headWidthScale = Math.max(0.05, Math.min(2.0, headWidthScale));
        pickupFlashStrength = Math.max(0.1, Math.min(2.0, pickupFlashStrength));
        pickupFlashSeconds = Math.max(0.08, Math.min(1.0, pickupFlashSeconds));
        pickupFlashSize = Math.max(0.02, Math.min(8.34, pickupFlashSize));
        if (!"soft".equals(pickupFlashStyle) && !"star".equals(pickupFlashStyle) && !"ring".equals(pickupFlashStyle)) pickupFlashStyle = "soft";
        if (savedPresets == null) savedPresets = new ArrayList<>();
        savedPresets.removeIf(p -> p == null || p.name == null || p.name.isBlank());
        if (savedPresets.size() > 12) savedPresets = new ArrayList<>(savedPresets.subList(0, 12));
        if (savedProfiles == null) savedProfiles = new ArrayList<>();
        savedProfiles.removeIf(p -> p == null || p.name == null || p.name.isBlank());
        if (savedProfiles.size() > 8) savedProfiles = new ArrayList<>(savedProfiles.subList(0, 8));
        for (SavedProfile profile : savedProfiles) profile.migrate();
        return this;
    }

    public void copyFrom(TrailConfig other) {
        configVersion = other.configVersion;
        enabled = other.enabled;
        trailCap = other.trailCap;
        renderRange = other.renderRange;
        lifetimeSeconds = other.lifetimeSeconds;
        width = other.width;
        crossSectionSides = other.crossSectionSides;
        opacity = other.opacity;
        effectStrength = other.effectStrength;
        pointSpacing = other.pointSpacing;
        groundOffset = other.groundOffset;
        lift = other.lift;
        groundHug = other.groundHug;
        maxOrbHeight = other.maxOrbHeight;
        startColor = other.startColor;
        endColor = other.endColor;
        orbYOffset = other.orbYOffset;
        motionShift = other.motionShift;
        cameraPush = other.cameraPush;
        smoothFlow = other.smoothFlow;
        pickupFadeSeconds = other.pickupFadeSeconds;
        colorMode = other.colorMode;
        rainbowSpeed = other.rainbowSpeed;
        glowStrength = other.glowStrength;
        tailWidthScale = other.tailWidthScale;
        middleWidthScale = other.middleWidthScale;
        headWidthScale = other.headWidthScale;
        pickupFlash = other.pickupFlash;
        pickupFlashStrength = other.pickupFlashStrength;
        pickupFlashSeconds = other.pickupFlashSeconds;
        pickupFlashSize = other.pickupFlashSize;
        pickupFlashStyle = other.pickupFlashStyle;
        savedPresets = new ArrayList<>();
        for (SavedPreset preset : other.savedPresets) savedPresets.add(new SavedPreset(preset.name, preset.startColor, preset.endColor));
        savedProfiles = new ArrayList<>();
        for (SavedProfile profile : other.savedProfiles) savedProfiles.add(new SavedProfile(profile));
        additiveGlow = other.additiveGlow;
    }

    public static final class SavedPreset {
        public String name;
        public int startColor;
        public int endColor;

        public SavedPreset() { }
        public SavedPreset(String name, int startColor, int endColor) {
            this.name = name;
            this.startColor = startColor;
            this.endColor = endColor;
        }
    }

    public static final class SavedProfile {
        public String name;
        public int settingsVersion;
        public int crossSectionSides = 3;
        public double effectStrength = 0.78;
        public boolean enabled, additiveGlow, pickupFlash;
        public int trailCap, startColor, endColor;
        public double renderRange, lifetimeSeconds, width, opacity, motionShift, cameraPush, smoothFlow,
                pickupFadeSeconds, rainbowSpeed, glowStrength, tailWidthScale, middleWidthScale, headWidthScale,
                pickupFlashStrength, pickupFlashSeconds, pickupFlashSize;
        public String colorMode, pickupFlashStyle;

        public SavedProfile() { }
        public SavedProfile(String name, TrailConfig c) {
            this.name = name;
            capture(c);
        }
        public SavedProfile(SavedProfile other) {
            other.migrate();
            settingsVersion = other.settingsVersion;
            crossSectionSides = other.crossSectionSides;
            effectStrength = other.effectStrength;
            this.name = other.name;
            enabled = other.enabled; additiveGlow = other.additiveGlow; pickupFlash = other.pickupFlash;
            trailCap = other.trailCap; startColor = other.startColor; endColor = other.endColor;
            renderRange = other.renderRange; lifetimeSeconds = other.lifetimeSeconds; width = other.width; opacity = other.opacity;
            motionShift = other.motionShift; cameraPush = other.cameraPush; smoothFlow = other.smoothFlow;
            pickupFadeSeconds = other.pickupFadeSeconds; rainbowSpeed = other.rainbowSpeed; glowStrength = other.glowStrength;
            tailWidthScale = other.tailWidthScale; middleWidthScale = other.middleWidthScale; headWidthScale = other.headWidthScale;
            pickupFlashStrength = other.pickupFlashStrength; pickupFlashSeconds = other.pickupFlashSeconds; pickupFlashSize = other.pickupFlashSize;
            colorMode = other.colorMode; pickupFlashStyle = other.pickupFlashStyle;
        }
        private void capture(TrailConfig c) {
            crossSectionSides = c.crossSectionSides;
            settingsVersion = 9;
            effectStrength = c.effectStrength;
            enabled = c.enabled; additiveGlow = c.additiveGlow; pickupFlash = c.pickupFlash;
            trailCap = c.trailCap; startColor = c.startColor; endColor = c.endColor;
            renderRange = c.renderRange; lifetimeSeconds = c.lifetimeSeconds; width = c.width; opacity = c.opacity;
            motionShift = c.motionShift; cameraPush = c.cameraPush; smoothFlow = c.smoothFlow;
            pickupFadeSeconds = c.pickupFadeSeconds; rainbowSpeed = c.rainbowSpeed; glowStrength = c.glowStrength;
            tailWidthScale = c.tailWidthScale; middleWidthScale = c.middleWidthScale; headWidthScale = c.headWidthScale;
            pickupFlashStrength = c.pickupFlashStrength; pickupFlashSeconds = c.pickupFlashSeconds; pickupFlashSize = c.pickupFlashSize;
            colorMode = c.colorMode; pickupFlashStyle = c.pickupFlashStyle;
        }
        public void applyTo(TrailConfig c) {
            migrate();
            c.crossSectionSides = crossSectionSides;
            c.configVersion = 9;
            c.effectStrength = effectStrength;
            c.enabled = enabled; c.additiveGlow = additiveGlow; c.pickupFlash = pickupFlash;
            c.trailCap = trailCap; c.startColor = startColor; c.endColor = endColor;
            c.renderRange = renderRange; c.lifetimeSeconds = lifetimeSeconds; c.width = width; c.opacity = opacity;
            c.motionShift = motionShift; c.cameraPush = cameraPush; c.smoothFlow = smoothFlow;
            c.pickupFadeSeconds = pickupFadeSeconds; c.rainbowSpeed = rainbowSpeed; c.glowStrength = glowStrength;
            c.tailWidthScale = tailWidthScale; c.middleWidthScale = middleWidthScale; c.headWidthScale = headWidthScale;
            c.pickupFlashStrength = pickupFlashStrength; c.pickupFlashSeconds = pickupFlashSeconds; c.pickupFlashSize = pickupFlashSize;
            c.colorMode = colorMode; c.pickupFlashStyle = pickupFlashStyle;
            c.sanitized();
        }

        private void migrate() {
            if (settingsVersion >= 9) return;
            effectStrength = Math.max(0.05, Math.min(1.0, opacity))
                    * Math.max(0.1, Math.min(2.0, glowStrength));
            pickupFlashSize = Math.max(0.25, Math.min(2.0, pickupFlashSize))
                    * Math.max(0.02, Math.min(1.0, width)) / 0.24;
            settingsVersion = 9;
        }
    }
}
