package com.trifectumsoftware.mekapda.network;

import java.util.UUID;

import com.trifectumsoftware.mekapda.MekaPDA;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestNetworkPayload(String typeName, int security, String name, UUID owner)
      implements CustomPacketPayload {

    public static final Type<RequestNetworkPayload> TYPE = new Type<>(
          ResourceLocation.fromNamespaceAndPath(MekaPDA.MODID, "request_network"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestNetworkPayload> STREAM_CODEC = StreamCodec.of(
          (buf, payload) -> {
              buf.writeUtf(payload.typeName, 64);
              buf.writeVarInt(payload.security);
              buf.writeUtf(payload.name, 64);
              PdaCodecs.writeNullableUuid(buf, payload.owner);
          },
          buf -> new RequestNetworkPayload(buf.readUtf(64), buf.readVarInt(), buf.readUtf(64),
                PdaCodecs.readNullableUuid(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
