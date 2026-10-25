package dev.axiom.modules;

import dev.axiom.module.Module;
import net.minecraft.client.MinecraftClient;

/** Gamma hoch — Höhlen ohne Fackel lesbar. */
public final class Fullbright extends Module {
    private double prev = 1.0;

    public Fullbright() {
        super("Fullbright", "Visuals", "Maximale Helligkeit");
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            prev = mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(16.0);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) mc.options.getGamma().setValue(prev);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && mc.options.getGamma().getValue() < 15.0) {
            mc.options.getGamma().setValue(16.0);
        }
    }
}
