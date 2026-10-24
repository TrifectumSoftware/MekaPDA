package com.trifectumsoftware.mekapda.network;

import com.trifectumsoftware.mekapda.MekaPDA;
import com.trifectumsoftware.mekapda.server.MekaPdaServer;

import mekanism.common.tile.base.TileEntityMekanism;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MekaPdaNetwork {

    private static final String VERSION = "1";
    private static final String CLIENT_PACKETS = "com.trifectumsoftware.mekapda.client.MekaPdaClientPackets";

    private MekaPdaNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(MekaPdaNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(RequestFrequenciesPayload.TYPE, RequestFrequenciesPayload.STREAM_CODEC,
              MekaPdaNetwork::handleRequestFrequencies);
        registrar.playToServer(RequestNetworkPayload.TYPE, RequestNetworkPayload.STREAM_CODEC,
              MekaPdaNetwork::handleRequestNetwork);
        registrar.playToServer(OpenDevicePayload.TYPE, OpenDevicePayload.STREAM_CODEC,
              MekaPdaNetwork::handleOpenDevice);
        registrar.playToClient(FrequencyListPayload.TYPE, FrequencyListPayload.STREAM_CODEC,
              MekaPdaNetwork::handleFrequencyList);
        registrar.playToClient(NetworkPayload.TYPE, NetworkPayload.STREAM_CODEC, MekaPdaNetwork::handleNetwork);
    }

    private static void handleRequestFrequencies(RequestFrequenciesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                context.reply(MekaPdaServer.buildFrequencyList(player));
            }
        });
    }

    private static void handleRequestNetwork(RequestNetworkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                context.reply(MekaPdaServer.buildNetwork(player, payload));
            }
        });
    }

    private static void handleOpenDevice(OpenDevicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, payload.dimension()));
            if (level == null) {
                return;
            }
            BlockEntity blockEntity = level.getBlockEntity(payload.pos());
            if (blockEntity instanceof TileEntityMekanism tile && !tile.isRemoved()) {
                tile.openGui(player);
            }
        });
    }

    private static void handleFrequencyList(FrequencyListPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> callClient("onFrequencyList", FrequencyListPayload.class, payload));
        }
    }

    private static void handleNetwork(NetworkPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> callClient("onNetwork", NetworkPayload.class, payload));
        }
    }

    private static void callClient(String method, Class<?> payloadType, Object payload) {
        try {
            Class.forName(CLIENT_PACKETS).getMethod(method, payloadType).invoke(null, payload);
        } catch (ReflectiveOperationException e) {
            MekaPDA.LOGGER.error("Failed to hand a PDA payload to the client", e);
        }
    }
}
