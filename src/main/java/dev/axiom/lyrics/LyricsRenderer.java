package dev.axiom.lyrics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Zeichnet die Lyrics.
 * Modus 0: Jede Zeile wird in Wortgruppen zerlegt, die verstreut und leicht geneigt in der Luft vor dir hängen.
 *          Die Gruppen erscheinen nacheinander. Die Welt-Position wird pro Frame auf den Bildschirm projiziert.
 * Modus 1: einfache Anzeige unten am Bildschirm.
 * Alles in glatter Poppins-Schrift (Fx.lyrics).
 */
public final class LyricsRenderer {
    private static final float UNIT = 0.075f;      // Weltmaß (Blöcke) pro Schrift-Einheit bei Größe 100 %
    private static final float SPACE = 3.6f;       // Wortabstand in Schrift-Einheiten
    private static final float HALF_H = 6f;        // halbe Texthöhe (Schriftgröße 12)
    private static final float ROW_SPAN = 118f;    // maximale Breite einer Reihe

    private record Chunk(List<Text> words, float[] widths, float width, int firstWord,
                         float cx, float cy, float roll, float tilt) {}
    private record Phrase(Vec3d anchor, Vector3f right, Vector3f up, Vector3f normal,
                          List<Chunk> chunks, long start, long dur, float step) {}

    private static final Map<Integer, Phrase> PHRASES = new HashMap<>();
    private static String cacheTrack = "";
    private static String lastStatus = "";
    private static long statusSince = 0;

