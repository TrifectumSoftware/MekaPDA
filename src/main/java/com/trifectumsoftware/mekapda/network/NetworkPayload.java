package com.trifectumsoftware.mekapda.network;

import java.util.List;

import com.trifectumsoftware.mekapda.MekaPDA;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NetworkPayload(NetworkSummary summary, List<DeviceEntry> devices) implements CustomPacketPayload {

    public static final Type<NetworkPayload> TYPE = new Type<>(
          ResourceLocation.fromNamespaceAndPath(MekaPDA.MODID, "network"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkPayload> STREAM_CODEC = StreamCodec.of(
          (buf, payload) -> {
              PdaCodecs.writeSummary(buf, payload.summary);
              buf.writeVarInt(payload.devices.size());
              for (DeviceEntry device : payload.devices) {
                  PdaCodecs.writeDevice(buf, device);
              }
          },
          buf -> new NetworkPayload(PdaCodecs.readSummary(buf), PdaCodecs.readDevices(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
