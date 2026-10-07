package dev.goldeneggovo.xporbtrails;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import java.util.List;

final class TrailSettingsList extends ContainerObjectSelectionList<TrailSettingsList.Row> {
    TrailSettingsList(int x, int y, int width, int height) {
        super(Minecraft.getInstance(), width, height, y, 30);
        setX(x);
        centerListVertically = false;
    }

    @Override public int getRowWidth() { return getWidth() - 18; }

    void row(Component label, AbstractWidget control) {
        int labelWidth = (getRowWidth() - 12) / 2;
        int lines = minecraft.font.split(label, labelWidth).size();
        addEntry(new Row(label, control), Math.max(30, lines * minecraft.font.lineHeight + 12));
    }

    void section(Component label) { addEntry(new Row(label, null), 26); }

    final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final Component label;
        private final AbstractWidget control;
        Row(Component label, AbstractWidget control) { this.label = label; this.control = control; }

        private void positionControl() {
            if (control == null) return;
            int labelWidth = (getContentWidth() - 12) / 2;
            int x = label.getString().isEmpty() ? getContentX() : getContentX() + labelWidth + 12;
            control.setX(x);
            control.setY(getContentY() + Math.max(0, (getContentHeight() - 20) / 2));
            control.setWidth(Math.max(1, getContentRight() - x));
        }
        @Override public void setX(int x) { super.setX(x); positionControl(); }
        @Override public void setY(int y) { super.setY(y); positionControl(); }
        @Override public void setWidth(int width) { super.setWidth(width); positionControl(); }
        @Override public void setHeight(int height) { super.setHeight(height); positionControl(); }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int labelWidth = control == null ? getContentWidth() : (getContentWidth() - 12) / 2;
            int y = getContentY() + 6;
            for (var line : minecraft.font.split(label, labelWidth)) {
                graphics.text(minecraft.font, line, getContentX(), y, control == null ? 0xFFB9DCA9 : 0xFFFFFFFF);
                y += minecraft.font.lineHeight;
            }
            if (control != null) {
                control.extractRenderState(graphics, mouseX, mouseY, delta);
            }
        }

        @Override public List<? extends GuiEventListener> children() { return control == null ? List.of() : List.of(control); }
        @Override public List<? extends NarratableEntry> narratables() { return control == null ? List.of() : List.of(control); }
    }
}
