package dev.axiom.lyrics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Einstellungs-Menü (Taste K): dunkles Glas in Lila, Navigation oben, Modul-Karten im Raster. */
public class LyricsScreen extends Screen {
    private static final int MUTED = 0xFFA79BC4, TEXT = 0xFFF4F0FF, OFF = 0x33FFFFFF;
    private static final String[] TAB_NAMES = {"Übersicht", "Anzeige", "Verbindung"};
    private static final String[] TAB_SUB = {
        "Was gerade läuft", "Sieh die Lyrics in einem anderen Licht", "Woher kommt der Song?"};

    private static int tab = 0;
    private TextFieldWidget clientIdField;
    private int px, py, pw, ph, gx, gw, y0;

    public LyricsScreen() {
        super(Text.literal("Spotify Lyrics"));
    }

    private static int accent() { return 0xFF000000 | LyricsState.theme().glow(); }
    private static int accentLight() { return 0xFF000000 | LyricsState.scale(LyricsState.theme().glow(), 1.35f); }
    private static int accentDark() { return 0xFF000000 | LyricsState.scale(LyricsState.theme().glow(), 0.62f); }

    // ------------------------------------------------------------------ Aufbau

    @Override
    protected void init() {
        pw = Math.min(this.width - 16, 450);
        ph = Math.min(this.height - 16, 272);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        gx = px + 16;
        gw = pw - 32;
        y0 = py + 78;
        int colW = (gw - 8) / 2;

        // Navigation oben
        int nx = px + 16;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            NavButton b = new NavButton(this.textRenderer, nx, py + 12, i);
            addDrawableChild(b);
            nx += b.bw + 6;
        }

