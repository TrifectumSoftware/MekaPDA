package com.trifectumsoftware.mekapda.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.content.qio.IQIODriveHolder;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.lib.frequency.Frequency;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PdaScanner {

    private PdaScanner() {
    }

    public static Map<FrequencyKey, Integer> countDevices(MinecraftServer server) {
        Map<FrequencyKey, Integer> counts = new HashMap<>();
        forEachTile(server, tile -> {
            for (FrequencyType<?> type : MekaPdaServer.FREQUENCY_TYPES) {
                Frequency frequency = selected(tile, type);
                if (isLive(frequency)) {
                    counts.merge(FrequencyKey.of(frequency), 1, Integer::sum);
                }
            }
        });
        return counts;
    }

    public static List<DeviceEntryData> findDevices(MinecraftServer server, Frequency target) {
        List<DeviceEntryData> devices = new ArrayList<>();
        forEachTile(server, tile -> {
            Frequency frequency = selected(tile, target.getType());
            if (!isLive(frequency) || !sameFrequency(frequency, target)) {
                return;
            }
            devices.add(describe(tile, target));
        });
        return devices;
    }

    private static boolean isLive(Frequency frequency) {
        return frequency != null && frequency.isValid() && !frequency.isRemoved();
    }

    private static boolean sameFrequency(Frequency a, Frequency b) {
        return a.getName().equals(b.getName()) && a.getSecurity() == b.getSecurity()
              && Objects.equals(a.getOwner(), b.getOwner());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Frequency selected(TileEntityMekanism tile, FrequencyType<?> type) {
        return (Frequency) tile.getFrequencyComponent().getFrequency((FrequencyType) type);
    }

    private static DeviceEntryData describe(TileEntityMekanism tile, Frequency target) {
        Contents contents;
        int drives = 0;
        int driveSlots = 0;
        if (target instanceof QIOFrequency) {

            if (tile instanceof IQIODriveHolder holder) {
                var slots = holder.getDriveSlots();
                if (slots != null) {
                    driveSlots = slots.size();
                    for (var slot : slots) {
                        if (!slot.isEmpty()) {
                            drives++;
                        }
                    }
                }
            }

            contents = driveSlots > 0 ? new Contents(drives, 0, 0, 0) : Contents.EMPTY;
        } else if (target instanceof mekanism.common.content.entangloporter.InventoryFrequency inventory) {
            contents = contentsOf(inventory);
        } else {
            contents = contentsOf(tile);
        }
        return new DeviceEntryData(tile.getDisplayName().getString(), tile.getLevel().dimension().location(),
              tile.getBlockPos(), drives, driveSlots, contents);
    }

    private static Contents contentsOf(TileEntityMekanism tile) {
        long items = 0;
        for (IInventorySlot slot : tile.getInventorySlots(null)) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                items += slot.getCount();
            }
        }
        long energy = 0;
        for (IEnergyContainer container : tile.getEnergyContainers(null)) {
            energy += container.getEnergy();
        }
        long fluid = 0;
        for (IExtendedFluidTank tank : tile.getFluidTanks(null)) {
            fluid += tank.getFluidAmount();
        }
        long chemical = 0;
        for (IChemicalTank tank : tile.getChemicalTanks(null)) {
            chemical += tank.getStored();
        }
        return new Contents(items, energy, fluid, chemical);
    }

    public static Contents contentsOf(mekanism.common.content.entangloporter.InventoryFrequency frequency) {
        long items = 0;
        for (IInventorySlot slot : frequency.getInventorySlots(null)) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                items += slot.getCount();
            }
        }
        long energy = 0;
        for (IEnergyContainer container : frequency.getEnergyContainers(null)) {
            energy += container.getEnergy();
        }
        long fluid = 0;
        for (IExtendedFluidTank tank : frequency.getFluidTanks(null)) {
            fluid += tank.getFluidAmount();
        }
        long chemical = 0;
        for (IChemicalTank tank : frequency.getChemicalTanks(null)) {
            chemical += tank.getStored();
        }
        return new Contents(items, energy, fluid, chemical);
    }

    private static void forEachTile(MinecraftServer server, TileConsumer consumer) {
        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkHolder holder : level.getChunkSource().chunkMap.getChunks()) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk == null) {
                    chunk = holder.getChunkToSend();
                }
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof TileEntityMekanism tile && !tile.isRemoved()) {
                        consumer.accept(tile);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface TileConsumer {

        void accept(TileEntityMekanism tile);
    }

    public record DeviceEntryData(String name, net.minecraft.resources.ResourceLocation dimension, BlockPos pos,
                                  int drives, int driveSlots, Contents contents) {
    }

    public record Contents(long items, long energy, long fluid, long chemical) {

        public static final Contents EMPTY = new Contents(0, 0, 0, 0);
    }
}
