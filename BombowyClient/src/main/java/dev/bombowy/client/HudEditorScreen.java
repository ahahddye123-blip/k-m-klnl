package dev.bombowy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private enum Target { FPS, STATS, INVENTORY, COOLDOWNS }

    private Target dragging = null;
    private Target resizing = null;
    private double dragOffsetX, dragOffsetY;
    private double resizeStartX, resizeStartScale;
    private float openAnim = 0.0f;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Edytor HUD"));
        this.parent = parent;
    }

    private BombowyConfig.HudElement getEl(Target t) {
        BombowyConfig cfg = BombowyConfig.get();
        return switch (t) {
            case FPS -> cfg.hudFps;
            case STATS -> cfg.hudStats;
            case INVENTORY -> cfg.hudInventory;
            case COOLDOWNS -> cfg.hudCooldowns;
        };
    }

    private String getModuleId(Target t) {
        return switch (t) {
            case FPS -> "fpshud";
            case STATS -> "statshud";
            case INVENTORY -> "inventoryhud";
            case COOLDOWNS -> "cooldowns";
        };
    }

    private int[] getBox(Target t) {
        BombowyConfig.HudElement el = getEl(t);
        int[] base = switch (t) {
            case FPS -> BombowyHud.fpsSize(this.textRenderer, this.client);
            case STATS -> BombowyHud.statsSize(this.textRenderer, this.client, true);
            case INVENTORY -> BombowyHud.inventorySize(BombowyConfig.get());
            case COOLDOWNS -> BombowyHud.cooldownsSize(this.client, 0f, true);
        };
        int w = Math.max(24, (int) (base[0] * el.scale));
        int h = Math.max(14, (int) (base[1] * el.scale));
        return new int[]{(int) el.x, (int) el.y, w, h};
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        openAnim = MathHelper.clamp(openAnim + delta * 0.18f, 0.0f, 1.0f);
        float ease = 1.0f - (1.0f - openAnim) * (1.0f - openAnim);

        ctx.fill(0, 0, this.width, this.height, ((int) (0x68 * ease) << 24));
        ctx.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x26FF9E44);
        ctx.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x26FF9E44);

        String info = "Edytor HUD — przeciągnij | róg = rozmiar | ⚙ = ustawienia | − = wyłącz";
        int iw = this.textRenderer.getWidth(info) + 22;
        int ix = (this.width - iw) / 2;
        int iy = (int) (-24 + (8 + 24) * ease);
        ctx.fill(ix, iy, ix + iw, iy + 18, 0xE016171D);
        ctx.fill(ix, iy + 17, ix + iw, iy + 18, 0xFFFF7A2E);
        ctx.drawText(this.textRenderer, info, ix + 11, iy + 5, 0xFFF2F3F8, false);

        int bx = (this.width - 120) / 2, by = this.height - 28;
        boolean bHov = mx >= bx && mx <= bx + 120 && my >= by && my <= by + 20;
        ctx.fill(bx, by, bx + 120, by + 20, bHov ? 0xFFFF8C3B : 0xFFE06620);
        String backLbl = "Zapisz i wróć";
        ctx.drawText(this.textRenderer, backLbl, bx + (120 - this.textRenderer.getWidth(backLbl)) / 2, by + 6, 0xFFFFFFFF, false);

        for (Target t : Target.values()) {
            BombowyConfig.HudElement el = getEl(t);
            if (!el.enabled) continue;

            if (t == Target.FPS) BombowyHud.renderFps(ctx, this.client, el, true);
            if (t == Target.STATS) BombowyHud.renderStats(ctx, this.client, el, true);
            if (t == Target.INVENTORY) BombowyHud.renderInventory(ctx, this.client, el, true);
            if (t == Target.COOLDOWNS) BombowyHud.renderCooldowns(ctx, this.client, el, 0f, true);

            int[] b = getBox(t);
            int x = b[0], y = b[1], w = b[2], h = b[3];
            int frameCol = 0xCCFF8A3D;
            ctx.fill(x - 1, y - 1, x + w + 1, y, frameCol);
            ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, frameCol);
            ctx.fill(x - 1, y, x, y + h, frameCol);
            ctx.fill(x + w, y, x + w + 1, y + h, frameCol);

            int barY = Math.max(2, y - 14);
            int gearX = x + w - 26;
            boolean gHov = mx >= gearX && mx < gearX + 12 && my >= barY && my < barY + 12;
            ctx.fill(gearX, barY, gearX + 12, barY + 12, gHov ? 0xFF3A3C4A : 0xDD17181E);
            ctx.drawText(this.textRenderer, "⚙", gearX + 3, barY + 2, 0xFFFFFFFF, false);

            int minX = x + w - 12;
            boolean mHov = mx >= minX && mx < minX + 12 && my >= barY && my < barY + 12;
            ctx.fill(minX, barY, minX + 12, barY + 12, mHov ? 0xFFE04848 : 0xDD17181E);
            ctx.drawText(this.textRenderer, "-", minX + 4, barY + 2, 0xFFFFFFFF, false);

            int rx = x + w - 6, ry = y + h - 6;
            boolean rHov = mx >= rx - 2 && mx <= x + w + 2 && my >= ry - 2 && my <= y + h + 2;
            ctx.fill(rx, ry, x + w + 1, y + h + 1, rHov ? 0xFFFFFFFF : 0xFFFF8A3D);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int bx = (this.width - 120) / 2, by = this.height - 28;
        if (mx >= bx && mx <= bx + 120 && my >= by && my <= by + 20) {
            BombowyConfig.save();
            this.client.setScreen(parent);
            return true;
        }

        for (Target t : List.of(Target.COOLDOWNS, Target.INVENTORY, Target.STATS, Target.FPS)) {
            BombowyConfig.HudElement el = getEl(t);
            if (!el.enabled) continue;

            int[] b = getBox(t);
            int x = b[0], y = b[1], w = b[2], h = b[3];
            int barY = Math.max(2, y - 14);

            int gearX = x + w - 26;
            if (mx >= gearX && mx < gearX + 12 && my >= barY && my < barY + 12) {
                Modules.Module mod = Modules.byId(getModuleId(t));
                if (mod != null) this.client.setScreen(new ModuleSettingsScreen(this, mod));
                return true;
            }

            int minX = x + w - 12;
            if (mx >= minX && mx < minX + 12 && my >= barY && my < barY + 12) {
                Modules.Module mod = Modules.byId(getModuleId(t));
                if (mod != null) mod.setOn(false);
                el.enabled = false;
                BombowyConfig.save();
                return true;
            }

            int rx = x + w - 6, ry = y + h - 6;
            if (mx >= rx - 2 && mx <= x + w + 2 && my >= ry - 2 && my <= y + h + 2) {
                resizing = t;
                resizeStartX = mx;
                resizeStartScale = el.scale;
                return true;
            }

            if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
                dragging = t;
                dragOffsetX = mx - el.x;
                dragOffsetY = my - el.y;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            BombowyConfig.HudElement el = getEl(dragging);
            el.x = MathHelper.clamp((float) (mx - dragOffsetX), 0, Math.max(10, this.width - 20));
            el.y = MathHelper.clamp((float) (my - dragOffsetY), 0, Math.max(10, this.height - 14));
            return true;
        }
        if (resizing != null) {
            BombowyConfig.HudElement el = getEl(resizing);
            el.scale = MathHelper.clamp((float) (resizeStartScale + (mx - resizeStartX) / 80.0), 0.5f, 2.5f);
            return true;
        }
        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null || resizing != null) {
            dragging = null;
            resizing = null;
            BombowyConfig.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT) {
            BombowyConfig.save();
            this.client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