        if (tab == 0) {
            addDrawableChild(new Card(this.textRenderer, gx, y0 + 70, colW, 48, 0, "Lyrics",
                "Songtexte im Spiel", () -> LyricsState.enabled, true,
                () -> { LyricsState.enabled = !LyricsState.enabled; LyricsState.saveSettings(); }));
        } else if (tab == 1) {
            addDrawableChild(new Card(this.textRenderer, gx, y0, colW, 48, 3, "Schwebend",
                "3D-Wörter in der Luft", () -> LyricsState.mode == 0, true,
                () -> { LyricsState.mode = 0; LyricsState.saveSettings(); }));
            addDrawableChild(new Card(this.textRenderer, gx + colW + 8, y0, colW, 48, 4, "Unten",
                "Zeilen am Bildschirmrand", () -> LyricsState.mode == 1, true,
                () -> { LyricsState.mode = 1; LyricsState.saveSettings(); }));
            addDrawableChild(new Swatches(this.textRenderer, gx, y0 + 56, gw, 34));
            addDrawableChild(slider(gx, y0 + 98, gw, "Größe", 40, 300, LyricsState.size * 100.0,
                v -> { LyricsState.size = (float) (v / 100.0); LyricsState.saveSettings(); },
                v -> Math.round(v) + " %"));
            addDrawableChild(slider(gx, y0 + 140, gw, "Timing", -1500, 1500, LyricsState.offsetMs,
                v -> { LyricsState.offsetMs = (int) (Math.round(v / 10.0) * 10); LyricsState.saveSettings(); },
                v -> (Math.round(v / 10.0) * 10) + " ms"));
        } else {
            addDrawableChild(new Card(this.textRenderer, gx, y0, colW, 48, 5, "Windows-Medien",
                "Ohne Login, App & Browser", () -> LyricsState.source == 0, true,
                () -> { LyricsState.source = 0; LyricsState.saveSettings(); this.clearAndInit(); }));
            addDrawableChild(new Card(this.textRenderer, gx + colW + 8, y0, colW, 48, 6, "Spotify-Konto",
                "Eigene Spotify-App nötig", () -> LyricsState.source == 1, true,
                () -> { LyricsState.source = 1; LyricsState.saveSettings(); this.clearAndInit(); }));
            if (LyricsState.source == 1) {
                clientIdField = new TextFieldWidget(this.textRenderer, gx + 10, y0 + 100, gw - 20, 18, Text.literal("Client ID"));
                clientIdField.setMaxLength(64);
                clientIdField.setPlaceholder(Text.literal("Spotify Client ID einfügen"));
                if (SpotifyAuth.clientId != null) clientIdField.setText(SpotifyAuth.clientId);
                addDrawableChild(clientIdField);
                addDrawableChild(new PillButton(this.textRenderer, gx + 10, y0 + 124, gw - 20, 20, "Mit Spotify verbinden", () -> {
                    String id = clientIdField.getText().trim();
                    if (id.isEmpty() && SpotifyAuth.clientId == null) {
                        LyricsState.status = "Bitte zuerst die Client ID eintragen.";
                    } else {
                        LyricsState.status = "Browser öffnet sich ...";
                        SpotifyAuth.login(id.isEmpty() ? null : id);
                    }
                }));
            }
        }
    }

    private Slide slider(int x, int y, int w, String label, double min, double max, double value,
                         DoubleConsumer onChange, DoubleFunction<String> fmt) {
        return new Slide(this.textRenderer, x, y, w, 34, label, min, max, value, onChange, fmt);
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);
        TextRenderer tr = this.textRenderer;

        // Glas-Panel: Farbverlauf von Violett nach fast Schwarz, feiner Rand, Sterne
        rrect(g, px - 1, py - 1, pw + 2, ph + 2, 11, 0x40FFFFFF);
        rrectGradient(g, px, py, pw, ph, 10, 0xF02A1442, 0xF00B0814);
        Random rnd = new Random(7);
        for (int i = 0; i < 70; i++) {
            int sx = px + 6 + rnd.nextInt(Math.max(1, pw - 12));
            int sy = py + 6 + rnd.nextInt(Math.max(1, ph - 12));
            int a = 0x22 + rnd.nextInt(0x40);
            g.fill(sx, sy, sx + 1, sy + 1, (a << 24) | 0xFFFFFF);
        }

        // Titel der Seite
        big(g, tr, Fx.ui(TAB_NAMES[tab]), px + 16, py + 40, 1.7f, TEXT);
        g.drawText(tr, Fx.ui(TAB_SUB[tab]), px + 16, py + 60, MUTED, false);

        // rechts oben: kurzer Status
        int dot = statusColor();
        String right = LyricsState.lines.isEmpty() ? shortStatus() : LyricsState.lines.size() + " Zeilen bereit";
        Text rt = Fx.ui(Fx.fit(tr, right, pw / 2 - 30));
        int rw = tr.getWidth(rt);
        rrect(g, px + pw - 16 - rw - 14, py + 46, 6, 6, 3, dot);
        g.drawText(tr, rt, px + pw - 16 - rw, py + 45, MUTED, false);

        if (tab == 0) drawOverview(g, tr);
        else if (tab == 1) drawDisplayCards(g);
        else drawConnection(g, tr);
    }

    private void drawOverview(DrawContext g, TextRenderer tr) {
        int colW = (gw - 8) / 2;
        // Karte "Jetzt läuft"
        glass(g, gx, y0, gw, 62, false);
        g.drawText(tr, Fx.ui("JETZT LÄUFT"), gx + 12, y0 + 9, MUTED, false);
        String title = LyricsState.title.isEmpty() ? "Nichts erkannt" : LyricsState.title;
        String artist = LyricsState.artist.isEmpty() ? "Starte Spotify (App oder Browser)" : LyricsState.artist;
        big(g, tr, Fx.ui(Fx.fit(tr, title, (int) ((gw - 24) / 1.3f))), gx + 12, y0 + 20, 1.3f, TEXT);
        g.drawText(tr, Fx.ui(Fx.fit(tr, artist, gw - 24)), gx + 12, y0 + 36, MUTED, false);

        long dur = LyricsState.durationMs;
        float frac = dur > 0 ? Math.max(0f, Math.min(1f, LyricsState.now() / (float) dur)) : 0f;
        int bx = gx + 12, bw = gw - 24, by = y0 + 52;
        rrect(g, bx, by, bw, 4, 2, OFF);
        if (frac > 0.01f) rrectGradient(g, bx, by, Math.max(4, (int) (bw * frac)), 4, 2, accentLight(), accent());

        // Karte "Status" rechts neben dem Lyrics-Schalter
        int sx = gx + colW + 8;
        glass(g, sx, y0 + 70, colW, 48, false);
        rrect(g, sx + 10, y0 + 79, 6, 6, 3, statusColor());
        g.drawText(tr, Fx.ui("Status"), sx + 22, y0 + 76, TEXT, false);
        String s = LyricsState.status;
        String l1 = Fx.fit(tr, s, colW - 20);
        g.drawText(tr, Fx.ui(l1), sx + 10, y0 + 91, MUTED, false);
        if (s.length() > l1.length() && !l1.endsWith("...")) {
            g.drawText(tr, Fx.ui(Fx.fit(tr, s.substring(l1.length()).trim(), colW - 20)), sx + 10, y0 + 102, MUTED, false);
        }
    }

    private void drawDisplayCards(DrawContext g) {
        glass(g, gx, y0 + 56, gw, 34, false);
        glass(g, gx, y0 + 98, gw, 34, false);
        glass(g, gx, y0 + 140, gw, 34, false);
    }

    private void drawConnection(DrawContext g, TextRenderer tr) {
        boolean spotify = LyricsState.source == 1;
        int h = spotify ? 150 : 100;
        glass(g, gx, y0 + 56, gw, h, false);
        int ty = y0 + 66;
        if (!spotify) {
            String[] t = {
                "Liest den Song aus der Windows-Medienanzeige.",
                "Funktioniert mit der Spotify-App und mit Spotify",
                "im Browser. Kein Login und kein Premium nötig."
            };
            for (int i = 0; i < t.length; i++) {
                g.drawText(tr, Fx.ui(Fx.fit(tr, t[i], gw - 24)), gx + 12, ty + i * 12, i == 0 ? TEXT : MUTED, false);
            }
            g.drawText(tr, Fx.ui(Fx.fit(tr, "Status: " + LyricsState.status, gw - 24)), gx + 12, ty + 52, MUTED, false);
        } else {
            g.drawText(tr, Fx.ui("Client ID deiner Spotify-App"), gx + 12, ty, TEXT, false);
            g.drawText(tr, Fx.ui(Fx.fit(tr, "Redirect URI: http://127.0.0.1:8888/callback", gw - 24)), gx + 12, ty + 12, MUTED, false);
            g.drawText(tr, Fx.ui(Fx.fit(tr, "Seit 2026 braucht der App-Besitzer Premium.", gw - 24)), gx + 12, ty + 24, MUTED, false);
            g.drawText(tr, Fx.ui(Fx.fit(tr, "Status: " + LyricsState.status, gw - 24)), gx + 12, y0 + 56 + h - 14, MUTED, false);
        }
    }

    private static String shortStatus() {
        String s = LyricsState.status;
        if (s.startsWith("Lade")) return "Lyrics werden geladen";
        if (s.startsWith("Keine Wiedergabe")) return "Keine Wiedergabe";
        if (s.startsWith("Keine synchron")) return "Keine Lyrics gefunden";
        if (s.contains("403") || s.contains("blockiert")) return "Spotify blockiert";
        return s.length() > 30 ? "Wartet ..." : s;
    }

    private static int statusColor() {
        String s = LyricsState.status;
        if (!LyricsState.lines.isEmpty()) return 0xFF4ADE80;
        if (s.startsWith("Lade") || s.startsWith("Suche") || s.startsWith("Verbinde") || s.startsWith("Browser")) return 0xFFFBBF24;
        if (s.contains("Fehler") || s.contains("Keine") || s.contains("403") || s.contains("blockiert")) return 0xFFF87171;
        return 0xFF9A8FB8;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ------------------------------------------------------------------ Hilfsfunktionen

    private static int lerp(int a, int b, float t) {
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
            | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static int inset(int i, int h, int r) {
        if (i < r) { double dy = r - i - 0.5; return (int) Math.round(r - Math.sqrt(r * r - dy * dy)); }
        if (i >= h - r) { double dy = i - (h - r) + 0.5; return (int) Math.round(r - Math.sqrt(r * r - dy * dy)); }
        return 0;
    }

    static void rrect(DrawContext g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { g.fill(x, y, x + w, y + h, color); return; }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(i, h, r);
            g.fill(x + in, y + i, x + w - in, y + i + 1, color);
            g.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, color);
        }
    }

    static void rrectGradient(DrawContext g, int x, int y, int w, int h, int r, int top, int bottom) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int in = r == 0 ? 0 : inset(i, h, r);
            g.fill(x + in, y + i, x + w - in, y + i + 1, lerp(top, bottom, h <= 1 ? 0f : i / (float) (h - 1)));
        }
    }

    /** Glas-Karte: heller Rand, dunkle, leicht durchsichtige Füllung. */
    static void glass(DrawContext g, int x, int y, int w, int h, boolean active) {
        rrect(g, x, y, w, h, 8, active ? ((accent() & 0x00FFFFFF) | 0xB0000000) : 0x30FFFFFF);
        rrect(g, x + 1, y + 1, w - 2, h - 2, 7, 0xA0140C24);
    }

    static void big(DrawContext g, TextRenderer tr, Text s, int x, int y, float scale, int color) {
        Matrix3x2fStack m = g.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        g.drawText(tr, s, 0, 0, color, false);
        m.popMatrix();
    }

    /** Kleine, einfache Symbole aus Rechtecken. */
    static void icon(DrawContext g, int id, int x, int y, int c) {
        switch (id) {
            case 0 -> { g.fill(x + 1, y + 5, x + 3, y + 11, c); g.fill(x + 5, y + 2, x + 7, y + 11, c); g.fill(x + 9, y + 6, x + 11, y + 11, c); }
            case 1 -> { g.fill(x, y + 3, x + 12, y + 4, c); g.fill(x, y + 8, x + 12, y + 9, c);
                        rrect(g, x + 7, y + 1, 3, 5, 1, c); rrect(g, x + 2, y + 6, 3, 5, 1, c); }
            case 2 -> { rrect(g, x, y + 3, 5, 5, 2, c); rrect(g, x + 7, y + 3, 5, 5, 2, c); g.fill(x + 4, y + 5, x + 8, y + 6, c); }
            case 3 -> { rrect(g, x + 2, y + 1, 8, 8, 2, c); g.fill(x + 3, y + 10, x + 10, y + 11, c); }
            case 4 -> { rrect(g, x, y + 1, 12, 9, 2, (c & 0x00FFFFFF) | 0x55000000); g.fill(x + 2, y + 7, x + 10, y + 9, c); }
            case 5 -> { for (int i = 0; i < 5; i++) g.fill(x + 3 + i, y + 1 + i, x + 4 + i, y + 10 - i, c); g.fill(x + 2, y + 1, x + 3, y + 10, c); }
            default -> { rrect(g, x + 1, y + 1, 10, 10, 5, c); g.fill(x + 4, y + 4, x + 8, y + 5, 0xFF0B0814); g.fill(x + 3, y + 6, x + 9, y + 7, 0xFF0B0814); g.fill(x + 4, y + 8, x + 8, y + 9, 0xFF0B0814); }
        }
    }

    // ------------------------------------------------------------------ Bedienelemente

    private abstract static class Base extends ClickableWidget {
        final int bx, by, bw, bh;
        int mx, my;

        Base(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty());
            this.bx = x; this.by = y; this.bw = w; this.bh = h;
        }

        boolean over() { return mx >= bx && mx < bx + bw && my >= by && my < by + bh; }

        @Override
        public void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
            mx = mouseX; my = mouseY;
            paint(g);
        }

        abstract void paint(DrawContext g);

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    /** Navigation oben: aktive Seite als Pille mit Text, die anderen als runde Symbole. */
    private final class NavButton extends Base {
        final TextRenderer tr;
        final int idx;

        NavButton(TextRenderer tr, int x, int y, int idx) {
            super(x, y, idx == tab ? 26 + tr.getWidth(Fx.ui(TAB_NAMES[idx])) + 8 : 22, 22);
            this.tr = tr; this.idx = idx;
        }

        @Override
        void paint(DrawContext g) {
            boolean sel = tab == idx;
            if (sel) {
                rrect(g, bx - 1, by - 1, bw + 2, bh + 2, 12, (accentLight() & 0x00FFFFFF) | 0x66000000);
                rrectGradient(g, bx, by, bw, bh, 11, accentLight(), accentDark());
                icon(g, idx == 0 ? 0 : idx == 1 ? 1 : 2, bx + 8, by + 5, 0xFFFFFFFF);
                g.drawText(tr, Fx.ui(TAB_NAMES[idx]), bx + 24, by + 7, 0xFFFFFFFF, false);
            } else {
                rrect(g, bx, by, bw, bh, 11, over() ? 0x40FFFFFF : 0x22FFFFFF);
                icon(g, idx == 0 ? 0 : idx == 1 ? 1 : 2, bx + 5, by + 5, over() ? 0xFFFFFFFF : MUTED);
            }
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            if (tab != idx) {
                tab = idx;
                LyricsScreen.this.clearAndInit();
            }
        }
    }

    /** Modul-Karte wie im Vorbild: Symbol-Kachel, Titel, Beschreibung, Schalter. */
    private static final class Card extends Base {
        final TextRenderer tr;
        final int icon;
        final String title, desc;
        final BooleanSupplier on;
        final boolean showSwitch;
        final Runnable action;

        Card(TextRenderer tr, int x, int y, int w, int h, int icon, String title, String desc,
             BooleanSupplier on, boolean showSwitch, Runnable action) {
            super(x, y, w, h);
            this.tr = tr; this.icon = icon; this.title = title; this.desc = desc;
            this.on = on; this.showSwitch = showSwitch; this.action = action;
        }

        @Override
        void paint(DrawContext g) {
            boolean active = on.getAsBoolean();
            glass(g, bx, by, bw, bh, active);
            if (over()) rrect(g, bx + 1, by + 1, bw - 2, bh - 2, 7, 0x18FFFFFF);

            // Symbol-Kachel
            if (active) rrectGradient(g, bx + 8, by + 8, 22, 22, 6, accentLight(), accentDark());
            else rrect(g, bx + 8, by + 8, 22, 22, 6, 0x26FFFFFF);
            icon(g, icon, bx + 13, by + 13, active ? 0xFFFFFFFF : MUTED);

            // Schalter oben rechts
            if (showSwitch) {
                int sx = bx + bw - 34, sy = by + 11;
                if (active) rrectGradient(g, sx, sy, 26, 14, 7, accentLight(), accent());
                else rrect(g, sx, sy, 26, 14, 7, OFF);
                rrect(g, active ? sx + 13 : sx + 1, sy + 1, 12, 12, 6, 0xFFFFFFFF);
            }

            g.drawText(tr, Fx.ui(Fx.fit(tr, title, bw - 16)), bx + 10, by + bh - 18, TEXT, false);
            g.drawText(tr, Fx.ui(Fx.fit(tr, desc, bw - 16)), bx + 10, by + bh - 9, MUTED, false);
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            action.run();
        }
    }

    private static final class Swatches extends Base {
        final TextRenderer tr;

        Swatches(TextRenderer tr, int x, int y, int w, int h) {
            super(x, y, w, h);
            this.tr = tr;
        }

        private int startX() {
            int n = LyricsState.THEMES.length;
            return bx + bw - 14 - (n * 16 + (n - 1) * 6);
        }

        @Override
        void paint(DrawContext g) {
            g.drawText(tr, Fx.ui("Farbe"), bx + 12, by + (bh - 8) / 2, TEXT, false);
            int x = startX();
            for (int i = 0; i < LyricsState.THEMES.length; i++) {
                boolean sel = LyricsState.themeIdx == i;
                if (sel) rrect(g, x - 2, by + (bh - 20) / 2, 20, 20, 7, 0xFFFFFFFF);
                rrect(g, x, by + (bh - 16) / 2, 16, 16, 5, 0xFF000000 | LyricsState.THEMES[i].glow());
                x += 22;
            }
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            int x = startX();
            for (int i = 0; i < LyricsState.THEMES.length; i++) {
                if (mx >= x - 2 && mx < x + 18) { LyricsState.themeIdx = i; LyricsState.saveSettings(); return; }
                x += 22;
            }
        }
    }

    private static final class PillButton extends Base {
        final TextRenderer tr;
        final String text;
        final Runnable action;

        PillButton(TextRenderer tr, int x, int y, int w, int h, String text, Runnable action) {
            super(x, y, w, h);
            this.tr = tr; this.text = text; this.action = action;
        }

        @Override
        void paint(DrawContext g) {
            rrectGradient(g, bx, by, bw, bh, 8, over() ? accentLight() : accent(), accentDark());
            Text t = Fx.ui(text);
            g.drawText(tr, t, bx + (bw - tr.getWidth(t)) / 2, by + (bh - 8) / 2, 0xFFFFFFFF, false);
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            action.run();
        }
    }

    /** Regler: Beschriftung links, Wert rechts, Schiene in der Mitte. */
    private static final class Slide extends SliderWidget {
        final TextRenderer tr;
        final int bx, by, bw, bh, trackX, trackW;
        final String label;
        final double min, max;
        final DoubleConsumer onChange;
        final DoubleFunction<String> fmt;

        Slide(TextRenderer tr, int x, int y, int w, int h, String label, double min, double max, double value,
              DoubleConsumer onChange, DoubleFunction<String> fmt) {
            super(x + 80 - 4, y, w - 80 - 70 + 8, h, Text.empty(), Math.max(0.0, Math.min(1.0, (value - min) / (max - min))));
            this.tr = tr;
            this.bx = x; this.by = y; this.bw = w; this.bh = h;
            this.trackX = x + 80;
            this.trackW = w - 80 - 70;
            this.label = label; this.min = min; this.max = max;
            this.onChange = onChange; this.fmt = fmt;
        }

        @Override
        protected void updateMessage() {}

        @Override
        protected void applyValue() {
            onChange.accept(min + (max - min) * this.value);
        }

        @Override
        public void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
            g.drawText(tr, Fx.ui(label), bx + 12, by + (bh - 8) / 2, TEXT, false);
            String v = fmt == null ? "" : fmt.apply(min + (max - min) * this.value);
            Text vt = Fx.ui(v);
            g.drawText(tr, vt, bx + bw - 12 - tr.getWidth(vt), by + (bh - 8) / 2, MUTED, false);
            int ty = by + bh / 2 - 2;
            rrect(g, trackX, ty, trackW, 4, 2, OFF);
            int fill = (int) (trackW * this.value);
            if (fill > 0) rrectGradient(g, trackX, ty, Math.max(4, fill), 4, 2, accentLight(), accent());
            rrect(g, trackX + fill - 6, by + bh / 2 - 6, 12, 12, 6, 0x55FFFFFF);
            rrect(g, trackX + fill - 5, by + bh / 2 - 5, 10, 10, 5, 0xFFFFFFFF);
        }
    }
}
