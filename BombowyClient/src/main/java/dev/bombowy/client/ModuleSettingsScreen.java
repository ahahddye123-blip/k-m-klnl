package dev.bombowy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final Modules.Module module;

    private int px, py, pw, ph;
    private double scroll = 0;
    private float openAnim = 0.0f;

    private static final int[] COLOR_PRESETS = {
            0xFF3B2F, 0xFFFFFF, 0xFF8C3B, 0x3B82F6, 0xFACC15, 0xA855F7, 0x06B6D4, 0x14151B
    };

    private interface SettingItem {
        int height();
        void render(DrawContext ctx, int x, int y, int w, int mx, int my);
        boolean click(int x, int y, int w, double mx, double my);
    }

    public ModuleSettingsScreen(Screen parent, Modules.Module module) {
        super(Text.literal("Ustawienia: " + module.name));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        pw = Math.min(this.width - 24, 440);
        ph = Math.min(this.height - 24, 310);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        openAnim = 0.0f;
    }

    private List<SettingItem> buildItems() {
        List<SettingItem> list = new ArrayList<>();
        BombowyConfig cfg = BombowyConfig.get();

        list.add(toggleItem("Status opcji", module::isOn, val -> module.setOn(val)));

        switch (module.id) {
            case "hitboxes" -> {
                list.add(colorPickerItem("Kolor przy celowaniu", () -> cfg.hitboxAimColor, c -> cfg.hitboxAimColor = c));
                list.add(colorPickerItem("Kolor z boku", () -> cfg.hitboxIdleColor, c -> cfg.hitboxIdleColor = c));
                list.add(sliderItem("Zasięg wykrywania celowania", () -> (cfg.hitboxReach - 2.0f) / 4.0f,
                        v -> cfg.hitboxReach = 2.0f + v * 4.0f, () -> String.format("%.1f kratek", cfg.hitboxReach)));
                list.add(toggleItem("Pokazuj ramki graczy z boku", () -> cfg.hitboxShowAll, v -> cfg.hitboxShowAll = v));
            }
            case "cleanview" -> {
                list.add(toggleItem("Niski ogień na ekranie", () -> cfg.lowFire, v -> cfg.lowFire = v));
                list.add(sliderItem("Obniżenie ognia", () -> cfg.fireOffset / 0.6f,
                        v -> cfg.fireOffset = v * 0.6f, () -> String.format("-%d%%", (int) (cfg.fireOffset * 100))));
                list.add(toggleItem("Bez winiety", () -> cfg.noVignette, v -> cfg.noVignette = v));
                list.add(toggleItem("Bez trzęsienia kamery", () -> cfg.noHurtCam, v -> cfg.noHurtCam = v));
            }
            case "colorboost" -> {
                list.add(sliderItem("Nasycenie (Vibrance)", () -> (cfg.colorVibrance - 1.0f) / 1.0f,
                        v -> cfg.colorVibrance = 1.0f + v * 1.0f, () -> String.format("%d%%", (int) (cfg.colorVibrance * 100))));
                list.add(sliderItem("Kontrast", () -> (cfg.colorContrast - 1.0f) / 0.5f,
                        v -> cfg.colorContrast = 1.0f + v * 0.5f, () -> String.format("%d%%", (int) (cfg.colorContrast * 100))));
                list.add(sliderItem("Siła odcienia", () -> cfg.colorTintStrength / 0.35f,
                        v -> cfg.colorTintStrength = v * 0.35f, () -> String.format("%d%%", (int) (cfg.colorTintStrength * 100))));
                list.add(colorPickerItem("Kolor odcienia", () -> cfg.colorTintHex, c -> cfg.colorTintHex = c));
            }
            case "optimization" -> {
                list.add(toggleItem("Tryb słabego PC (Potato)", () -> cfg.potatoMode, v -> {
                    cfg.potatoMode = v;
                    if (module.isOn()) Features.optimization(true);
                }));
                list.add(toggleItem("Ukryj deszcz i śnieg", () -> cfg.disableWeather, v -> cfg.disableWeather = v));
                list.add(toggleItem("Ogranicz zbędne animacje", () -> cfg.reduceAnimations, v -> cfg.reduceAnimations = v));
            }
            case "fpshud" -> addHudSettings(list, cfg.hudFps);
            case "statshud" -> {
                addHudSettings(list, cfg.hudStats);
                list.add(toggleItem("Pokazuj CPS (kliknięcia/s)", () -> cfg.showCps, v -> cfg.showCps = v));
                list.add(toggleItem("Pokazuj Ping (ms)", () -> cfg.showPing, v -> cfg.showPing = v));
            }
            case "inventoryhud" -> {
                addHudSettings(list, cfg.hudInventory);
                list.add(toggleItem("Pokazuj pasek podręczny (Hotbar)", () -> cfg.invShowHotbar, v -> cfg.invShowHotbar = v));
                list.add(toggleItem("Kompaktowe sloty", () -> cfg.invCompact, v -> cfg.invCompact = v));
            }
            case "cooldowns" -> {
                addHudSettings(list, cfg.hudCooldowns);
                list.add(toggleItem("Perła Endu", () -> cfg.cdPearl, v -> cfg.cdPearl = v));
                list.add(toggleItem("Tarcza", () -> cfg.cdShield, v -> cfg.cdShield = v));
                list.add(toggleItem("Owoc refrenusu", () -> cfg.cdChorus, v -> cfg.cdChorus = v));
                list.add(toggleItem("Złote jabłko", () -> cfg.cdGoldenApple, v -> cfg.cdGoldenApple = v));
                list.add(toggleItem("Wietrzna kula", () -> cfg.cdWindCharge, v -> cfg.cdWindCharge = v));
                list.add(toggleItem("Pozostałe przedmioty", () -> cfg.cdOther, v -> cfg.cdOther = v));
            }
            case "nametags" -> {
                list.add(toggleItem("Tag (💣) na czarno + biały nick", () -> cfg.bombTagEnabled, v -> cfg.bombTagEnabled = v));
                list.add(toggleItem("Pokazuj (💣) przy wszystkich graczach", () -> cfg.bombTagAllPlayers, v -> cfg.bombTagAllPlayers = v));
            }
        }
        return list;
    }

    private void addHudSettings(List<SettingItem> list, BombowyConfig.HudElement el) {
        list.add(sliderItem("Przezroczystość tła", () -> el.bgOpacity, v -> el.bgOpacity = v,
                () -> String.format("%d%%", (int) (el.bgOpacity * 100))));
        list.add(sliderItem("Rozmiar elementu", () -> (el.scale - 0.5f) / 2.0f, v -> el.scale = 0.5f + v * 2.0f,
                () -> String.format("%.2fx", el.scale)));
        list.add(toggleItem("Kolor tęczowy (Chroma)", () -> el.chroma, v -> el.chroma = v));
        list.add(colorPickerItem("Kolor tekstu", () -> el.textColor, c -> { el.textColor = c; el.chroma = false; }));
    }

    private SettingItem toggleItem(String label, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
        return new SettingItem() {
            @Override public int height() { return 28; }
            @Override public void render(DrawContext ctx, int x, int y, int w, int mx, int my) {
                boolean on = getter.getAsBoolean();
                ctx.fill(x, y, x + w, y + 24, 0xFF191B23);
                ctx.drawText(textRenderer, label, x + 8, y + 8, 0xFFEAEBF2, false);
                int bw = 66, bh = 16, bx = x + w - bw - 6, by = y + 4;
                boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
                ctx.fill(bx, by, bx + bw, by + bh, on ? (hov ? 0xFFFF8C3B : 0xFFE26A22) : (hov ? 0xFF3A3E4E : 0xFF2B2E3A));
                String t = on ? "Włączony" : "Wyłączony";
                ctx.drawText(textRenderer, t, bx + (bw - textRenderer.getWidth(t)) / 2, by + 4, 0xFFFFFFFF, false);
            }
            @Override public boolean click(int x, int y, int w, double mx, double my) {
                if (mx >= x && mx <= x + w && my >= y && my <= y + 24) {
                    setter.accept(!getter.getAsBoolean());
                    BombowyConfig.save();
                    return true;
                }
                return false;
            }
        };
    }

    private SettingItem sliderItem(String label, java.util.function.Supplier<Float> getter,
                                   java.util.function.Consumer<Float> setter, java.util.function.Supplier<String> valText) {
        return new SettingItem() {
            @Override public int height() { return 36; }
            @Override public void render(DrawContext ctx, int x, int y, int w, int mx, int my) {
                float p = MathHelper.clamp(getter.get(), 0f, 1f);
                ctx.fill(x, y, x + w, y + 32, 0xFF191B23);
                ctx.drawText(textRenderer, label, x + 8, y + 5, 0xFFEAEBF2, false);
                String vt = valText.get();
                ctx.drawText(textRenderer, vt, x + w - 8 - textRenderer.getWidth(vt), y + 5, 0xFFFF9447, false);
                int sx = x + 8, sy = y + 20, sw = w - 16, sh = 5;
                ctx.fill(sx, sy, sx + sw, sy + sh, 0xFF2B2E3A);
                int fw = (int) (sw * p);
                ctx.fill(sx, sy, sx + fw, sy + sh, 0xFFFF7A2E);
                ctx.fill(sx + fw - 3, sy - 2, sx + fw + 3, sy + sh + 2, 0xFFFFFFFF);
            }
            @Override public boolean click(int x, int y, int w, double mx, double my) {
                int sx = x + 8, sy = y + 14, sw = w - 16, sh = 16;
                if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
                    setter.accept(MathHelper.clamp((float) ((mx - sx) / sw), 0f, 1f));
                    BombowyConfig.save();
                    return true;
                }
                return false;
            }
        };
    }

    private SettingItem colorPickerItem(String label, java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new SettingItem() {
            @Override public int height() { return 42; }
            @Override public void render(DrawContext ctx, int x, int y, int w, int mx, int my) {
                int cur = getter.getAsInt() & 0xFFFFFF;
                ctx.fill(x, y, x + w, y + 38, 0xFF191B23);
                ctx.drawText(textRenderer, label, x + 8, y + 5, 0xFFEAEBF2, false);
                for (int i = 0; i < COLOR_PRESETS.length; i++) {
                    int cx = x + 8 + i * 24, cy = y + 18, col = COLOR_PRESETS[i];
                    if (col == cur) ctx.fill(cx - 2, cy - 2, cx + 18, cy + 16, 0xFFFF8C3B);
                    ctx.fill(cx, cy, cx + 16, cy + 14, 0xFF000000 | col);
                }
            }
            @Override public boolean click(int x, int y, int w, double mx, double my) {
                for (int i = 0; i < COLOR_PRESETS.length; i++) {
                    int cx = x + 8 + i * 24, cy = y + 18;
                    if (mx >= cx && mx <= cx + 16 && my >= cy && my <= cy + 14) {
                        setter.accept(COLOR_PRESETS[i]);
                        BombowyConfig.save();
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        openAnim = MathHelper.clamp(openAnim + delta * 0.18f, 0.0f, 1.0f);
        float ease = 1.0f - (1.0f - openAnim) * (1.0f - openAnim);
        int slideY = (int) ((1.0f - ease) * 12);
        int curY0 = py + slideY;

        ctx.fill(0, 0, this.width, this.height, 0xB8090A0E);
        ctx.fill(px - 1, curY0 - 1, px + pw + 1, curY0 + ph + 1, 0xFF3A271E);
        ctx.fill(px, curY0, px + pw, curY0 + ph, 0xFF121318);

        ctx.drawText(this.textRenderer, Text.literal("⚙ " + module.name).styled(s -> s.withBold(true)),
                px + 12, curY0 + 11, 0xFFFF9447, false);
        ctx.drawText(this.textRenderer, module.desc, px + 12, curY0 + 24, 0xFF8C90A0, false);

        int bx = px + pw - 68, by = curY0 + 10;
        boolean bHov = mx >= bx && mx <= bx + 56 && my >= by && my <= by + 18;
        ctx.fill(bx, by, bx + 56, by + 18, bHov ? 0xFFFF8C3B : 0xFF262934);
        ctx.drawText(this.textRenderer, "Powrót", bx + 11, by + 5, 0xFFFFFFFF, false);

        List<SettingItem> items = buildItems();
        int listX = px + 12, listY = curY0 + 42, listW = pw - 24, listH = ph - 52;
        ctx.enableScissor(listX, listY, listX + listW, listY + listH);
        int curY = listY - (int) scroll;
        for (SettingItem item : items) {
            item.render(ctx, listX, curY, listW, mx, my);
            curY += item.height();
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int slideY = (int) ((1.0f - (1.0f - (1.0f - openAnim) * (1.0f - openAnim))) * 12);
        int curY0 = py + slideY;

        int bx = px + pw - 68, by = curY0 + 10;
        if (mx >= bx && mx <= bx + 56 && my >= by && my <= by + 18) {
            this.client.setScreen(parent);
            return true;
        }

        List<SettingItem> items = buildItems();
        int listX = px + 12, listY = curY0 + 42, listW = pw - 24, curY = listY - (int) scroll;
        for (SettingItem item : items) {
            if (item.click(listX, curY, listW, mx, my)) return true;
            curY += item.height();
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * 16, 0, 240);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT) {
            this.client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
