package com.trifectumsoftware.mekapda.client.render;

import java.io.IOException;

import com.trifectumsoftware.mekapda.MekaPDA;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = MekaPDA.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PdaShaders {

    private static ShaderInstance voronoi;

    private PdaShaders() {
    }

    public static ShaderInstance voronoi() {
        return voronoi;
    }

    @SubscribeEvent
    static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
              ResourceLocation.fromNamespaceAndPath(MekaPDA.MODID, "voronoi"), DefaultVertexFormat.POSITION_TEX),
              shader -> voronoi = shader);
    }
}
