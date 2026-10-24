package com.trifectumsoftware.mekapda.client;

import com.trifectumsoftware.mekapda.MekaPDA;
import com.trifectumsoftware.mekapda.item.MekaPdaItem;
import com.trifectumsoftware.mekapda.network.DeviceEntry;
import com.trifectumsoftware.mekapda.network.OpenDevicePayload;
import com.trifectumsoftware.mekapda.client.ui.PdaSurface;
import com.trifectumsoftware.mekapda.client.ui.PdaView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MekaPDA.MODID, value = Dist.CLIENT)
public final class PdaClient {

    private static final float RAISE_STEP = 0.25f;
    private static final float CURSOR_GAIN = 0.55f;
    private static final PdaSurface SURFACE = new PdaSurface();

    private static boolean held;
    private static boolean focused;
    private static boolean useLatch;
    private static float raise;
    private static float raiseO;
    private static float cursorX = PdaSurface.WIDTH / 2;
    private static float cursorY = PdaSurface.HEIGHT / 2;
    private static boolean pressed;
    private static boolean pressLatch;
    private static float wheel;
    private static BlockPos highlightPos;
    private static ResourceLocation highlightDimension;
    private static long highlightUntil;

    private PdaClient() {
    }

    public static PdaSurface surface() {
        return SURFACE;
    }

    public static boolean held() {
        return held;
    }

    public static boolean focused() {
        return focused;
    }

    public static float raise(float partialTick) {
        return raiseO + (raise - raiseO) * partialTick;
    }

    public static boolean pressed() {
        return pressed;
    }

    public static float wheel() {
        return wheel;
    }

    public static void lower() {
        focused = false;
        pressed = false;
        pressLatch = false;
        wheel = 0;
    }

    public static void focus() {
        focused = true;
    }

    public static void openDeviceGui(DeviceEntry device) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        PacketDistributor.sendToServer(new OpenDevicePayload(device.dimension(), device.pos()));
    }

    public static void toggleHighlight(DeviceEntry device) {
        long now = System.currentTimeMillis();
        if (highlightPos != null && highlightPos.equals(device.pos())
              && highlightDimension != null && highlightDimension.equals(device.dimension())
              && now < highlightUntil) {
            highlightPos = null;
            highlightDimension = null;
            highlightUntil = 0;
        } else {
            highlightPos = device.pos().immutable();
            highlightDimension = device.dimension();
            highlightUntil = now + 10_000;
        }
    }

    public static boolean highlightActive() {
        return highlightPos != null && System.currentTimeMillis() < highlightUntil;
    }

    public static BlockPos highlightPos() {
        return highlightPos;
    }

    public static ResourceLocation highlightDimension() {
        return highlightDimension;
    }

    public static boolean steer(double yaw, double pitch) {
        if (!focused || Minecraft.getInstance().screen != null) {
            return false;
        }

        float minX = PdaView.PANEL_X + 1;
        float maxX = PdaView.PANEL_X + PdaView.PANEL_W - 1;
        float minY = PdaView.PANEL_Y + 1;
        float maxY = PdaView.PANEL_Y + PdaView.PANEL_H - 1;
        cursorX = Math.max(minX, Math.min(maxX, cursorX + (float) yaw * CURSOR_GAIN));
        cursorY = Math.max(minY, Math.min(maxY, cursorY + (float) pitch * CURSOR_GAIN));
        return true;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        raiseO = raise;
        if (player == null) {
            held = false;
            lower();
            raise = raiseO = 0;
            return;
        }
        held = player.getMainHandItem().getItem() instanceof MekaPdaItem;
        if (!held || minecraft.screen != null || !player.isAlive()) {
            lower();
        }
        if (!minecraft.options.keyUse.isDown()) {
            useLatch = false;
        }
        raise = focused ? Math.min(1, raise + RAISE_STEP) : Math.max(0, raise - RAISE_STEP);
    }

    @SubscribeEvent
    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!held || minecraft.screen != null || minecraft.player == null) {
            return;
        }
        if (event.isUseItem()) {

            if (!focused && minecraft.player.isShiftKeyDown()) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.getHand() != InteractionHand.MAIN_HAND || useLatch) {
                return;
            }
            useLatch = true;
            if (focused) {
                lower();
            } else {
                focus();
            }
        } else if (focused) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!focused || Minecraft.getInstance().screen != null) {
            return;
        }

        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        pressed = event.getAction() != GLFW.GLFW_RELEASE;
        pressLatch |= pressed;
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (focused && Minecraft.getInstance().screen == null) {
            wheel += (float) event.getScrollDeltaY();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (focused && event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreen(ScreenEvent.Opening event) {
        if (focused) {
            if (event.getNewScreen() instanceof PauseScreen) {
                event.setCanceled(true);
            }
            lower();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onFrame(RenderFrameEvent.Pre event) {
        if (!held) {
            return;
        }
        SURFACE.update(cursorX, cursorY, focused, pressed || pressLatch, wheel);
        pressLatch = false;
        wheel = 0;
    }
}
