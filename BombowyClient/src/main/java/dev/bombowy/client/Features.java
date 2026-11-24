package dev.bombowy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.particle.ParticlesMode;

import java.util.Map;

public final class Features {
    private Features() {}

    private static Map<String, String> orig() {
        return BombowyConfig.get().orig;
    }

    private static <E extends Enum<E>> void en(String key, SimpleOption<E> opt, E val, boolean on, Class<E> cls) {
        if (on) {
            orig().putIfAbsent(key, opt.getValue().name());
            opt.setValue(val);
        } else {
            String s = orig().remove(key);
            if (s != null) {
                try { opt.setValue(Enum.valueOf(cls, s)); } catch (Exception ignored) { }
            }
        }
    }

    private static void bool(String key, SimpleOption<Boolean> opt, boolean val, boolean on) {
        if (on) {
            orig().putIfAbsent(key, String.valueOf(opt.getValue()));
            opt.setValue(val);
        } else {
            String s = orig().remove(key);
            if (s != null) opt.setValue(Boolean.parseBoolean(s));
        }
    }

    private static void num(String key, SimpleOption<Integer> opt, int val, boolean on, boolean asCap) {
        if (on) {
            orig().putIfAbsent(key, String.valueOf(opt.getValue()));
            int cur = opt.getValue();
            opt.setValue(asCap ? Math.min(cur, val) : val);
        } else {
            String s = orig().remove(key);
            if (s != null) {
                try { opt.setValue(Integer.parseInt(s)); } catch (Exception ignored) { }
            }
        }
    }

    private static void dbl(String key, SimpleOption<Double> opt, double val, boolean on) {
        if (on) {
            orig().putIfAbsent(key, String.valueOf(opt.getValue()));
            opt.setValue(val);
        } else {
            String s = orig().remove(key);
            if (s != null) {
                try { opt.setValue(Double.parseDouble(s)); } catch (Exception ignored) { }
            }
        }
    }

    public static void fpsBoost(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        en("graphics", o.getGraphicsMode(), GraphicsMode.FAST, on, GraphicsMode.class);
        en("particles", o.getParticles(), ParticlesMode.MINIMAL, on, ParticlesMode.class);
        en("clouds", o.getCloudRenderMode(), CloudRenderMode.OFF, on, CloudRenderMode.class);
        bool("shadows", o.getEntityShadows(), false, on);
        bool("fps_ao", o.getAo(), false, on);
    }

    public static void optimization(boolean on) {
        BombowyConfig cfg = BombowyConfig.get();
        GameOptions o = MinecraftClient.getInstance().options;
        en("opt_graphics", o.getGraphicsMode(), GraphicsMode.FAST, on, GraphicsMode.class);
        en("opt_clouds", o.getCloudRenderMode(), CloudRenderMode.OFF, on, CloudRenderMode.class);
        bool("opt_shadows", o.getEntityShadows(), false, on);
        bool("opt_ao", o.getAo(), false, on);
        bool("opt_vsync", o.getEnableVsync(), false, on);

        boolean potato = on && cfg.potatoMode;
        en("opt_particles", o.getParticles(), ParticlesMode.MINIMAL, potato, ParticlesMode.class);
        dbl("opt_entdist", o.getEntityDistanceScaling(), 0.5, potato);
        num("opt_view", o.getViewDistance(), 8, potato, true);
        num("opt_sim", o.getSimulationDistance(), 5, potato, true);
        num("opt_mipmap", o.getMipmapLevels(), 0, potato, false);
        num("opt_blend", o.getBiomeBlendRadius(), 0, potato, false);
        writeOptions();
    }

    public static void unlimitedFps(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        num("maxfps", o.getMaxFps(), 260, on, false);
        bool("vsync", o.getEnableVsync(), false, on);
    }

    public static void entityDistance(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        dbl("entdist", o.getEntityDistanceScaling(), 0.6, on);
    }

    public static void chunkCap(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        num("view", o.getViewDistance(), 12, on, true);
        num("sim", o.getSimulationDistance(), 8, on, true);
    }

    public static void quality(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        num("mipmap", o.getMipmapLevels(), 4, on, false);
        num("blend", o.getBiomeBlendRadius(), 3, on, false);
    }

    public static void writeOptions() {
        MinecraftClient.getInstance().options.write();
    }
}
