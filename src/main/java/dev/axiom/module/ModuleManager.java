package dev.axiom.module;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.axiom.modules.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    private static final Map<String, Module> MAP = new LinkedHashMap<>();
    private static final List<Module> LIST = new ArrayList<>();

    public static void init() {
        reg(new Hitboxes());
        reg(new BlockOverlay());
        reg(new Fullbright());
        reg(new StorageEsp());
        reg(new Tracers());
        load();
    }

    private static void reg(Module m) {
        MAP.put(m.name().toLowerCase(), m);
        LIST.add(m);
    }

    public static List<Module> all() { return LIST; }

    public static Module get(String name) { return MAP.get(name.toLowerCase()); }

    public static void tick() {
        for (Module m : LIST) if (m.enabled()) m.onTick();
    }

    public static void worldRender(float td) {
        for (Module m : LIST) if (m.enabled()) m.onWorldRender(td);
    }

    public static void hud(net.minecraft.client.gui.DrawContext g, float td) {
        for (Module m : LIST) if (m.enabled()) m.onHud(g, td);
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("axiom-visuals.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (Module m : LIST) {
                if (o.has(m.name()) && o.get(m.name()).getAsBoolean()) {
                    m.setEnabled(true);
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            for (Module m : LIST) o.addProperty(m.name(), m.enabled());
            Files.writeString(file(), o.toString());
        } catch (Exception ignored) {}
    }

    private ModuleManager() {}
}
