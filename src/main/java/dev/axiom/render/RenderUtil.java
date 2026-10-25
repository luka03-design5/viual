package dev.axiom.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** 3D box / line helpers for world overlay (hitboxes, block outline, tracers). */
public final class RenderUtil {

    public static Vec3d cam() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.gameRenderer.getCamera().getPos();
    }

    public static void box(MatrixStack ms, Box box, float r, float g, float b, float a, float lineWidth) {
        Vec3d c = cam();
        Box b2 = box.offset(-c.x, -c.y, -c.z);
        Matrix4f m = ms.peek().getPositionMatrix();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float x0 = (float) b2.minX, y0 = (float) b2.minY, z0 = (float) b2.minZ;
        float x1 = (float) b2.maxX, y1 = (float) b2.maxY, z1 = (float) b2.maxZ;

        // bottom
        line(buf, m, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buf, m, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buf, m, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buf, m, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // top
        line(buf, m, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buf, m, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buf, m, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buf, m, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // pillars
        line(buf, m, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buf, m, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buf, m, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buf, m, x0, y0, z1, x0, y1, z1, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public static void filledBox(MatrixStack ms, Box box, float r, float g, float b, float a) {
        Vec3d c = cam();
        Box b2 = box.offset(-c.x, -c.y, -c.z);
        Matrix4f m = ms.peek().getPositionMatrix();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float x0 = (float) b2.minX, y0 = (float) b2.minY, z0 = (float) b2.minZ;
        float x1 = (float) b2.maxX, y1 = (float) b2.maxY, z1 = (float) b2.maxZ;

        // -Y
        quad(buf, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        // +Y
        quad(buf, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        // -Z
        quad(buf, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        // +Z
        quad(buf, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        // -X
        quad(buf, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // +X
        quad(buf, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public static void line3d(MatrixStack ms, Vec3d from, Vec3d to, float r, float g, float b, float a) {
        Vec3d c = cam();
        Matrix4f m = ms.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        line(buf, m,
            (float) (from.x - c.x), (float) (from.y - c.y), (float) (from.z - c.z),
            (float) (to.x - c.x), (float) (to.y - c.y), (float) (to.z - c.z),
            r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void line(BufferBuilder buf, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float r, float g, float b, float a) {
        buf.vertex(m, x0, y0, z0).color(r, g, b, a);
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
    }

    private static void quad(BufferBuilder buf, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float r, float g, float b, float a) {
        buf.vertex(m, x0, y0, z0).color(r, g, b, a);
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
        buf.vertex(m, x3, y3, z3).color(r, g, b, a);
    }

    private RenderUtil() {}
}
