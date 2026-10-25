package dev.axiom.modules;

import dev.axiom.module.Module;
import dev.axiom.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Set;

/** Storage ESP: Truhen, Endertruhen, Shulker in Reichweite. */
public final class StorageEsp extends Module {
    private static final int RANGE = 48;
    private final Set<BlockPos> cache = new HashSet<>();
    private int tick;

    public StorageEsp() {
        super("StorageESP", "Visuals", "Truhen / Shulker / Enderchest");
    }

    @Override
    public void onTick() {
        if (++tick % 20 != 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        cache.clear();
        BlockPos origin = mc.player.getBlockPos();
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos p = origin.add(x, y, z);
                    Block b = mc.world.getBlockState(p).getBlock();
                    if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST
                        || b == Blocks.ENDER_CHEST || b == Blocks.BARREL
                        || b == Blocks.SHULKER_BOX
                        || b.name().getString().contains("shulker_box")) {
                        cache.add(p.toImmutable());
                    }
                }
            }
        }
    }

    @Override
    public void onWorldRender(float tickDelta) {
        if (cache.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        MatrixStack ms = new MatrixStack();
        for (BlockPos p : cache) {
            if (p.getSquaredDistance(mc.player.getBlockPos()) > (double) RANGE * RANGE) continue;
            Box box = new Box(p).expand(0.01);
            Block b = mc.world.getBlockState(p).getBlock();
            float r = 1f, g = 0.75f, bl = 0.2f;
            if (b == Blocks.ENDER_CHEST) { r = 0.55f; g = 0.25f; bl = 1f; }
            else if (b.name().getString().contains("shulker")) { r = 0.9f; g = 0.3f; bl = 0.9f; }
            RenderUtil.box(ms, box, r, g, bl, 0.95f, 1.2f);
            RenderUtil.filledBox(ms, box, r, g, bl, 0.15f);
        }
    }
}
