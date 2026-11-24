package dev.bombowy.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.lwjgl.glfw.GLFW;

public class BombowyClient implements ClientModInitializer {
    public static KeyBinding openKey;
    private static boolean ownsNightVision = false;

    @Override
    public void onInitializeClient() {
        BombowyConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bombowyclient.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.categories.bombowyclient"));

        BombowyRender.register();
        BombowyHud.register();
        ServerPreview.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MenuScreen());
                }
            }
            tickBrightness(client);
            BombowyExtras.tick(client);
            BombowyHud.tick(client);
        });
    }

    private static void tickBrightness(MinecraftClient client) {
        if (client.player == null) {
            ownsNightVision = false;
            return;
        }
        if (Modules.isOn("brightness")) {
            StatusEffectInstance cur = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || !cur.isInfinite()) {
                client.player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
            }
            ownsNightVision = true;
        } else if (ownsNightVision) {
            client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            ownsNightVision = false;
        }
    }
}
