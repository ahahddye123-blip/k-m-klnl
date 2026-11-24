package dev.bombowy.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BombowyHud {
    private BombowyHud() {}

    private static final Map<Item, Long> cooldownStartTime = new LinkedHashMap<>();
    private static final Map<Item, Float> lastSeenProgress = new LinkedHashMap<>();

    public static final class CooldownEntry {
        public final ItemStack stack;
        public final float progress;
        public final float secondsLeft;

        public CooldownEntry(ItemStack stack, float progress, float secondsLeft) {
            this.stack = stack;
            this.progress = progress;
            this.secondsLeft = secondsLeft;
        }
    }

    public static void register() {
        HudRenderCallback.EVENT.register(BombowyHud::onRenderHud);
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            cooldownStartTime.clear();
            lastSeenProgress.clear();
            return;
        }
        ItemCooldownManager mgr = client.player.getItemCooldownManager();
        long now = System.currentTimeMillis();

        List<Item> watched = List.of(
                Items.ENDER_PEARL, Items.SHIELD, Items.CHORUS_FRUIT,
                Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.WIND_CHARGE
        );

        for (Item item : watched) {
            ItemStack dummy = new ItemStack(item);
            if (mgr.isCoolingDown(dummy)) {
                float p = mgr.getCooldownProgress(dummy, 0f);
                Float prev = lastSeenProgress.get(item);
                if (!cooldownStartTime.containsKey(item) || (prev != null && p > prev + 0.05f)) {
                    cooldownStartTime.put(item, now);
                }
                lastSeenProgress.put(item, p);
            } else {
                cooldownStartTime.remove(item);
                lastSeenProgress.remove(item);
            }
        }
    }

    private static void onRenderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.currentScreen instanceof HudEditorScreen) return;

        float td = tickCounter.getTickDelta(false);
        BombowyConfig cfg = BombowyConfig.get();

        if (Modules.isOn("fpshud") || cfg.hudFps.enabled) renderFps(ctx, client, cfg.hudFps, false);
        if (Modules.isOn("statshud") || cfg.hudStats.enabled) renderStats(ctx, client, cfg.hudStats, false);
        if (Modules.isOn("inventoryhud") || cfg.hudInventory.enabled) renderInventory(ctx, client, cfg.hudInventory, false);
        if (Modules.isOn("cooldowns") || cfg.hudCooldowns.enabled) renderCooldowns(ctx, client, cfg.hudCooldowns, td, false);
    }

    public static int resolveTextColor(BombowyConfig.HudElement el) {
        if (el.chroma) {
            float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
            return 0xFF000000 | MathHelper.hsvToRgb(hue, 0.75f, 1.0f);
        }
        return 0xFF000000 | (el.textColor & 0xFFFFFF);
    }

    public static int resolveBgColor(BombowyConfig.HudElement el) {
        int alpha = MathHelper.clamp((int) (el.bgOpacity * 255.0f), 0, 255);
        return (alpha << 24) | 0x121318;
    }

    public static int[] fpsSize(TextRenderer tr, MinecraftClient client) {
        String txt = "FPS: " + client.getCurrentFps();
        return new int[]{tr.getWidth(txt) + 10, 16};
    }

    public static void renderFps(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElement el, boolean preview) {
        TextRenderer tr = client.textRenderer;
        String txt = "FPS: " + client.getCurrentFps();
        int[] sz = fpsSize(tr, client);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(el.x, el.y, 0);
        ctx.getMatrices().scale(el.scale, el.scale, 1.0f);

        int bg = resolveBgColor(el);
        if ((bg >>> 24) > 0) ctx.fill(0, 0, sz[0], sz[1], bg);
        ctx.drawText(tr, txt, 5, 4, resolveTextColor(el), true);
        ctx.getMatrices().pop();
    }

    public static String statsText(MinecraftClient client, boolean preview) {
        BombowyConfig cfg = BombowyConfig.get();
        List<String> parts = new ArrayList<>();
        if (cfg.showCps) {
            int l = BombowyExtras.getLeftCps();
            int r = BombowyExtras.getRightCps();
            parts.add((preview && l == 0 && r == 0) ? "CPS: 12 / 6" : "CPS: " + l + " / " + r);
        }
        if (cfg.showPing) {
            int ping = BombowyExtras.getPing(client);
            parts.add((preview && ping == 0) ? "Ping: 24ms" : "Ping: " + ping + "ms");
        }
        if (parts.isEmpty()) return "CPS: 0 | Ping: 0ms";
        return String.join("  |  ", parts);
    }

    public static int[] statsSize(TextRenderer tr, MinecraftClient client, boolean preview) {
        String txt = statsText(client, preview);
        return new int[]{tr.getWidth(txt) + 12, 16};
    }

    public static void renderStats(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElement el, boolean preview) {
        TextRenderer tr = client.textRenderer;
        String txt = statsText(client, preview);
        int[] sz = statsSize(tr, client, preview);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(el.x, el.y, 0);
        ctx.getMatrices().scale(el.scale, el.scale, 1.0f);

        int bg = resolveBgColor(el);
        if ((bg >>> 24) > 0) {
            ctx.fill(0, 0, sz[0], sz[1], bg);
            ctx.fill(0, sz[1] - 1, sz[0], sz[1], 0x88FF7A2E);
        }
        ctx.drawText(tr, txt, 6, 4, resolveTextColor(el), true);
        ctx.getMatrices().pop();
    }

    public static int[] inventorySize(BombowyConfig cfg) {
        int slot = cfg.invCompact ? 16 : 18;
        int rows = cfg.invShowHotbar ? 4 : 3;
        return new int[]{9 * slot + 4, rows * slot + 4 + (cfg.invShowHotbar ? 3 : 0)};
    }

    public static void renderInventory(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElement el, boolean preview) {
        BombowyConfig cfg = BombowyConfig.get();
        int slot = cfg.invCompact ? 16 : 18;
        int[] sz = inventorySize(cfg);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(el.x, el.y, 0);
        ctx.getMatrices().scale(el.scale, el.scale, 1.0f);

        int bg = resolveBgColor(el);
        if ((bg >>> 24) > 0) ctx.fill(0, 0, sz[0], sz[1], bg);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = 9 + row * 9 + col;
                int sx = 2 + col * slot;
                int sy = 2 + row * slot;
                ctx.fill(sx, sy, sx + 16, sy + 16, 0x22FFFFFF);

                ItemStack stack = client.player != null ? client.player.getInventory().getStack(idx) : ItemStack.EMPTY;
                if (preview && stack.isEmpty() && row == 0 && col < 3) {
                    stack = col == 0 ? new ItemStack(Items.ENDER_PEARL, 16)
                            : col == 1 ? new ItemStack(Items.GOLDEN_APPLE, 8)
                            : new ItemStack(Items.TOTEM_OF_UNDYING, 1);
                }
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, sx, sy);
                    ctx.drawStackOverlay(client.textRenderer, stack, sx, sy);
                }
            }
        }

        if (cfg.invShowHotbar) {
            int hy = 2 + 3 * slot + 3;
            for (int col = 0; col < 9; col++) {
                int sx = 2 + col * slot;
                ctx.fill(sx, hy, sx + 16, hy + 16, 0x28FFFFFF);
                ItemStack stack = client.player != null ? client.player.getInventory().getStack(col) : ItemStack.EMPTY;
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, sx, hy);
                    ctx.drawStackOverlay(client.textRenderer, stack, sx, hy);
                }
            }
        }

        ctx.getMatrices().pop();
    }

    public static List<CooldownEntry> collectCooldowns(MinecraftClient client, float td, boolean preview) {
        List<CooldownEntry> list = new ArrayList<>();
        BombowyConfig cfg = BombowyConfig.get();

        if (client.player != null) {
            ItemCooldownManager mgr = client.player.getItemCooldownManager();
            List<Item> candidates = new ArrayList<>();

            if (cfg.cdPearl) candidates.add(Items.ENDER_PEARL);
            if (cfg.cdShield) candidates.add(Items.SHIELD);
            if (cfg.cdChorus) candidates.add(Items.CHORUS_FRUIT);
            if (cfg.cdGoldenApple) {
                candidates.add(Items.GOLDEN_APPLE);
                candidates.add(Items.ENCHANTED_GOLDEN_APPLE);
            }
            if (cfg.cdWindCharge) candidates.add(Items.WIND_CHARGE);

            if (cfg.cdOther) {
                for (int i = 0; i < client.player.getInventory().size(); i++) {
                    ItemStack st = client.player.getInventory().getStack(i);
                    if (!st.isEmpty() && !candidates.contains(st.getItem())) {
                        candidates.add(st.getItem());
                    }
                }
            }

            for (Item item : candidates) {
                ItemStack dummy = new ItemStack(item);
                if (mgr.isCoolingDown(dummy)) {
                    float prog = mgr.getCooldownProgress(dummy, td);
                    if (prog > 0.001f) {
                        list.add(new CooldownEntry(dummy, prog, estimateSecondsLeft(item, prog)));
                    }
                }
            }
        }

        if (preview && list.isEmpty()) {
            list.add(new CooldownEntry(new ItemStack(Items.ENDER_PEARL), 0.65f, 0.7f));
            list.add(new CooldownEntry(new ItemStack(Items.SHIELD), 0.48f, 2.4f));
        }
        return list;
    }

    private static float estimateSecondsLeft(Item item, float progress) {
        Long started = cooldownStartTime.get(item);
        if (started != null && progress > 0.01f && progress < 0.99f) {
            float elapsedSec = (System.currentTimeMillis() - started) / 1000.0f;
            float totalEst = elapsedSec / Math.max(0.05f, (1.0f - progress));
            if (totalEst > 0.2f && totalEst < 60.0f) {
                return Math.max(0.1f, totalEst * progress);
            }
        }
        if (item == Items.SHIELD) return 5.0f * progress;
        if (item == Items.ENDER_PEARL) return 1.0f * progress;
        if (item == Items.CHORUS_FRUIT) return 1.0f * progress;
        if (item == Items.WIND_CHARGE) return 0.5f * progress;
        return 3.0f * progress;
    }

    public static int[] cooldownsSize(MinecraftClient client, float td, boolean preview) {
        List<CooldownEntry> entries = collectCooldowns(client, td, preview);
        int rows = Math.max(1, entries.size());
        return new int[]{90, rows * 18 + 4};
    }

    public static void renderCooldowns(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElement el, float td, boolean preview) {
        List<CooldownEntry> entries = collectCooldowns(client, td, preview);
        if (entries.isEmpty()) return;

        TextRenderer tr = client.textRenderer;
        int w = 90;
        int h = entries.size() * 18 + 4;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(el.x, el.y, 0);
        ctx.getMatrices().scale(el.scale, el.scale, 1.0f);

        int bg = resolveBgColor(el);
        if ((bg >>> 24) > 0) ctx.fill(0, 0, w, h, bg);

        int textColor = resolveTextColor(el);
        for (int i = 0; i < entries.size(); i++) {
            CooldownEntry e = entries.get(i);
            int ry = 2 + i * 18;
            ctx.drawItem(e.stack, 2, ry);

            String timeTxt = String.format("%.1fs", Math.max(0.1f, e.secondsLeft));
            ctx.drawText(tr, timeTxt, 22, ry + 4, textColor, true);

            int barX = 52, barW = 34, barY = ry + 6;
            ctx.fill(barX, barY, barX + barW, barY + 4, 0x33FFFFFF);
            int fillW = MathHelper.clamp((int) (barW * (1.0f - e.progress)), 1, barW);
            ctx.fill(barX, barY, barX + fillW, barY + 4, textColor);
        }

        ctx.getMatrices().pop();
    }
}
