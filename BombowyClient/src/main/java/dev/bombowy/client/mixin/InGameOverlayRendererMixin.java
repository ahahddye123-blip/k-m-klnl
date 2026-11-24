package dev.bombowy.client.mixin;

import dev.bombowy.client.BombowyConfig;
import dev.bombowy.client.Modules;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), require = 0)
    private static void bombowy$lowFireHead(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Modules.isOn("cleanview") && BombowyConfig.get().lowFire) {
            matrices.push();
            matrices.translate(0.0, -BombowyConfig.get().fireOffset, 0.0);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"), require = 0)
    private static void bombowy$lowFireReturn(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Modules.isOn("cleanview") && BombowyConfig.get().lowFire) {
            matrices.pop();
        }
    }
}
