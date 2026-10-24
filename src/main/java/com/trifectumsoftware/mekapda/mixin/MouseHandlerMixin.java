package com.trifectumsoftware.mekapda.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.trifectumsoftware.mekapda.client.PdaClient;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MouseHandler.class, priority = 1500)
public abstract class MouseHandlerMixin {

    @WrapOperation(method = "turnPlayer",
          at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void mekapda$steerPda(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        if (!PdaClient.steer(yaw, pitch)) {
            original.call(player, yaw, pitch);
        }
    }
}
