package com.trifectumsoftware.mekapda.network;

import com.trifectumsoftware.mekapda.MekaPDA;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenDevicePayload(ResourceLocation dimension, BlockPos pos) implements CustomPacketPayload {

    public static final Type<OpenDevicePayload> TYPE = new Type<>(
          ResourceLocation.fromNamespaceAndPath(MekaPDA.MODID, "open_device"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDevicePayload> STREAM_CODEC = StreamCodec.of(
          (buf, payload) -> {
              buf.writeResourceLocation(payload.dimension);
              buf.writeBlockPos(payload.pos);
          },
          buf -> new OpenDevicePayload(buf.readResourceLocation(), buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
