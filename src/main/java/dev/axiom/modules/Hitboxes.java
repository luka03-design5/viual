package dev.axiom.modules;

import dev.axiom.module.Module;
import dev.axiom.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

/** Zeichnet Entity-Hitboxen (Spieler andersfarbig). */
public final class Hitboxes extends Module {
    public Hitboxes() {
        super("Hitboxes", "Visuals", "Entity-Hitboxen anzeigen");
    }

    @Override
    public void onWorldRender(float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        MatrixStack ms = new MatrixStack();
        // depth off for overlay feel - via render layer; DEBUG_LINES ignore depth somewhat
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            if (!(e instanceof LivingEntity)) continue;
            if (e.squaredDistanceTo(mc.player) > 64 * 64) continue;

            Box box = e.getBoundingBox().expand(0.02);
            boolean player = e instanceof PlayerEntity;
            float r = player ? 1f : 0.35f;
            float g = player ? 0.25f : 0.85f;
            float b = player ? 0.35f : 1f;
            float a = 0.95f;
            RenderUtil.box(ms, box, r, g, b, a, 1.5f);
            // soft fill
            RenderUtil.filledBox(ms, box, r, g, b, 0.12f);
        }
    }
}
