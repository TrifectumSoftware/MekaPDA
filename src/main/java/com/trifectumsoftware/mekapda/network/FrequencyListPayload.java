package com.trifectumsoftware.mekapda.network;

import java.util.List;

import com.trifectumsoftware.mekapda.MekaPDA;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FrequencyListPayload(List<FrequencyEntry> entries) implements CustomPacketPayload {

    public static final Type<FrequencyListPayload> TYPE = new Type<>(
          ResourceLocation.fromNamespaceAndPath(MekaPDA.MODID, "frequency_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FrequencyListPayload> STREAM_CODEC = StreamCodec.of(
          (buf, payload) -> {
              buf.writeVarInt(payload.entries.size());
              for (FrequencyEntry entry : payload.entries) {
                  PdaCodecs.writeFrequency(buf, entry);
              }
          },
          buf -> new FrequencyListPayload(PdaCodecs.readFrequencies(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
