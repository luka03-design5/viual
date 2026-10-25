package dev.axiom.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Poppins UI text — not the pixel default. */
public final class Ui {
    public static final Style FONT = Style.EMPTY.withFont(
        new StyleSpriteSource.Font(Identifier.of("spotifylyrics", "ui")));
    public static final Style FONT_BOLD = Style.EMPTY.withFont(
        new StyleSpriteSource.Font(Identifier.of("spotifylyrics", "lyrics")));

    public static Text t(String s) { return Text.literal(s).setStyle(FONT); }
    public static Text tb(String s) { return Text.literal(s).setStyle(FONT_BOLD); }

    public static void text(DrawContext g, TextRenderer tr, String s, int x, int y, int argb) {
        g.drawText(tr, t(s), x, y, argb, false);
    }

    public static void textBold(DrawContext g, TextRenderer tr, String s, int x, int y, int argb) {
        g.drawText(tr, tb(s), x, y, argb, false);
    }

    public static int width(TextRenderer tr, String s) {
        return tr.getWidth(t(s));
    }

    /** Smooth-ish rounded rect (same technique as lyrics screen). */
    public static void rrect(DrawContext g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { g.fill(x, y, x + w, y + h, color); return; }
        g.fill(x, y + r, x + w, y + h - r, color);
        g.fill(x + r, y, x + w - r, y + r, color);
        g.fill(x + r, y + h - r, x + w - r, y + h, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    private Ui() {}
}
