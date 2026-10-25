package dev.axiom.gui;

import dev.axiom.client.Ui;
import dev.axiom.module.Module;
import dev.axiom.module.ModuleManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Visuals-Client ClickGUI — dunkles Panel, Kategorie-Spalten, Toggle-Karten.
 * Schrift: Poppins (nicht pixelig).
 */
public final class ClickGui extends Screen {
    private static final int BG = 0xF20B0D12;
    private static final int PANEL = 0xFF12151C;
    private static final int CARD = 0xFF1A1F2A;
    private static final int CARD_ON = 0xFF252B3A;
    private static final int TEXT = 0xFFE8EAF0;
    private static final int MUTED = 0xFF8B93A5;
    private static final int ACCENT = 0xFFA06CFF;

    private final Map<String, List<Module>> cats = new LinkedHashMap<>();
    private int scroll;

    public ClickGui() {
        super(Text.literal("Axiom Visuals"));
        for (Module m : ModuleManager.all()) {
            cats.computeIfAbsent(m.category(), k -> new ArrayList<>()).add(m);
        }
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        // dim world behind
        g.fill(0, 0, this.width, this.height, 0x88000000);

        int colW = 160;
        int gap = 12;
        int cols = cats.size();
        int totalW = cols * colW + (cols - 1) * gap;
        int startX = (this.width - totalW) / 2;
        int top = 48;

        Ui.textBold(g, this.textRenderer, "AXIOM", startX, 18, TEXT);
        Ui.text(g, this.textRenderer, "visuals  ·  right shift", startX + Ui.width(this.textRenderer, "AXIOM ") + 4, 22, MUTED);

        int ci = 0;
        for (Map.Entry<String, List<Module>> e : cats.entrySet()) {
            int x = startX + ci * (colW + gap);
            int y = top - scroll;
            int h = 28 + e.getValue().size() * 36 + 12;
            Ui.rrect(g, x, y, colW, h, 10, PANEL);

            // category header
            Ui.rrect(g, x + 8, y + 8, 6, 6, 3, ACCENT);
            Ui.textBold(g, this.textRenderer, e.getKey(), x + 20, y + 10, TEXT);

            int my = y + 28;
            for (Module m : e.getValue()) {
                boolean on = m.enabled();
                boolean hover = mouseX >= x + 8 && mouseX < x + colW - 8
                    && mouseY >= my && mouseY < my + 32;
                int bg = on ? CARD_ON : CARD;
                if (hover) bg = on ? 0xFF2C3344 : 0xFF222836;
                Ui.rrect(g, x + 8, my, colW - 16, 32, 8, bg);

                // accent bar if on
                if (on) Ui.rrect(g, x + 8, my, 3, 32, 2, ACCENT);

                Ui.text(g, this.textRenderer, m.name(), x + 18, my + 7, on ? TEXT : MUTED);
                Ui.text(g, this.textRenderer, m.desc(), x + 18, my + 18, 0xFF5B6377);

                // toggle pill
                int px = x + colW - 40;
                int py = my + 10;
                Ui.rrect(g, px, py, 22, 12, 6, on ? ACCENT : 0xFF3A4150);
                Ui.rrect(g, on ? px + 12 : px + 2, py + 2, 8, 8, 4, 0xFFFFFFFF);

                my += 36;
            }
            ci++;
        }

        Ui.text(g, this.textRenderer, "click module  ·  esc close", startX, this.height - 20, 0xFF4A5160);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        int colW = 160;
        int gap = 12;
        int cols = cats.size();
        int totalW = cols * colW + (cols - 1) * gap;
        int startX = (this.width - totalW) / 2;
        int top = 48;

        int ci = 0;
        for (Map.Entry<String, List<Module>> e : cats.entrySet()) {
            int x = startX + ci * (colW + gap);
            int y = top - scroll;
            int my = y + 28;
            for (Module m : e.getValue()) {
                if (mouseX >= x + 8 && mouseX < x + colW - 8
                    && mouseY >= my && mouseY < my + 32) {
                    m.toggle();
                    return true;
                }
                my += 36;
            }
            ci++;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() { return false; }
}
