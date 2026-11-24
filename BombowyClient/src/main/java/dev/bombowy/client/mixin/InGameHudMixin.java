package dev.bombowy.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bombowy.client.BombowyConfig;
import dev.bombowy.client.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void bombowy$noVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        if (Modules.isOn("cleanview") && BombowyConfig.get().noVignette) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void bombowy$colorBoostOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!Modules.isOn("colorboost")) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;

        BombowyConfig cfg = BombowyConfig.get();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();

        int vibranceAlpha = (int) Math.min(36, Math.max(0, (cfg.colorVibrance - 1.0f) * 30.0f));
        if (vibranceAlpha > 0) {
            RenderSystem.enableBlend();
            context.fill(0, 0, w, h, (vibranceAlpha << 24) | 0xFF9E44);
        }

        int tintAlpha = (int) Math.min(90, Math.max(0, cfg.colorTintStrength * 255.0f));
        if (tintAlpha > 0) {
            context.fill(0, 0, w, h, (tintAlpha << 24) | (cfg.colorTintHex & 0xFFFFFF));
        }
    }
}
