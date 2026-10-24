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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = MekaPDA.MODID, value = Dist.CLIENT)
public final class PdaHighlightRenderer {

    private static final int FACE = 0x3375FF89;
    private static final int EDGE = 0xFF75FF89;

    private PdaHighlightRenderer() {
    }

    @SubscribeEvent
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !PdaClient.highlightActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(PdaClient.highlightDimension())) {
            return;
        }
        BlockPos pos = PdaClient.highlightPos();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        faces(pose);
        RenderSystem.lineWidth(2.0F);
        edges(pose);
        RenderSystem.lineWidth(1.0F);

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    private static void faces(PoseStack pose) {
        Matrix4f matrix = pose.last().pose();
        float lo = -0.002f;
        float hi = 1.002f;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
              DefaultVertexFormat.POSITION_COLOR);

        quad(buffer, matrix, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi, FACE);
        quad(buffer, matrix, lo, hi, lo, hi, hi, lo, hi, hi, hi, lo, hi, hi, FACE);

        quad(buffer, matrix, lo, lo, lo, hi, lo, lo, hi, hi, lo, lo, hi, lo, FACE);
        quad(buffer, matrix, lo, lo, hi, hi, lo, hi, hi, hi, hi, lo, hi, hi, FACE);

        quad(buffer, matrix, lo, lo, lo, lo, lo, hi, lo, hi, hi, lo, hi, lo, FACE);
        quad(buffer, matrix, hi, lo, lo, hi, lo, hi, hi, hi, hi, hi, hi, lo, FACE);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1,
                             float z1, float x2, float y2, float z2, float x3, float y3, float z3, int argb) {
        buffer.addVertex(matrix, x0, y0, z0).setColor(argb);
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x3, y3, z3).setColor(argb);
    }

    private static void edges(PoseStack pose) {
        Matrix4f matrix = pose.last().pose();
        float lo = -0.002f;
        float hi = 1.002f;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES,
              DefaultVertexFormat.POSITION_COLOR);
        line(buffer, matrix, lo, lo, lo, hi, lo, lo);
        line(buffer, matrix, hi, lo, lo, hi, lo, hi);
        line(buffer, matrix, hi, lo, hi, lo, lo, hi);
        line(buffer, matrix, lo, lo, hi, lo, lo, lo);
        line(buffer, matrix, lo, hi, lo, hi, hi, lo);
        line(buffer, matrix, hi, hi, lo, hi, hi, hi);
        line(buffer, matrix, hi, hi, hi, lo, hi, hi);
        line(buffer, matrix, lo, hi, hi, lo, hi, lo);
        line(buffer, matrix, lo, lo, lo, lo, hi, lo);
        line(buffer, matrix, hi, lo, lo, hi, hi, lo);
        line(buffer, matrix, hi, lo, hi, hi, hi, hi);
        line(buffer, matrix, lo, lo, hi, lo, hi, hi);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1,
                             float z1) {
        buffer.addVertex(matrix, x0, y0, z0).setColor(EDGE);
        buffer.addVertex(matrix, x1, y1, z1).setColor(EDGE);
    }
}
