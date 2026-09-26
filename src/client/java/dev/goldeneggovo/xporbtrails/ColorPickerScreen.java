package dev.goldeneggovo.xporbtrails;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.IntConsumer;

public final class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer consumer;
    private float hue;
    private float saturation;
    private float brightness;
    private EditBox hexBox;
    private boolean draggingPalette;
    private boolean draggingHue;
    private boolean syncingHex;

    public ColorPickerScreen(Screen parent, int color, IntConsumer consumer) {
        super(Component.translatable("screen.xporbtrails.color_picker"));
        this.parent = parent;
        this.consumer = consumer;
        float[] hsv = rgbToHsv(color);
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2];
    }

    @Override
    protected void init() {
        int center = width / 2;
        hexBox = new EditBox(font, center - 80, 205, 160, 20, Component.translatable("screen.xporbtrails.hex"));
        hexBox.setMaxLength(7);
        hexBox.setValue(hex(currentColor()));
        hexBox.setResponder(text -> {
            if (syncingHex) return;
            String value = text.startsWith("#") ? text.substring(1) : text;
            if (value.matches("[0-9a-fA-F]{6}")) {
                int color = Integer.parseInt(value, 16);
                float[] hsv = rgbToHsv(color);
                hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2];
                consumer.accept(color);
            }
        });
        addRenderableWidget(hexBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(center - 80, 235, 160, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int px = width / 2 - 80, py = 48, pw = 160, ph = 112;
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        int hueColor = hsvToRgb(hue, 1.0F, 1.0F);
        for (int x = 0; x < pw; x++) {
            float sat = x / (float) (pw - 1);
            graphics.fillGradient(px + x, py, px + x + 1, py + ph,
                    0xFF000000 | mix(0xFFFFFF, hueColor, sat), 0xFF000000);
        }
        graphics.outline(px - 1, py - 1, pw + 2, ph + 2, 0xFFFFFFFF);
        int sx = px + Math.round(saturation * (pw - 1));
        int sy = py + Math.round((1.0F - brightness) * (ph - 1));
        graphics.outline(sx - 3, sy - 3, 7, 7, 0xFFFFFFFF);
        graphics.outline(sx - 2, sy - 2, 5, 5, 0xFF000000);

        int hy = 174, hh = 12;
        for (int x = 0; x < pw; x++) {
            int color = hsvToRgb(x / (float) (pw - 1), 1.0F, 1.0F);
            graphics.fill(px + x, hy, px + x + 1, hy + hh, 0xFF000000 | color);
        }
        graphics.outline(px - 1, hy - 1, pw + 2, hh + 2, 0xFFFFFFFF);
        int hx = px + Math.round(hue * (pw - 1));
        graphics.outline(hx - 2, hy - 2, 5, hh + 4, 0xFFFFFFFF);
        graphics.fill(px + pw + 12, py, px + pw + 42, py + 30, 0xFF000000 | currentColor());
        graphics.outline(px + pw + 11, py - 1, 32, 32, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("screen.xporbtrails.hex"), px, 193, 0xFFA0A0A0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) return true;
        int px = width / 2 - 80;
        if (inside(event.x(), event.y(), px, 48, 160, 112)) {
            draggingPalette = true; updatePalette(event.x(), event.y()); return true;
        }
        if (inside(event.x(), event.y(), px, 174, 160, 12)) {
            draggingHue = true; updateHue(event.x()); return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingPalette) { updatePalette(event.x(), event.y()); return true; }
        if (draggingHue) { updateHue(event.x()); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingPalette = false; draggingHue = false;
        return super.mouseReleased(event);
    }

    private void updatePalette(double mouseX, double mouseY) {
        int px = width / 2 - 80;
        saturation = clamp((float) ((mouseX - px) / 159.0));
        brightness = 1.0F - clamp((float) ((mouseY - 48) / 111.0));
        changed();
    }

    private void updateHue(double mouseX) {
        hue = clamp((float) ((mouseX - (width / 2 - 80)) / 159.0));
        changed();
    }

    private void changed() {
        int color = currentColor();
        consumer.accept(color);
        if (hexBox != null) {
            syncingHex = true;
            hexBox.setValue(hex(color));
            syncingHex = false;
        }
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    private int currentColor() { return hsvToRgb(hue, saturation, brightness); }
    private static boolean inside(double x, double y, int bx, int by, int bw, int bh) { return x >= bx && x < bx + bw && y >= by && y < by + bh; }
    private static float clamp(float v) { return Math.max(0.0F, Math.min(1.0F, v)); }
    private static String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF); }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int hsvToRgb(float h, float s, float v) {
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

    private static float[] rgbToHsv(int color) {
        float r = ((color >> 16) & 255) / 255.0F, g = ((color >> 8) & 255) / 255.0F, b = (color & 255) / 255.0F;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d != 0) {
            if (max == r) h = ((g - b) / d) % 6.0F;
            else if (max == g) h = (b - r) / d + 2.0F;
            else h = (r - g) / d + 4.0F;
            h /= 6.0F; if (h < 0) h += 1.0F;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