    public static void render(DrawContext g, RenderTickCounter tc) {
        if (!LyricsState.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        TextRenderer tr = mc.textRenderer;
        int W = g.getScaledWindowWidth(), H = g.getScaledWindowHeight();

        String track = LyricsState.trackId;
        if (!track.equals(cacheTrack)) { PHRASES.clear(); cacheTrack = track; }

        List<LyricsState.LrcLine> lines = LyricsState.lines;
        if (lines.isEmpty()) { drawStatus(g, tr, W, H); return; }

        long t = LyricsState.now() + LyricsState.offsetMs;
        int cur = LyricsState.indexAt(lines, t);
        if (cur < 0) return;

        if (LyricsState.mode == 1) drawBottom(g, tr, lines, cur, W, H);
        else draw3d(g, mc, tr, lines, cur, t, W, H, tc.getTickProgress(true));
    }

    // ------------------------------------------------------------------ Status

    private static void drawStatus(DrawContext g, TextRenderer tr, int W, int H) {
        String s = LyricsState.status;
        long now = System.currentTimeMillis();
        if (!s.equals(lastStatus)) { lastStatus = s; statusSince = now; }
        if (now - statusSince > 8000) return;          // nur kurz nach einer Änderung zeigen
        String text = Fx.fit(tr, s, W - 20);
        Text tx = Fx.ui(text);
        g.drawText(tr, tx, (W - tr.getWidth(tx)) / 2, H - 64, 0xFFC9B3FF, true);
    }

    // ------------------------------------------------------------------ Modus 1: unten

    private static void drawBottom(DrawContext g, TextRenderer tr, List<LyricsState.LrcLine> lines,
                                   int cur, int W, int H) {
        LyricsState.Theme th = LyricsState.theme();
        String prev = cur > 0 ? lines.get(cur - 1).text() : "";
        String now = lines.get(cur).text();
        String next = cur + 1 < lines.size() ? lines.get(cur + 1).text() : "";
        float k = LyricsState.size;
        int base = H - (int) (86 * Math.min(1.5f, k));
        centered(g, tr, prev, W / 2f, base - 20 * k, 0.9f * k, argb(150, th.past()), argb(0, th.glow()));
        centered(g, tr, now, W / 2f, base, 1.8f * k, argb(255, th.front()), argb(70, th.glow()));
        centered(g, tr, next, W / 2f, base + 28 * k, 0.9f * k, argb(150, th.past()), argb(0, th.glow()));
    }

    private static void centered(DrawContext g, TextRenderer tr, String s, float cx, float y, float scale,
                                 int color, int glow) {
        if (s == null || s.isBlank()) return;
        Text tx = Fx.lyrics(s);
        int w = tr.getWidth(tx);
        Matrix3x2fStack m = g.getMatrices();
        m.pushMatrix();
        m.translate(cx, y);
        m.scale(scale, scale);
        if ((glow >>> 24) > 8) {
            for (int i = 1; i <= 3; i++) {
                m.pushMatrix();
                m.translate(0f, HALF_H);
                m.scale(1f + 0.05f * i, 1f + 0.05f * i);
                m.translate(0f, -HALF_H);
                g.drawText(tr, tx, -w / 2, 0, glow, false);
                m.popMatrix();
            }
        }
        g.drawText(tr, tx, -w / 2, 0, color, false);
        m.popMatrix();
    }

    // ------------------------------------------------------------------ Modus 0: 3D

    private static void draw3d(DrawContext g, MinecraftClient mc, TextRenderer tr,
                               List<LyricsState.LrcLine> lines, int cur, long t, int W, int H, float tickProgress) {
        PHRASES.keySet().removeIf(k -> k < cur - 2 || k > cur);

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = mc.player.getCameraPosVec(tickProgress);
        Quaternionf q = cam.getRotation();
        Quaternionf inv = new Quaternionf(q).conjugate();

        float fov = mc.options.getFov().getValue();
        float tanHalf = (float) Math.tan(Math.toRadians(fov) / 2.0);
        float aspect = (float) W / (float) H;

        for (int k = Math.max(0, cur - 2); k <= cur; k++) {
            Phrase p = PHRASES.get(k);
            if (p == null) {
                if (k != cur) continue;                     // nur die aktuelle Zeile neu erzeugen
                p = spawn(k, lines, camPos, q, tr);
                if (p == null) continue;
                PHRASES.put(k, p);
            }
            drawPhrase(g, tr, p, t, camPos, inv, tanHalf, aspect, W, H);
        }
    }

    private static Phrase spawn(int k, List<LyricsState.LrcLine> lines, Vec3d camPos, Quaternionf q, TextRenderer tr) {
        LyricsState.LrcLine ln = lines.get(k);
        String text = ln.text().trim();
        if (text.isEmpty()) return null;

        long end = k + 1 < lines.size() ? lines.get(k + 1).ms() : ln.ms() + 4000;
        long dur = Math.max(1200, Math.min(6000, end - ln.ms()));
        Random rnd = new Random(31L * k + LyricsState.trackId.hashCode());

        // 1) Wörter zu Gruppen von 1-2 Wörtern (kurze Wörter bis 3)
        String[] ws = text.split("\\s+");
        List<List<String>> groups = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        int chars = 0;
        for (String w : ws) {
            int limit = (w.length() <= 3 && cur.size() < 3) ? 3 : 2;
            if (!cur.isEmpty() && (cur.size() >= limit || chars + 1 + w.length() > 15)) {
                groups.add(cur);
                cur = new ArrayList<>();
                chars = 0;
            }
            cur.add(w);
            chars += (chars == 0 ? 0 : 1) + w.length();
        }
        if (!cur.isEmpty()) groups.add(cur);

        // 2) Gruppen messen und in Reihen verteilen
        List<Chunk> chunks = new ArrayList<>();
        List<List<float[]>> rowsMeta = new ArrayList<>();
        List<float[]> rowCur = new ArrayList<>();
        List<List<Integer>> rowIdx = new ArrayList<>();
        List<Integer> idxCur = new ArrayList<>();
        float rowW = 0;
        int gi = 0;
        List<float[]> widthsList = new ArrayList<>();
        List<List<Text>> textsList = new ArrayList<>();
        List<Integer> firstList = new ArrayList<>();
        for (List<String> grp : groups) {
            float[] wd = new float[grp.size()];
            List<Text> tx = new ArrayList<>();
            float tot = 0;
            for (int i = 0; i < grp.size(); i++) {
                Text t = Fx.lyrics(grp.get(i));
                tx.add(t);
                wd[i] = tr.getWidth(t);
                tot += wd[i] + (i > 0 ? SPACE : 0f);
            }
            widthsList.add(wd);
            textsList.add(tx);
            firstList.add(gi);
            gi += grp.size();
            float[] meta = {tot};
            if (!rowCur.isEmpty() && rowW + 16f + tot > ROW_SPAN) {
                rowsMeta.add(rowCur); rowIdx.add(idxCur);
                rowCur = new ArrayList<>(); idxCur = new ArrayList<>(); rowW = 0;
            }
            rowW += (rowCur.isEmpty() ? 0f : 16f) + tot;
            rowCur.add(meta);
            idxCur.add(widthsList.size() - 1);
        }
        if (!rowCur.isEmpty()) { rowsMeta.add(rowCur); rowIdx.add(idxCur); }

        float dir = (k % 2 == 0) ? 1f : -1f;                 // absteigende oder aufsteigende Diagonale
        int nRows = rowsMeta.size();
        for (int r = 0; r < nRows; r++) {
            List<float[]> meta = rowsMeta.get(r);
            List<Integer> idx = rowIdx.get(r);
            float span = 0;
            for (int j = 0; j < meta.size(); j++) span += meta.get(j)[0] + (j > 0 ? 16f : 0f);
            float x = -span / 2f;
            float rowY = (r - (nRows - 1) / 2f) * 26f;
            for (int j = 0; j < meta.size(); j++) {
                float w = meta.get(j)[0];
                int ci = idx.get(j);
                float cx = x + w / 2f + (rnd.nextFloat() - 0.5f) * 8f;
                float cy = rowY + (j - (meta.size() - 1) / 2f) * 14f * dir + (rnd.nextFloat() - 0.5f) * 8f;
                float roll = (float) Math.toRadians((rnd.nextFloat() - 0.5f) * 12f);
                float tilt = (float) Math.toRadians((rnd.nextFloat() - 0.5f) * 80f);
                chunks.add(new Chunk(textsList.get(ci), widthsList.get(ci), w, firstList.get(ci), cx, cy, roll, tilt));
                x += w + 16f;
            }
        }
        chunks.sort((a, b) -> Integer.compare(a.firstWord(), b.firstWord()));

        float step = Math.min(420f, dur * 0.7f / Math.max(1, ws.length));

        // 3) Ort: vor dem Spieler, seitlich versetzt, etwas höher
        Vector3f fwd = q.transform(new Vector3f(0f, 0f, -1f));
        Vector3f right = q.transform(new Vector3f(1f, 0f, 0f));
        Vector3f up = q.transform(new Vector3f(0f, 1f, 0f));
        double dist = 9.0 + rnd.nextDouble() * 3.0;
        double side = (k % 2 == 0 ? -1 : 1) * (0.05 + rnd.nextDouble() * 0.22);
        double lift = 0.02 + rnd.nextDouble() * 0.22;
        Vec3d pos = camPos.add(
            fwd.x * dist + right.x * side * dist,
            fwd.y * dist + right.y * side * dist + lift * dist,
            fwd.z * dist + right.z * side * dist);

        return new Phrase(pos, right, up, fwd, chunks, ln.ms(), dur, step);
    }

    private static void drawPhrase(DrawContext g, TextRenderer tr, Phrase p, long t, Vec3d camPos,
                                   Quaternionf inv, float tanHalf, float aspect, int W, int H) {
        if (t < p.start()) return;
        long end = p.start() + p.dur();

        float fade = 1f;
        long fadeStart = end + 900;
        if (t > fadeStart) fade = 1f - (t - fadeStart) / 1400f;
        if (fade <= 0.02f) return;

        LyricsState.Theme th = LyricsState.theme();
        int main = t > end ? th.past() : th.front();
        float unit = UNIT * LyricsState.size;

        for (Chunk c : p.chunks()) {
            float x = -c.width() / 2f;
            float cosT = (float) Math.cos(c.tilt()), sinT = (float) Math.sin(c.tilt());
            for (int w = 0; w < c.words().size(); w++) {
                float ww = c.widths()[w];
                int gi = c.firstWord() + w;
                float prog = (t - (p.start() + gi * p.step())) / 260f;
                float dx = x + ww / 2f;                         // Mitte des Wortes in der Gruppe
                x += ww + SPACE;
                if (prog <= 0f) continue;

                float e = 1f - (float) Math.pow(1f - Math.min(1f, prog), 3);
                int alpha = (int) (255 * e * fade);
                if (alpha < 8) continue;

                // Mitte des Wortes in Schrift-Einheiten (y nach unten)
                float px = c.cx() + dx * (float) Math.cos(c.roll());
                float py = c.cy() + dx * (float) Math.sin(c.roll()) + (1f - e) * 6f
                    + (float) Math.sin((t + gi * 230L) / 650.0) * 0.9f;

                double wx = p.anchor().x + (p.right().x * px - p.up().x * py) * unit;
                double wy = p.anchor().y + (p.right().y * px - p.up().y * py) * unit;
                double wz = p.anchor().z + (p.right().z * px - p.up().z * py) * unit;

                Vector3f v = new Vector3f((float) (wx - camPos.x), (float) (wy - camPos.y), (float) (wz - camPos.z));
                float len = v.length();
                inv.transform(v);
                float depth = -v.z;
                if (depth < 0.6f || depth > 70f) continue;     // hinter dir oder zu weit weg
                float sx = W * 0.5f * (1f + v.x / (depth * tanHalf * aspect));
                float sy = H * 0.5f * (1f - v.y / (depth * tanHalf));
                if (sx < -250 || sx > W + 250 || sy < -120 || sy > H + 120) continue;

                float s = (H * 0.5f) / (tanHalf * depth) * unit;   // Bildschirm-Pixel pro Schrift-Einheit
                if (s < 0.2f) continue;
                float pop = 0.75f + 0.25f * e;
                float dot = Math.abs(((float) (wx - camPos.x) * p.normal().x
                    + (float) (wy - camPos.y) * p.normal().y
                    + (float) (wz - camPos.z) * p.normal().z) / Math.max(0.001f, len));
                float squish = Math.max(0.35f, Math.min(1f, dot)) * Math.max(0.55f, Math.abs(cosT));
                float skew = sinT * 0.28f;                          // Schrägstellung wie bei einer gedrehten Fläche

                drawWord(g, tr, c.words().get(w), ww, sx, sy, s * squish * pop, s * pop,
                    c.roll(), skew, alpha, main, th);
            }
        }
    }

    private static void drawWord(DrawContext g, TextRenderer tr, Text word, float ww,
                                 float sx, float sy, float scaleX, float scaleY, float roll, float skew,
                                 int alpha, int main, LyricsState.Theme th) {
        Matrix3x2fStack m = g.getMatrices();
        m.pushMatrix();
        m.translate(sx, sy);
        m.rotate(roll);
        m.mul(new Matrix3x2f(1f, skew, 0f, 1f, 0f, 0f));
        m.scale(scaleX, scaleY);

        // weiches Leuchten: mehrere leicht vergrößerte Kopien in der Leuchtfarbe
        for (int i = 4; i >= 1; i--) {
            int ga = (int) (alpha * (0.075f + 0.02f * (5 - i)));
            if (ga < 8) continue;
            m.pushMatrix();
            m.scale(1f + 0.045f * i, 1f + 0.06f * i);
            g.drawText(tr, word, (int) (-ww / 2f), (int) -HALF_H, argb(ga, th.glow()), false);
            m.popMatrix();
        }
        // dezenter Schatten für Kontrast
        g.drawText(tr, word, (int) (-ww / 2f + 1f), (int) (-HALF_H + 1.2f), argb((int) (alpha * 0.45f), th.depth(3)), false);
        // Vorderseite
        g.drawText(tr, word, (int) (-ww / 2f), (int) -HALF_H, argb(alpha, main), false);

        m.popMatrix();
    }

    private static int argb(int a, int rgb) {
        return (Math.min(255, Math.max(0, a)) << 24) | (rgb & 0xFFFFFF);
    }

    private LyricsRenderer() {}
}
