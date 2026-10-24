package com.trifectumsoftware.mekapda.network;

import com.trifectumsoftware.mekapda.MekaPDA;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestFrequenciesPayload() implements CustomPacketPayload {

    public static final Type<RequestFrequenciesPayload> TYPE = new Type<>(
          ResourceLocation.fromNamespaceAndPath(MekaPDA.MODID, "request_frequencies"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestFrequenciesPayload> STREAM_CODEC =
          StreamCodec.unit(new RequestFrequenciesPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
