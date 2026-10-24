package com.trifectumsoftware.mekapda.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class PdaCodecs {

    private PdaCodecs() {
    }

    public static void writeNullableUuid(RegistryFriendlyByteBuf buf, UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeUUID(uuid);
        }
    }

    public static UUID readNullableUuid(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUUID() : null;
    }

    public static void writeFrequency(RegistryFriendlyByteBuf buf, FrequencyEntry entry) {
        buf.writeUtf(entry.typeName(), 64);
        buf.writeVarInt(entry.security());
        buf.writeUtf(entry.name(), 64);
        writeNullableUuid(buf, entry.owner());
        buf.writeUtf(entry.ownerName(), 64);
        buf.writeVarInt(entry.deviceCount());
        buf.writeVarLong(entry.itemCount());
        buf.writeVarLong(entry.energy());
        buf.writeVarLong(entry.fluid());
        buf.writeVarLong(entry.chemical());
    }

    public static FrequencyEntry readFrequency(RegistryFriendlyByteBuf buf) {
        String type = buf.readUtf(64);
        int security = buf.readVarInt();
        String name = buf.readUtf(64);
        UUID owner = readNullableUuid(buf);
        String ownerName = buf.readUtf(64);
        int devices = buf.readVarInt();
        long items = buf.readVarLong();
        long energy = buf.readVarLong();
        long fluid = buf.readVarLong();
        long chemical = buf.readVarLong();
        return new FrequencyEntry(type, security, name, owner, ownerName, devices, items, energy, fluid, chemical);
    }

    public static void writeSummary(RegistryFriendlyByteBuf buf, NetworkSummary summary) {
        buf.writeUtf(summary.typeName(), 64);
        buf.writeVarInt(summary.security());
        buf.writeUtf(summary.name(), 64);
        writeNullableUuid(buf, summary.owner());
        buf.writeUtf(summary.ownerName(), 64);
        buf.writeVarInt(summary.devices());
        buf.writeVarLong(summary.itemCount());
        buf.writeVarLong(summary.energy());
        buf.writeVarLong(summary.fluid());
        buf.writeVarLong(summary.chemical());
    }

    public static NetworkSummary readSummary(RegistryFriendlyByteBuf buf) {
        String type = buf.readUtf(64);
        int security = buf.readVarInt();
        String name = buf.readUtf(64);
        UUID owner = readNullableUuid(buf);
        String ownerName = buf.readUtf(64);
        int devices = buf.readVarInt();
        long items = buf.readVarLong();
        long energy = buf.readVarLong();
        long fluid = buf.readVarLong();
        long chemical = buf.readVarLong();
        return new NetworkSummary(type, security, name, owner, ownerName, devices, items, energy, fluid, chemical);
    }

    public static void writeDevice(RegistryFriendlyByteBuf buf, DeviceEntry device) {
        buf.writeUtf(device.name(), 256);
        buf.writeResourceLocation(device.dimension());
        buf.writeBlockPos(device.pos());
        buf.writeVarInt(device.drives());
        buf.writeVarInt(device.driveSlots());
        buf.writeVarLong(device.itemCount());
        buf.writeVarLong(device.energy());
        buf.writeVarLong(device.fluid());
        buf.writeVarLong(device.chemical());
    }

    public static DeviceEntry readDevice(RegistryFriendlyByteBuf buf) {
        String name = buf.readUtf(256);
        ResourceLocation dimension = buf.readResourceLocation();
        BlockPos pos = buf.readBlockPos();
        int drives = buf.readVarInt();
        int driveSlots = buf.readVarInt();
        long items = buf.readVarLong();
        long energy = buf.readVarLong();
        long fluid = buf.readVarLong();
        long chemical = buf.readVarLong();
        return new DeviceEntry(name, dimension, pos, drives, driveSlots, items, energy, fluid, chemical);
    }

    static List<FrequencyEntry> readFrequencies(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<FrequencyEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(readFrequency(buf));
        }
        return entries;
    }

    static List<DeviceEntry> readDevices(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<DeviceEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(readDevice(buf));
        }
        return entries;
    }
}
