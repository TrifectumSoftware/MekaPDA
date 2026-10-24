package com.trifectumsoftware.mekapda.client.render;

import com.trifectumsoftware.mekapda.MekaPDA;
import com.wf.gemrender.asset.GemRenderModels;
import com.wf.gemrender.asset.ModelCache;
import com.wf.gemrender.direct.DirectPass;
import com.wf.gemrender.direct.DirectRenderer;
import com.wf.gemrender.direct.TextureSlots;
import com.wf.gemrender.gltf.GemRenderGltfModel;
import com.wf.gemrender.gltf.GltfPaletteLayout;
import com.wf.gemrender.gltf.NodeTable;
import com.wf.gemrender.texture.Paint;
import com.wf.gemrender.texture.VariantUv;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class PdaArms {

    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(MekaPDA.MODID,
          "models/arms/player_arms.glb");
    private static final String[] SIDES = {"right", "left"};

    private static final int TWISTS = 6;

    public record Lengths(float upper, float forearm) {
    }

    private static ModelCache.Handle<GemRenderGltfModel> handle;
    private static PdaArms cached;

    private final GemRenderGltfModel model;
    private final Matrix4f[] palette;
    private final Matrix4f[] rest;

    private final int[][] joints = new int[2][3];

    private final int[][] twists = new int[2][TWISTS - 1];
    private final Matrix4f[] upper = {new Matrix4f(), new Matrix4f()};
    private final Matrix4f[] lower = {new Matrix4f(), new Matrix4f()};
    private final Matrix4f[] twistOut = new Matrix4f[TWISTS - 1];
    public final Lengths lengths;

    private PdaArms(GemRenderGltfModel model) {
        this.model = model;
        palette = model.newPalette();
        rest = model.restPalette();
        NodeTable table = model.layout().nodeTable();
        String[] names = {"upper", "lower", "fist"};
        for (int side = 0; side < 2; side++) {
            for (int j = 0; j < 3; j++) {
                int slot = table.slotOfName("arm_" + SIDES[side] + "_" + names[j]);
                if (slot < 0) {
                    throw new IllegalStateException(MODEL + ": no node arm_" + SIDES[side] + "_" + names[j]);
                }
                joints[side][j] = slot;
            }
            for (int k = 1; k < TWISTS; k++) {
                int slot = table.slotOfName("arm_" + SIDES[side] + "_twist" + k);
                if (slot < 0) {
                    throw new IllegalStateException(MODEL + ": no node arm_" + SIDES[side] + "_twist" + k);
                }
                twists[side][k - 1] = slot;
            }
        }
        lengths = new Lengths(table.restTranslation(joints[0][1], 1), table.restTranslation(joints[0][2], 1));
    }

    @Nullable
    public static PdaArms get() {
        if (handle == null) {
            handle = GemRenderModels.handle(MODEL);
        }
        GemRenderGltfModel loaded = handle.get();
        if (loaded == null) {
            return null;
        }
        if (cached == null || cached.model != loaded) {
            cached = new PdaArms(loaded);
        }
        return cached;
    }

    public void pose(int side, Matrix4fc upperMatrix, Matrix4fc lowerMatrix) {
        upper[side].set(upperMatrix);
        lower[side].set(lowerMatrix);
    }

    private static void jointMatrices(Matrix4fc upper, Matrix4fc lower, float forearm, Matrix4f upperOut,
                                      Matrix4f lowerOut, Matrix4f[] twists, Matrix4f fist) {
        upperOut.set(upper);
        lowerOut.set(lower);
        for (int k = 1; k < TWISTS; k++) {
            twists[k - 1].set(lower).translate(0, forearm * k / TWISTS, 0);
        }
        fist.set(lower).translate(0, forearm, 0);
    }

    private void palette(int side) {
        int[] j = joints[side];
        for (int k = 1; k < TWISTS; k++) {
            twistOut[k - 1] = palette[twists[side][k - 1]];
        }
        jointMatrices(upper[side], lower[side], lengths.forearm(), palette[j[0]], palette[j[1]], twistOut,
              palette[j[2]]);
    }

    public void submit(Matrix4f base, int light, int argb, AbstractClientPlayer player) {
        palette(0);
        palette(1);
        for (GltfPaletteLayout.SkinBlock block : model.layout().skins()) {
            int[] slots = block.jointSlots();
            for (int k = 0; k < slots.length; k++) {
                palette[block.base() + k].set(palette[slots[k]]).mul(block.inverseBind()[k]);
            }
        }
        PlayerSkin skin = player.getSkin();
        ResourceLocation texture = skin.texture();
        boolean slim = skin.model() == PlayerSkin.Model.SLIM;
        boolean rightSleeve = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        boolean leftSleeve = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        TextureSlots slots = slot -> {
            if (slot.endsWith("_slim") != slim) {
                return null;
            }
            String name = slim ? slot.substring(0, slot.length() - "_slim".length()) : slot;
            return switch (name) {
                case "player_arm" -> texture;
                case "player_sleeve_right" -> rightSleeve ? texture : null;
                case "player_sleeve_left" -> leftSleeve ? texture : null;
                default -> null;
            };
        };
        DirectRenderer.submit(model, palette, null, base, light, OverlayTexture.NO_OVERLAY, argb, DirectPass.HAND,
              VariantUv.NONE, Paint.NONE, 0, rest, slots);
    }
}
