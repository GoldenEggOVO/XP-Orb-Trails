package dev.goldeneggovo.xporbtrails;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

final class TrailChoiceScreen extends Screen {
    record Choice(Component label, Runnable select) { }
    private final Screen parent;
    private final List<Choice> choices;

    TrailChoiceScreen(Screen parent, Component title, List<Choice> choices) {
        super(title);
        this.parent = parent;
        this.choices = choices;
    }

    @Override protected void init() {
        int w = Math.min(380, width - 24);
        var list = addRenderableWidget(new TrailSettingsList((width - w) / 2, 36, w, height - 76));
        for (Choice choice : choices) {
            list.row(Component.empty(), Button.builder(choice.label(), b -> {
                choice.select().run();
                onClose();
            }).bounds(0, 0, w - 24, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(width / 2 - 75, height - 28, 150, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
