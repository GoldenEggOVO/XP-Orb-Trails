package dev.goldeneggovo.xporbtrails;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TrailPreviewScreen extends Screen {
    private final Screen parent;
    private long demoStartNanos = System.nanoTime();

    public TrailPreviewScreen(Screen parent) {
        super(Component.translatable("screen.xporbtrails.preview_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int footerY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.replay_pickup"), b -> demoStartNanos = System.nanoTime())
                .bounds(width / 2 - 154, footerY, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.xporbtrails.back_to_settings"), b -> onClose())
                .bounds(width / 2 + 4, footerY, 150, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);

        int canvasW = Math.max(260, Math.min(640, width - 32));
        int canvasH = Math.max(120, height - 76);
        int x0 = width / 2 - canvasW / 2;
        int y0 = 30;
        extractPreview(graphics, x0, y0, canvasW, canvasH, false);
    }

    public void extractPreview(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean compact) {
        graphics.enableScissor(x, y, x + w, y + h);
        drawBackdrop(graphics, x, y, w, h);
        drawPreview(graphics, x, y, w, h, compact);
        graphics.disableScissor();
    }

    private void drawBackdrop(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xE80A1018);
        for (int band = 0; band < 8; band++) {
            int top = y + band * h / 8;
            int bottom = y + (band + 1) * h / 8;
            int alpha = 18 + band * 3;
            graphics.fill(x + 1, top, x + w - 1, bottom, (alpha << 24) | 0x173828);
        }
        for (int gx = x + 16; gx < x + w; gx += 32) graphics.fill(gx, y + 1, gx + 1, y + h - 1, 0x163E8060);
        for (int gy = y + 16; gy < y + h; gy += 32) graphics.fill(x + 1, gy, x + w - 1, gy + 1, 0x163E8060);
        graphics.outline(x, y, w, h, 0xAA6EDC96);
        graphics.outline(x + 2, y + 2, w - 4, h - 4, 0x443B8E62);

        double seconds = System.nanoTime() / 1_000_000_000.0;
        for (int i = 0; i < 30; i++) {
            int px = x + 10 + Math.floorMod(i * 83 + 17, Math.max(1, w - 20));
            int py = y + 24 + Math.floorMod(i * 47 + 11, Math.max(1, h - 40));
            int alpha = 28 + (int) (26 * (0.5 + 0.5 * Math.sin(seconds * (0.7 + i % 4 * 0.13) + i)));
            graphics.fill(px, py, px + 1 + i % 2, py + 1 + i % 2, (alpha << 24) | 0xB8FFD0);
        }
    }

    private void drawPreview(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean compact) {
        TrailConfig c = XpOrbTrailsClient.CONFIG;
        double seconds = System.nanoTime() / 1_000_000_000.0;
        double elapsed = (System.nanoTime() - demoStartNanos) / 1_000_000_000.0;
        double cycle = elapsed % 5.2;
        double travel = Math.min(1.0, cycle / 4.0);
        travel = smooth(travel);
        int left = x + 28;
        int right = x + w - 30;
        int centerY = y + h / 2 + 8;

        Component mode = Component.translatable("screen.xporbtrails.preview_mode")
                .append(": ").append(Component.translatable("screen.xporbtrails.mode." + c.colorMode));
        Component flash = Component.translatable("screen.xporbtrails.preview_flash")
                .append(": ").append(Component.translatable("screen.xporbtrails.flash_style." + c.pickupFlashStyle));
        graphics.text(font, mode, x + 10, y + 9, 0xFFBFD9C8);
        graphics.text(font, flash, x + 10, y + 21, 0xFF8EB59B);
        graphics.text(font, TrailConfigScreen.crossSectionLabel(c.crossSectionSides), x + 10, y + 33, 0xFF8EB59B);

        int samples = Math.max(36, Math.min(90, w / 6));
        double visibleLength = Math.min(1.0, 0.28 + c.lifetimeSeconds / 7.5);
        double tailStart = Math.max(0.0, travel - visibleLength);
        for (int i = 0; c.enabled && i < samples; i++) {
            double q = i / (double) (samples - 1);
            double p = tailStart + (travel - tailStart) * q;
            int px = left + (int) Math.round((right - left) * p);
            int py = pathY(centerY, h, p, seconds);
            double shape = widthScale(c, q);
            int radius = Math.max(1, Math.min(11, (int) Math.round(c.width * 22.0 * shape)));
            int rgb = colorAt(c, q, seconds);
            double fade = cycle < 4.0 ? 1.0 : 1.0 - smooth((cycle - 4.0) / c.pickupFadeSeconds);
            int alpha = clamp255((int) (255 * c.effectStrength * fade * (0.14 + q * 0.86)));
            if (c.additiveGlow) glowSquare(graphics, px, py, radius, rgb, alpha);
            else graphics.fill(px - radius, py - radius, px + radius + 1, py + radius + 1, (alpha << 24) | rgb);
        }

        int headX = left + (int) Math.round((right - left) * travel);
        int headY = pathY(centerY, h, travel, seconds);
        drawOrb(graphics, headX, headY, c.endColor);

        if (c.enabled && c.pickupFlash && cycle >= 4.0 && cycle < 4.0 + c.pickupFlashSeconds * 2.4) {
            double p = Math.min(1.0, (cycle - 4.0) / Math.max(0.12, c.pickupFlashSeconds * 2.4));
            drawFlash(graphics, headX, headY, p, c, seconds);
        }

        if (!compact) graphics.centeredText(font, Component.translatable("screen.xporbtrails.preview_hint"), x + w / 2, y + h - 15, 0xAAADC8B6);
    }

    private void glowSquare(GuiGraphicsExtractor graphics, int x, int y, int radius, int rgb, int alpha) {
        int outer = radius + 4;
        graphics.fill(x - outer, y - outer, x + outer + 1, y + outer + 1, (alpha / 7 << 24) | rgb);
        int middle = radius + 2;
        graphics.fill(x - middle, y - middle, x + middle + 1, y + middle + 1, (alpha / 3 << 24) | rgb);
        graphics.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, (alpha << 24) | rgb);
        if (radius >= 3) graphics.fill(x - radius / 2, y - radius / 2, x + radius / 2 + 1, y + radius / 2 + 1, (Math.min(255, alpha + 55) << 24) | 0xE8FFD8);
    }

    private void drawOrb(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x - 10, y - 10, x + 11, y + 11, 0x183CFF70);
        graphics.fill(x - 7, y - 7, x + 8, y + 8, 0x443CFF70);
        graphics.fill(x - 4, y - 4, x + 5, y + 5, 0xFF000000 | color);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFC2);
    }

    private void drawFlash(GuiGraphicsExtractor graphics, int x, int y, double progress, TrailConfig c, double seconds) {
        double eased = smooth(progress);
        int radius = Math.max(3, (int) Math.round((8 + 40 * eased) * c.pickupFlashSize));
        int alpha = clamp255((int) (210 * c.effectStrength * c.pickupFlashStrength * (1.0 - smooth(progress))));
        int rgb = "rainbow".equals(c.colorMode) ? hsv((float) (seconds * c.rainbowSpeed + 0.72), 0.75F, 1.0F)
                : "solid".equals(c.colorMode) ? c.startColor : c.endColor;
        int color = (alpha << 24) | rgb;
        if ("star".equals(c.pickupFlashStyle)) {
            graphics.fill(x - radius, y - 1, x + radius + 1, y + 2, color);
            graphics.fill(x - 1, y - radius, x + 2, y + radius + 1, color);
            for (int i = -radius; i <= radius; i += 2) {
                int d = i / 2;
                graphics.fill(x + d, y + d, x + d + 2, y + d + 2, color);
                graphics.fill(x + d, y - d, x + d + 2, y - d + 2, color);
            }
        } else if ("ring".equals(c.pickupFlashStyle)) {
            graphics.outline(x - radius, y - radius, radius * 2, radius * 2, color);
            if (radius > 8) graphics.outline(x - radius + 2, y - radius + 2, radius * 2 - 4, radius * 2 - 4, (alpha / 2 << 24) | rgb);
        } else {
            graphics.fill(x - radius, y - radius, x + radius, y + radius, (alpha / 10 << 24) | rgb);
            graphics.outline(x - radius, y - radius, radius * 2, radius * 2, color);
            int inner = Math.max(2, radius / 2);
            graphics.outline(x - inner, y - inner, inner * 2, inner * 2, (alpha / 2 << 24) | rgb);
        }
    }

    private static int pathY(int centerY, int height, double p, double seconds) {
        double amplitude = Math.max(10.0, Math.min(34.0, height * 0.16));
        return centerY + (int) Math.round(Math.sin(p * 7.2 + seconds * 1.7) * amplitude
                + Math.sin(p * 14.0 - seconds * 0.8) * amplitude * 0.24);
    }

    private static double widthScale(TrailConfig c, double p) {
        if (p < 0.5) {
            double t = smooth(p * 2.0);
            return c.tailWidthScale + (c.middleWidthScale - c.tailWidthScale) * t;
        }
        double t = smooth((p - 0.5) * 2.0);
        return c.middleWidthScale + (c.headWidthScale - c.middleWidthScale) * t;
    }

    private static int colorAt(TrailConfig c, double p, double seconds) {
        if ("solid".equals(c.colorMode)) return c.startColor;
        if ("rainbow".equals(c.colorMode)) return hsv((float) (p * 0.72 + seconds * c.rainbowSpeed), 0.88F, 1.0F);
        return mix(c.startColor, c.endColor, p);
    }

    private static double smooth(double value) {
        value = Math.max(0.0, Math.min(1.0, value));
        return value * value * (3.0 - 2.0 * value);
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int hsv(float h, float s, float v) {
        float sector = (h - (float) Math.floor(h)) * 6.0F;
        int i = (int) sector; float f = sector - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private static int clamp255(int value) { return Math.max(0, Math.min(255, value)); }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}
