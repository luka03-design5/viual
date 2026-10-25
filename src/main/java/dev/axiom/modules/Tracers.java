package dev.axiom.modules;

import dev.axiom.module.Module;
import dev.axiom.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Linien zu Spielern. */
public final class Tracers extends Module {
    public Tracers() {
        super("Tracers", "Visuals", "Linien zu Spielern");
    }

    @Override
    public void onWorldRender(float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        Vec3d eyes = mc.player.getCameraPosVec(tickDelta);
        MatrixStack ms = new MatrixStack();
        for (Entity e : mc.world.getPlayers()) {
            if (e == mc.player || !(e instanceof PlayerEntity)) continue;
            if (e.squaredDistanceTo(mc.player) > 128 * 128) continue;
            Vec3d to = e.getBoundingBox().getCenter();
            RenderUtil.line3d(ms, eyes, to, 1f, 0.3f, 0.35f, 0.85f);
        }
    }
}
