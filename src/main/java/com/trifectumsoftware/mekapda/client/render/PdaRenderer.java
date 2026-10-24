package com.trifectumsoftware.mekapda.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.trifectumsoftware.mekapda.MekaPDA;
import com.trifectumsoftware.mekapda.client.PdaClient;
import com.trifectumsoftware.mekapda.client.ui.PdaSurface;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = MekaPDA.MODID, value = Dist.CLIENT)
public final class PdaRenderer {

    private static final float HALF_W = 0.155f;
    private static final float HALF_H = 0.1008f;
    private static final float PDA_SCALE = 1.15f;

    private static final float RAISED_Z = 0.29f;

    private static final Vector3f REST = new Vector3f(0.0f, -0.31f, -0.42f);
    private static final float REST_TILT = -50.0f;
    private static final Vector3f RAISED = new Vector3f(0.0f, -0.03f, -0.29f);
    private static final float RAISED_TILT = -4.0f;

    private static final Vector3f[] GRIP_LOCAL = {
          new Vector3f(0.145f, -0.086f, -0.012f),
          new Vector3f(-0.145f, -0.086f, -0.012f)
    };
    private static final Vector3f WRIST = new Vector3f();
    private static final Vector3f SHOULDER = new Vector3f();
    private static final Vector3f ELBOW = new Vector3f();
    private static final Matrix4f UPPER = new Matrix4f();
    private static final Matrix4f LOWER = new Matrix4f();

    private PdaRenderer() {
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        if (!PdaClient.held()) {
            return;
        }

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        int texture = PdaClient.surface().texture();
        if (texture == 0) {
            return;
        }

        float t = PdaClient.raise(event.getPartialTick());
        t = t * t * (3 - 2 * t);
        Vector3f at = new Vector3f(REST).lerp(RAISED, t);
        float tilt = REST_TILT + (RAISED_TILT - REST_TILT) * t;

        PoseStack pose = event.getPoseStack();

        Matrix4f view = new Matrix4f(pose.last().pose());
        Matrix4f local = new Matrix4f()
              .translation(at)
              .rotateX((float) Math.toRadians(tilt))
              .scale(PDA_SCALE);

        pose.pushPose();
        pose.last().pose().mul(local);

        PdaClient.surface().pixelScale(measurePixelScale());

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        screenQuad(pose);
        drawVoronoi(pose);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        pose.popPose();

        renderArms(event, view, local);
    }

    private static void renderArms(RenderHandEvent event, Matrix4f view, Matrix4f local) {
        PdaArms arms = PdaArms.get();
        LocalPlayer player = Minecraft.getInstance().player;
        if (arms == null || player == null) {
            return;
        }
        for (int side = 0; side < 2; side++) {
            local.transformPosition(GRIP_LOCAL[side], WRIST);
            ArmViewPose.shoulder(side == 1, SHOULDER);
            ArmViewPose.solve(WRIST, SHOULDER, side == 0 ? ArmViewPose.RIGHT_POLE : ArmViewPose.LEFT_POLE,
                  arms.lengths, UPPER, LOWER, ELBOW);
            arms.pose(side, UPPER, LOWER);
        }
        arms.submit(view, event.getPackedLight(), 0xFFFFFFFF, player);
    }

    private static void screenQuad(PoseStack pose) {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
              DefaultVertexFormat.POSITION_TEX);

        buffer.addVertex(matrix, -HALF_W, -HALF_H, 0).setUv(0, 1);
        buffer.addVertex(matrix, HALF_W, -HALF_H, 0).setUv(1, 1);
        buffer.addVertex(matrix, HALF_W, HALF_H, 0).setUv(1, 0);
        buffer.addVertex(matrix, -HALF_W, HALF_H, 0).setUv(0, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawVoronoi(PoseStack pose) {
        ShaderInstance shader = PdaShaders.voronoi();
        if (shader == null) {
            return;
        }
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("Time").set((System.currentTimeMillis() % 3_600_000L) / 1000.0f);
        shader.safeGetUniform("Panel").set(0.025f, 0.0385f, 0.975f, 0.9615f);
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        screenQuad(pose);
        RenderSystem.defaultBlendFunc();
    }

    private static int cachedHeight = -1;
    private static int cachedScale = 2;

    private static int measurePixelScale() {
        Minecraft minecraft = Minecraft.getInstance();
        int height = minecraft.getWindow().getHeight();
        if (height == cachedHeight) {
            return cachedScale;
        }
        cachedHeight = height;
        float projection = RenderSystem.getProjectionMatrix().m00();
        float halfWidth = HALF_W * PDA_SCALE;
        float ndc = projection * halfWidth / RAISED_Z;
        float pixels = ndc * minecraft.getWindow().getWidth();
        cachedScale = Math.round(pixels / PdaSurface.WIDTH);
        return cachedScale;
    }

}
