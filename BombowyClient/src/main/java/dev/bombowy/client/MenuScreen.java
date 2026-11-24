package dev.bombowy.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuScreen extends Screen {
    private static final Identifier BOMB_LOGO = Identifier.of("bombowyclient", "textures/bomb_logo.png");

    private static final int SIDE = 132, HEAD = 48, PAD = 10, GAP = 8, CARD_H = 90, MIN_CARD = 122;
    private static int lastTab = 0;
    private static boolean onlyFavorites = false;

    private TextFieldWidget search;
    private double scroll = 0;
    private int px, py, pw, ph;
    private float openAnim = 0.0f;
    private float tabBarY = -1.0f;
    private final Map<String, Long> clickAnim = new HashMap<>();

    public MenuScreen() {
        super(Text.literal("BombowyClient"));
    }

    @Override
    protected void init() {
        pw = Math.min(this.width - 14, 680);
        ph = Math.min(this.height - 14, 380);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        openAnim = 0.0f;
        tabBarY = -1.0f;

        int sw = Math.max(85, Math.min(154, pw - SIDE - PAD * 2 - 160));
        search = new TextFieldWidget(this.textRenderer, px + pw - PAD - sw - 24, py + 14, sw, 18, Text.literal("Szukaj"));
        search.setPlaceholder(Text.literal("Szukaj opcji..."));
        search.setChangedListener(s -> scroll = 0);
        this.addSelectableChild(search);
    }

    private int gridX() { return px + SIDE + PAD; }
    private int gridY() { return py + HEAD; }
    private int gridW() { return pw - SIDE - PAD * 2; }
    private int gridH() { return ph - HEAD - PAD; }
    private int cols() { return Math.max(2, Math.min(4, (gridW() + GAP) / (MIN_CARD + GAP))); }
    private int cardW() { int c = cols(); return (gridW() - GAP * (c - 1)) / c; }
    private int cardX(int i) { return gridX() + (i % cols()) * (cardW() + GAP); }
    private int cardY(int i) { return gridY() + (i / cols()) * (CARD_H + GAP) - (int) scroll; }

    private int maxScroll(int count) {
        int rows = (count + cols() - 1) / cols();
        return Math.max(0, rows * (CARD_H + GAP) - GAP - gridH());
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Modules.Module> visible() {
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        Modules.Tab currentTab = Modules.Tab.values()[lastTab];
        List<Modules.Module> out = new ArrayList<>();

        for (Modules.Module m : Modules.ALL) {
            if (onlyFavorites && !BombowyConfig.get().favorites.contains(m.id)) continue;
            boolean match;
            if (!q.isEmpty()) match = m.name.toLowerCase().contains(q) || m.desc.toLowerCase().contains(q);
            else if (currentTab == Modules.Tab.ALL) match = true;
            else if (currentTab == Modules.Tab.ENABLED) match = m.isOn();
            else match = m.tab == currentTab;
            if (match) out.add(m);
        }
        return out;
    }

    private static int countIn(Modules.Tab tab) {
        if (tab == Modules.Tab.ALL) return Modules.ALL.size();
        int n = 0;
        for (Modules.Module m : Modules.ALL) {
            if (tab == Modules.Tab.ENABLED) { if (m.isOn()) n++; }
            else if (m.tab == tab) n++;
        }
        return n;
    }

    private static void rrect(DrawContext c, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) { c.fill(x, y, x + w, y + h, color); return; }
        c.fill(x + 1, y, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private void playClickFeedback(String moduleId) {
        if (moduleId != null) clickAnim.put(moduleId, System.currentTimeMillis());
        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 1.2f);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        TextRenderer tr = this.textRenderer;
        long now = System.currentTimeMillis();

        openAnim = MathHelper.clamp(openAnim + delta * 0.16f, 0.0f, 1.0f);
        float ease = 1.0f - (1.0f - openAnim) * (1.0f - openAnim);
        int slideY = (int) ((1.0f - ease) * 14);
        int curY0 = py + slideY;

        ctx.fill(0, 0, this.width, this.height, 0xB8090A0E);
        rrect(ctx, px - 1, curY0 - 1, pw + 2, ph + 2, 0xFF3A271E);
        rrect(ctx, px, curY0, pw, ph, 0xFF121318);

        ctx.fill(px + 1, curY0 + 1, px + SIDE, curY0 + ph - 1, 0xFF16181F);
        ctx.fill(px + SIDE, curY0 + 1, px + SIDE + 1, curY0 + ph - 1, 0xFF252834);

        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                BOMB_LOGO, px + 10, curY0 + 10, 0f, 0f, 20, 20, 20, 20);
        ctx.drawText(tr, Text.literal("BOMBOWY").styled(s -> s.withBold(true)), px + 35, curY0 + 11, 0xFFFF8C3B, false);
        ctx.drawText(tr, "Client 1.21.4", px + 35, curY0 + 22, 0xFF8C90A0, false);

        Modules.Tab[] tabs = Modules.Tab.values();
        boolean searching = search != null && !search.getText().trim().isEmpty();

        int targetTabY = curY0 + 44 + lastTab * 24;
        if (tabBarY < 0) tabBarY = targetTabY;
        tabBarY = MathHelper.lerp(MathHelper.clamp(delta * 0.28f, 0.05f, 1.0f), tabBarY, targetTabY);

        if (!searching) ctx.fill(px + 7, (int) tabBarY + 3, px + 10, (int) tabBarY + 18, 0xFFFF7A2E);

        for (int i = 0; i < tabs.length; i++) {
            int tx = px + 7, ty = curY0 + 44 + i * 24, tw = SIDE - 14, th = 21;
            boolean sel = !searching && i == lastTab;
            boolean hov = inside(mx, my, tx, ty, tw, th);
            if (sel) rrect(ctx, tx, ty, tw, th, 0xFF232631);
            else if (hov) rrect(ctx, tx, ty, tw, th, 0xFF1C1E27);

            drawTabIcon(ctx, tabs[i], tx + 8, ty + 6, sel ? 0xFFFF8C3B : 0xFF9EA3B5);
            ctx.drawText(tr, tabs[i].label, tx + 21, ty + 6, sel ? 0xFFFFFFFF : 0xFFA5AAB8, false);
            String cnt = String.valueOf(countIn(tabs[i]));
            ctx.drawText(tr, cnt, tx + tw - 6 - tr.getWidth(cnt), ty + 6, 0xFF6B6F80, false);
        }

        int hudBtnX = px + 7, hudBtnY = curY0 + ph - 44, hudBtnW = SIDE - 14, hudBtnH = 22;
        boolean hudHov = inside(mx, my, hudBtnX, hudBtnY, hudBtnW, hudBtnH);
        rrect(ctx, hudBtnX, hudBtnY, hudBtnW, hudBtnH, hudHov ? 0xFF332721 : 0xFF231D1B);
        ctx.fill(hudBtnX, hudBtnY + hudBtnH - 1, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 0xFFFF7A2E);
        ctx.drawText(tr, Text.literal("Edytor HUD").styled(s -> s.withBold(true)), hudBtnX + 12, hudBtnY + 7, 0xFFFFB27A, false);
        ctx.drawText(tr, "v2.3 • klawisz `", px + 11, curY0 + ph - 14, 0xFF616575, false);

        int hx = px + SIDE + PAD;
        ctx.drawText(tr, Text.literal("Moduły i opcje").styled(s -> s.withBold(true)), hx, curY0 + 11, 0xFFFFFFFF, false);
        ctx.drawText(tr, searching ? "Wyniki wyszukiwania" : "Konfiguracja BombowyClient", hx, curY0 + 24, 0xFF8B8FA0, false);

        if (search != null) {
            search.setY(curY0 + 14);
            search.render(ctx, mx, my, delta);
        }

        int favBtnX = px + pw - PAD - 19, favBtnY = curY0 + 14;
        boolean favHov = inside(mx, my, favBtnX, favBtnY, 18, 18);
        rrect(ctx, favBtnX, favBtnY, 18, 18, onlyFavorites ? 0xFFFF7A2E : (favHov ? 0xFF2A2D3A : 0xFF1F212B));
        ctx.drawText(tr, "♥", favBtnX + 5, favBtnY + 5, 0xFFFFFFFF, false);

        List<Modules.Module> list = visible();
        int gx = gridX(), gy = curY0 + HEAD, gw = gridW(), gh = gridH();
        scroll = MathHelper.clamp(scroll, 0, maxScroll(list.size()));
        int cw = cardW();

        ctx.enableScissor(gx, gy, gx + gw, gy + gh);
        if (list.isEmpty()) ctx.drawText(tr, "Brak pasujących opcji", gx + 6, gy + 8, 0xFF8A8E9E, false);

        for (int i = 0; i < list.size(); i++) {
            Modules.Module m = list.get(i);
            int cx = cardX(i), cy = cardY(i) + slideY;
            if (cy + CARD_H < gy || cy > gy + gh) continue;

            boolean inGrid = inside(mx, my, gx, gy, gw, gh);
            boolean hov = inGrid && inside(mx, my, cx, cy, cw, CARD_H);
            boolean on = m.isOn();
            boolean isFav = BombowyConfig.get().favorites.contains(m.id);

            long clickTime = clickAnim.getOrDefault(m.id, 0L);
            float pulse = MathHelper.clamp(1.0f - (now - clickTime) / 260.0f, 0.0f, 1.0f);
            int pressOffset = pulse > 0.55f ? 1 : 0;

            int borderCol = pulse > 0.05f ? 0xFFFF9E44 : (on ? 0xFF8F471D : (hov ? 0xFF3A3E4F : 0xFF252834));
            rrect(ctx, cx - 1, cy - 1 + pressOffset, cw + 2, CARD_H + 2, borderCol);
            rrect(ctx, cx, cy + pressOffset, cw, CARD_H, hov ? 0xFF20232D : 0xFF181A22);

            String title = tr.trimToWidth(m.name, Math.max(24, cw - 28));
            ctx.drawText(tr, Text.literal(title).styled(s -> s.withBold(true)), cx + 8, cy + 7 + pressOffset, 0xFFF4F5FA, false);
            ctx.drawText(tr, "♥", cx + cw - 15, cy + 7 + pressOffset, isFav ? 0xFFFF5E7E : 0xFF6A6E7E, false);
            drawCardBadgeIcon(ctx, m.icon, cx + cw / 2, cy + 40 + pressOffset, on);

            int gearX = cx + 7, gearY = cy + CARD_H - 22 + pressOffset, gearSize = 16;
            boolean gHov = inGrid && inside(mx, my, gearX, gearY, gearSize, gearSize);
            rrect(ctx, gearX, gearY, gearSize, gearSize, gHov ? 0xFF343848 : 0xFF232631);
            ctx.drawText(tr, "⚙", gearX + 4, gearY + 4, m.hasSettings ? 0xFFFFA866 : 0xFF666A7A, false);

            int btnX = gearX + gearSize + 5, btnY = gearY, btnW = cw - 14 - gearSize - 5, btnH = 16;
            boolean bHov = inGrid && inside(mx, my, btnX, btnY, btnW, btnH);
            int btnBg = on ? (bHov ? 0xFFFF8C3B : 0xFFE26A22) : (bHov ? 0xFF353846 : 0xFF282B36);
            rrect(ctx, btnX, btnY, btnW, btnH, btnBg);

            String label = on ? "Włączony" : "Wyłączony";
            ctx.drawText(tr, label, btnX + (btnW - tr.getWidth(label)) / 2, btnY + 4, 0xFFFFFFFF, false);
        }
        ctx.disableScissor();

        int max = maxScroll(list.size());
        if (max > 0) {
            int barH = Math.max(20, gh * gh / (gh + max));
            int barY = gy + (int) ((gh - barH) * (scroll / max));
            ctx.fill(gx + gw + 3, barY, gx + gw + 5, barY + barH, 0xFFFF7A2E);
        }
    }

    private void drawTabIcon(DrawContext ctx, Modules.Tab tab, int x, int y, int col) {
        switch (tab) {
            case ALL -> {
                ctx.fill(x, y, x + 3, y + 3, col); ctx.fill(x + 5, y, x + 8, y + 3, col);
                ctx.fill(x, y + 5, x + 3, y + 8, col); ctx.fill(x + 5, y + 5, x + 8, y + 8, col);
            }
            case ENABLED -> {
                ctx.fill(x + 1, y + 4, x + 3, y + 7, col); ctx.fill(x + 3, y + 6, x + 5, y + 8, col);
                ctx.fill(x + 5, y + 2, x + 8, y + 6, col);
            }
            case WALKA -> { ctx.fill(x + 1, y + 1, x + 4, y + 4, col); ctx.fill(x + 4, y + 4, x + 8, y + 8, col); }
            case WYGLAD -> { ctx.fill(x + 4, y, x + 5, y + 9, col); ctx.fill(x, y + 4, x + 9, y + 5, col); }
            case HUD -> { rrect(ctx, x, y + 1, 9, 7, col); rrect(ctx, x + 1, y + 2, 7, 5, 0xFF16181F); }
            case NARZEDZIA -> rrect(ctx, x + 1, y + 1, 7, 7, col);
        }
    }

    private void drawCardBadgeIcon(DrawContext ctx, Modules.Icon icon, int cx, int cy, boolean active) {
        int badgeBg = active ? 0xFF2B1F19 : 0xFF222530;
        int accent = active ? 0xFFFF9447 : 0xFFCBD0E0;
        rrect(ctx, cx - 12, cy - 10, 24, 20, badgeBg);
        ctx.fill(cx - 12, cy + 9, cx + 12, cy + 10, active ? 0xFFFF7A2E : 0xFF323646);

        switch (icon) {
            case HITBOX -> {
                rrect(ctx, cx - 6, cy - 5, 12, 10, accent); rrect(ctx, cx - 4, cy - 3, 8, 6, badgeBg);
                ctx.fill(cx - 1, cy - 6, cx, cy + 6, accent); ctx.fill(cx - 7, cy - 1, cx + 7, cy, accent);
            }
            case AUTOSPRINT -> {
                ctx.fill(cx - 6, cy - 4, cx - 2, cy - 1, accent); ctx.fill(cx - 2, cy - 2, cx + 2, cy + 1, accent);
                ctx.fill(cx - 2, cy + 1, cx + 2, cy + 4, accent); ctx.fill(cx - 6, cy + 3, cx - 2, cy + 6, accent);
                ctx.fill(cx + 2, cy - 3, cx + 6, cy + 4, 0xFFFFD166);
            }
            case STATS_HUD -> {
                ctx.fill(cx - 7, cy + 2, cx - 4, cy + 5, accent); ctx.fill(cx - 2, cy - 2, cx + 1, cy + 5, accent);
                ctx.fill(cx + 3, cy - 5, cx + 6, cy + 5, 0xFFFFD166);
            }
            case NAMETAGS -> {
                rrect(ctx, cx - 8, cy - 4, 16, 8, accent); ctx.fill(cx - 6, cy - 2, cx - 2, cy + 2, 0xFF111216);
                ctx.fill(cx, cy - 1, cx + 6, cy + 1, badgeBg);
            }
            case OPTIMIZATION -> {
                ctx.fill(cx + 1, cy - 6, cx + 3, cy - 2, accent); ctx.fill(cx - 2, cy - 3, cx + 2, cy, accent);
                ctx.fill(cx - 3, cy, cx + 2, cy + 3, 0xFFFFD166); ctx.fill(cx - 2, cy + 3, cx, cy + 6, 0xFFFFD166);
            }
            case CLEAN_VIEW -> { rrect(ctx, cx - 7, cy - 4, 14, 8, accent); rrect(ctx, cx - 2, cy - 2, 4, 4, badgeBg); }
            case NO_PARTICLES -> {
                ctx.fill(cx - 4, cy - 6, cx - 3, cy + 1, accent); ctx.fill(cx - 7, cy - 3, cx, cy - 2, accent);
                ctx.fill(cx + 3, cy, cx + 4, cy + 6, 0xFFFFD166); ctx.fill(cx + 1, cy + 3, cx + 6, cy + 4, 0xFFFFD166);
            }
            case COLOR_BOOST -> {
                ctx.fill(cx - 6, cy - 4, cx - 1, cy + 4, 0xFFFF6B6B); ctx.fill(cx - 1, cy - 4, cx + 3, cy + 4, 0xFFFFD166);
                ctx.fill(cx + 3, cy - 4, cx + 7, cy + 4, 0xFF4ECDC4);
            }
            case INVENTORY -> {
                for (int r = 0; r < 2; r++) for (int c = 0; c < 3; c++) rrect(ctx, cx - 7 + c * 5, cy - 4 + r * 5, 4, 4, accent);
            }
            case COOLDOWNS -> {
                rrect(ctx, cx - 6, cy - 5, 12, 10, accent); rrect(ctx, cx - 4, cy - 3, 8, 6, badgeBg);
                ctx.fill(cx, cy - 3, cx + 1, cy + 1, 0xFFFFD166); ctx.fill(cx, cy, cx + 3, cy + 1, 0xFFFFD166);
            }
            case FPS_HUD -> {
                rrect(ctx, cx - 7, cy - 5, 14, 8, accent); rrect(ctx, cx - 5, cy - 3, 10, 4, badgeBg);
                ctx.fill(cx - 2, cy + 4, cx + 2, cy + 6, accent);
            }
            case QUALITY -> {
                ctx.fill(cx - 6, cy - 4, cx + 6, cy - 2, accent); ctx.fill(cx - 4, cy - 1, cx + 4, cy + 1, 0xFFFFD166);
                ctx.fill(cx - 6, cy + 2, cx + 6, cy + 4, accent);
            }
            case BRIGHTNESS -> {
                rrect(ctx, cx - 3, cy - 3, 6, 6, 0xFFFFD166);
                ctx.fill(cx - 1, cy - 6, cx, cy - 4, accent); ctx.fill(cx - 1, cy + 4, cx, cy + 6, accent);
                ctx.fill(cx - 6, cy - 1, cx - 4, cy, accent); ctx.fill(cx + 4, cy - 1, cx + 6, cy, accent);
            }
            case UNLIMITED_FPS, ENTITY_DIST, CHUNK_CAP, QUICK_JOIN -> {
                ctx.fill(cx - 5, cy - 4, cx - 2, cy, accent); ctx.fill(cx - 5, cy, cx - 2, cy + 4, accent);
                ctx.fill(cx + 1, cy - 4, cx + 4, cy, 0xFFFFD166); ctx.fill(cx + 1, cy, cx + 4, cy + 4, 0xFFFFD166);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int slideY = (int) ((1.0f - (1.0f - (1.0f - openAnim) * (1.0f - openAnim))) * 14);
            int curY0 = py + slideY;

            Modules.Tab[] tabs = Modules.Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                if (inside(mx, my, px + 7, curY0 + 44 + i * 24, SIDE - 14, 21)) {
                    lastTab = i; scroll = 0;
                    if (search != null) search.setText("");
                    playClickFeedback(null);
                    return true;
                }
            }

            if (inside(mx, my, px + 7, curY0 + ph - 44, SIDE - 14, 22)) {
                playClickFeedback(null);
                this.client.setScreen(new HudEditorScreen(this));
                return true;
            }

            if (inside(mx, my, px + pw - PAD - 19, curY0 + 14, 18, 18)) {
                onlyFavorites = !onlyFavorites; scroll = 0;
                playClickFeedback(null);
                return true;
            }

            if (inside(mx, my, gridX(), curY0 + HEAD, gridW(), gridH())) {
                List<Modules.Module> list = visible();
                int cw = cardW();
                for (int i = 0; i < list.size(); i++) {
                    Modules.Module m = list.get(i);
                    int cx = cardX(i), cy = cardY(i) + slideY;

                    if (inside(mx, my, cx + cw - 17, cy + 5, 14, 14)) {
                        if (BombowyConfig.get().favorites.contains(m.id)) BombowyConfig.get().favorites.remove(m.id);
                        else BombowyConfig.get().favorites.add(m.id);
                        BombowyConfig.save();
                        playClickFeedback(m.id);
                        return true;
                    }

                    int gearX = cx + 7, gearY = cy + CARD_H - 22;
                    if (inside(mx, my, gearX, gearY, 16, 16)) {
                        playClickFeedback(m.id);
                        this.client.setScreen(new ModuleSettingsScreen(this, m));
                        return true;
                    }

                    if (inside(mx, my, gearX + 21, gearY, cw - 35, 16)) {
                        m.toggle();
                        playClickFeedback(m.id);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * 20, 0, maxScroll(visible().size()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT && (search == null || !search.isFocused())) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
