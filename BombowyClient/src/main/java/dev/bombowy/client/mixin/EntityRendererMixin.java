package dev.bombowy.client.mixin;

import dev.bombowy.client.BombowyExtras;
import dev.bombowy.client.Modules;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Redirect(
            method = "renderLabelIfPresent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getTextBackgroundOpacity(F)F"),
            require = 0)
    private float bombowy$transparentNametags(GameOptions options, float fallback) {
        return Modules.isOn("nametags") ? 0.0F : options.getTextBackgroundOpacity(fallback);
    }

    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text bombowy$formatPlayerLabel(Text text, Entity entity, Text originalText, MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers, int light, float tickDelta) {
        if (Modules.isOn("nametags") && entity instanceof PlayerEntity pe) {
            return BombowyExtras.formatPlayerText(text, pe.getGameProfile().getName());
        }
        return text;
    }
}
