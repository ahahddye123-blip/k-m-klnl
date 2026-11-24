package dev.bombowy.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BombowyConfig {
    public Map<String, Boolean> modules = new HashMap<>();
    public Map<String, String> orig = new HashMap<>();
    public Set<String> favorites = new HashSet<>();

    public boolean lowFire = true;
    public float fireOffset = 0.35f;
    public boolean noVignette = true;
    public boolean noHurtCam = true;

    public float colorVibrance = 1.35f;
    public float colorContrast = 1.12f;
    public int colorTintHex = 0xFFFFFF;
    public float colorTintStrength = 0.0f;

    public int hitboxAimColor = 0xFF3B2F;
    public int hitboxIdleColor = 0xFFFFFF;
    public float hitboxReach = 3.0f;
    public boolean hitboxShowAll = true;

    public boolean potatoMode = true;
    public boolean disableWeather = true;
    public boolean reduceAnimations = true;

    public boolean bombTagEnabled = true;
    public boolean bombTagAllPlayers = false;
    public List<String> bombTagPlayers = new ArrayList<>();

    public static class HudElement {
        public boolean enabled;
        public float x;
        public float y;
        public float scale;
        public float bgOpacity;
        public int textColor;
        public boolean chroma;

        public HudElement() {}

        public HudElement(boolean enabled, float x, float y, float scale, float bgOpacity, int textColor) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.bgOpacity = bgOpacity;
            this.textColor = textColor;
            this.chroma = false;
        }
    }

    public HudElement hudFps = new HudElement(false, 8, 8, 1.0f, 0.0f, 0xFFFFFF);
    public HudElement hudStats = new HudElement(false, 8, 26, 1.0f, 0.25f, 0xFF8A3D);
    public HudElement hudInventory = new HudElement(false, 8, 46, 1.0f, 0.0f, 0xFFFFFF);
    public HudElement hudCooldowns = new HudElement(false, 8, 112, 1.0f, 0.0f, 0xFFFFFF);

    public boolean showCps = true;
    public boolean showPing = true;

    public boolean cdPearl = true;
    public boolean cdShield = true;
    public boolean cdChorus = true;
    public boolean cdGoldenApple = true;
    public boolean cdWindCharge = true;
    public boolean cdOther = true;

    public boolean invShowHotbar = false;
    public boolean invCompact = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BombowyConfig instance = new BombowyConfig();

    public static BombowyConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("bombowyclient.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                BombowyConfig c = GSON.fromJson(Files.readString(p), BombowyConfig.class);
                if (c != null) {
                    if (c.orig == null) c.orig = new HashMap<>();
                    if (c.modules == null) c.modules = new HashMap<>();
                    if (c.favorites == null) c.favorites = new HashSet<>();
                    if (c.bombTagPlayers == null) c.bombTagPlayers = new ArrayList<>();
                    if (c.hudFps == null) c.hudFps = new HudElement(false, 8, 8, 1.0f, 0.0f, 0xFFFFFF);
                    if (c.hudStats == null) c.hudStats = new HudElement(false, 8, 26, 1.0f, 0.25f, 0xFF8A3D);
                    if (c.hudInventory == null) c.hudInventory = new HudElement(false, 8, 46, 1.0f, 0.0f, 0xFFFFFF);
                    if (c.hudCooldowns == null) c.hudCooldowns = new HudElement(false, 8, 112, 1.0f, 0.0f, 0xFFFFFF);
                    instance = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[BombowyClient] Nie udało się wczytać konfiguracji: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[BombowyClient] Nie udało się zapisać konfiguracji: " + e);
        }
    }
}
