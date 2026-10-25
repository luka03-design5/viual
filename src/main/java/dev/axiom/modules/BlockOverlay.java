package dev.axiom.modules;

import dev.axiom.module.Module;
import dev.axiom.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Highlight des anvisierten Blocks (Filled + Outline). */
public final class BlockOverlay extends Module {
    public BlockOverlay() {
        super("BlockOverlay", "Visuals", "Anvisierten Block highlighten");
    }

    @Override
    public void onWorldRender(float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        if (mc.world.getBlockState(pos).isAir()) return;

        Box box = new Box(pos).expand(0.002);
        MatrixStack ms = new MatrixStack();
        // accent cyan/purple
        RenderUtil.filledBox(ms, box, 0.55f, 0.35f, 1f, 0.28f);
        RenderUtil.box(ms, box, 0.75f, 0.55f, 1f, 1f, 2f);
    }
}
