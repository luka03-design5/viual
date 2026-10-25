package dev.axiom.lyrics;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Eigene, glatte Schriften (Poppins, mitgeliefert unter assets/spotifylyrics/font). */
final class Fx {
    static final Style LYRICS = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("spotifylyrics", "lyrics")));
    static final Style UI = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("spotifylyrics", "ui")));

    static Text lyrics(String s) { return Text.literal(s).setStyle(LYRICS); }
    static Text ui(String s) { return Text.literal(s).setStyle(UI); }

    /** Kürzt den Text mit "..." so, dass er in die Breite passt (in UI-Schrift gemessen). */
    static String fit(TextRenderer tr, String s, int maxW) {
        if (s == null) return "";
        if (tr.getWidth(ui(s)) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && tr.getWidth(ui(t + "...")) > maxW) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    private Fx() {}
}
