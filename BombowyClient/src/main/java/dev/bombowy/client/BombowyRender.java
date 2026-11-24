package dev.bombowy.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.Optional;

public final class BombowyRender {
    private BombowyRender() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(BombowyRender::onAfterEntities);
    }

    private static void onAfterEntities(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        boolean hitboxes = Modules.isOn("hitboxes");
        boolean ownName = Modules.isOn("nametags") && !client.options.getPerspective().isFirstPerson();
        if (!hitboxes && !ownName) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        float td = client.getRenderTickCounter().getTickDelta(false);
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        if (hitboxes) {
            BombowyConfig cfg = BombowyConfig.get();
            VertexConsumer lines = immediate.getBuffer(RenderLayer.getLines());
            Vec3d eye = client.player.getCameraPosVec(td);
            Vec3d look = client.player.getRotationVec(td);
            double reach = Math.max(1.0, cfg.hitboxReach);

            Vec3d maxEnd = eye.add(look.multiply(reach));
            BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                    eye, maxEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
            double effectiveReach = reach;
            if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                effectiveReach = blockHit.getPos().distanceTo(eye);
            }

            Entity aimedEntity = null;
            if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof AbstractClientPlayerEntity) {
                if (eye.distanceTo(ehr.getPos()) <= effectiveReach + 0.3) {
                    aimedEntity = ehr.getEntity();
                }
            }
            if (aimedEntity == null) {
                Vec3d reachEnd = eye.add(look.multiply(effectiveReach));
                Box searchBox = client.player.getBoundingBox().stretch(look.multiply(effectiveReach)).expand(1.0);
                EntityHitResult rayHit = ProjectileUtil.raycast(
                        client.player, eye, reachEnd, searchBox,
                        e -> e instanceof AbstractClientPlayerEntity && !e.isSpectator() && e != client.player,
                        effectiveReach * effectiveReach
                );
                if (rayHit != null) aimedEntity = rayHit.getEntity();
            }

            int aimRgb = cfg.hitboxAimColor & 0xFFFFFF;
            float aimR = ((aimRgb >> 16) & 0xFF) / 255.0f;
            float aimG = ((aimRgb >> 8) & 0xFF) / 255.0f;
            float aimB = (aimRgb & 0xFF) / 255.0f;

            int idleRgb = cfg.hitboxIdleColor & 0xFFFFFF;
            float idleR = ((idleRgb >> 16) & 0xFF) / 255.0f;
            float idleG = ((idleRgb >> 8) & 0xFF) / 255.0f;
            float idleB = (idleRgb & 0xFF) / 255.0f;

            matrices.push();
            matrices.translate(-cam.x, -cam.y, -cam.z);
            for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                if (p == client.player || p.isSpectator()) continue;

                double ix = MathHelper.lerp(td, p.prevX, p.getX());
                double iy = MathHelper.lerp(td, p.prevY, p.getY());
                double iz = MathHelper.lerp(td, p.prevZ, p.getZ());
                Box box = p.getBoundingBox().offset(ix - p.getX(), iy - p.getY(), iz - p.getZ());

                Vec3d end = eye.add(look.multiply(effectiveReach));
                Optional<Vec3d> hitPos = box.expand(p.getTargetingMargin()).raycast(eye, end);
                boolean isAiming = (p == aimedEntity) || hitPos.isPresent();

                if (!isAiming && !cfg.hitboxShowAll) continue;

                if (isAiming) bombowyDrawBox(matrices, lines, box, aimR, aimG, aimB, 1.0f);
                else bombowyDrawBox(matrices, lines, box, idleR, idleG, idleB, 0.9f);
            }
            matrices.pop();
            immediate.draw(RenderLayer.getLines());
        }

        if (ownName) {
            String rawName = client.player.getGameProfile().getName();
            Text name = BombowyExtras.formatPlayerText(client.player.getDisplayName(), rawName);
            Vec3d pos = client.player.getLerpedPos(td);
            TextRenderer tr = client.textRenderer;

            matrices.push();
            matrices.translate(pos.x - cam.x, pos.y + client.player.getHeight() + 0.5 - cam.y, pos.z - cam.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(0.025f, -0.025f, 0.025f);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float x = -tr.getWidth(name) / 2.0f;
            tr.draw(name, x, 0.0f, 0xFFFFFFFF, false, matrix, immediate,
                    TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            immediate.draw();
            matrices.pop();
        }
    }

    private static void bombowyDrawBox(MatrixStack matrices, VertexConsumer lines, Box box,
                                         float r, float g, float b, float a) {
        try {
            Class<?> vr = Class.forName("net.minecraft.client.render.VertexRendering");
            for (var m : vr.getMethods()) {
                if (m.getName().equals("drawBox") && m.getParameterCount() == 7) {
                    m.invoke(null, matrices, lines, box, r, g, b, a);
                    return;
                }
            }
        } catch (Throwable ignored) {}
        var entry = matrices.peek();
        double x1 = box.minX, y1 = box.minY, z1 = box.minZ;
        double x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
        double[][] pts = {
            {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
            {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}
        };
        int[][] edges = {
            {0,1},{1,2},{2,3},{3,0},
            {4,5},{5,6},{6,7},{7,4},
            {0,4},{1,5},{2,6},{3,7}
        };
        for (int[] e : edges) {
            double[] p1 = pts[e[0]], p2 = pts[e[1]];
            float dx = (float)(p2[0] - p1[0]), dy = (float)(p2[1] - p1[1]), dz = (float)(p2[2] - p1[2]);
            float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
            float nx = len > 0 ? dx/len : 0f, ny = len > 0 ? dy/len : 1f, nz = len > 0 ? dz/len : 0f;
            lines.vertex(entry, (float)p1[0], (float)p1[1], (float)p1[2]).color(r, g, b, a).normal(entry, nx, ny, nz);
            lines.vertex(entry, (float)p2[0], (float)p2[1], (float)p2[2]).color(r, g, b, a).normal(entry, nx, ny, nz);
        }
    }
}
