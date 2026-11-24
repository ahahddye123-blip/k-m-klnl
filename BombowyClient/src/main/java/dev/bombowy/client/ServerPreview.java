package dev.bombowy.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

public final class ServerPreview {
    private ServerPreview() {}

    private static String lastText = "";
    private static long lastChange = 0;
    private static String requested = "";
    private static volatile ServerPing.Result result = null;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof DirectConnectScreen || screen instanceof AddServerScreen) {
                lastText = "";
                requested = "";
                result = null;
                ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, tickDelta) -> render(s, ctx));
            }
        });
    }

    private static void render(Screen screen, DrawContext ctx) {
        if (!Modules.isOn("quickjoin")) return;

        TextFieldWidget field = null;
        for (Object child : screen.children()) {
            if (child instanceof TextFieldWidget tf) field = tf;
        }
        if (field == null) return;

        String text = field.getText().trim();
        long now = System.currentTimeMillis();
        if (!text.equals(lastText)) {
            lastText = text;
            lastChange = now;
            result = null;
        }
        if (!text.isEmpty() && !text.equals(requested) && now - lastChange > 600) {
            requested = text;
            final String target = text;
            Thread t = new Thread(() -> {
                ServerPing.Result r = ServerPing.ping(target);
                if (target.equals(requested)) result = r;
            }, "BombowyClient-ping");
            t.setDaemon(true);
            t.start();
        }
        if (text.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        ServerPing.Result r = result;

        String line1, line2 = "";
        int color1;
        if (r == null) { line1 = "Sprawdzam serwer..."; color1 = 0xFFB0B3C2; }
        else if (!r.ok) { line1 = "Serwer nie odpowiada"; color1 = 0xFFFF6666; }
        else {
            line1 = "Online: " + r.online + "/" + r.max
                    + (r.ms >= 0 ? "   Ping: " + r.ms + " ms" : "")
                    + (r.version.isEmpty() ? "" : "   " + r.version);
            line2 = r.motd;
            color1 = 0xFFFF9447;
        }

        int w = Math.min(screen.width - 20, 300), h = 30, x = (screen.width - w) / 2, y = screen.height - h - 4;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF3A271E);
        ctx.fill(x, y, x + w, y + h, 0xE6121318);
        ctx.drawText(tr, tr.trimToWidth(line1, w - 12), x + 6, y + 5, color1, false);
        if (!line2.isEmpty()) ctx.drawText(tr, tr.trimToWidth(line2, w - 12), x + 6, y + 17, 0xFFB0B3C2, false);
    }
}
