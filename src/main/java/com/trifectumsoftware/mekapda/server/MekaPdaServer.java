package com.trifectumsoftware.mekapda.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.trifectumsoftware.mekapda.network.DeviceEntry;
import com.trifectumsoftware.mekapda.network.FrequencyEntry;
import com.trifectumsoftware.mekapda.network.FrequencyListPayload;
import com.trifectumsoftware.mekapda.network.NetworkPayload;
import com.trifectumsoftware.mekapda.network.NetworkSummary;
import com.trifectumsoftware.mekapda.network.RequestNetworkPayload;

import mekanism.api.security.SecurityMode;
import mekanism.common.content.entangloporter.InventoryFrequency;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.content.teleporter.TeleporterFrequency;
import mekanism.common.lib.frequency.Frequency;
import mekanism.common.lib.frequency.FrequencyManager;
import mekanism.common.lib.frequency.FrequencyManagerWrapper;
import mekanism.common.lib.frequency.FrequencyType;
import net.minecraft.server.level.ServerPlayer;

public final class MekaPdaServer {

    public static final List<FrequencyType<?>> FREQUENCY_TYPES = List.of(FrequencyType.QIO,
          FrequencyType.INVENTORY, FrequencyType.TELEPORTER);

    private MekaPdaServer() {
    }

    public static FrequencyListPayload buildFrequencyList(ServerPlayer player) {
        FrequencyManager.load();
        Map<FrequencyKey, Integer> counts = PdaScanner.countDevices(player.server);
        List<FrequencyEntry> entries = new ArrayList<>();
        for (FrequencyType<?> type : FREQUENCY_TYPES) {
            for (SecurityMode security : List.of(SecurityMode.PUBLIC, SecurityMode.TRUSTED, SecurityMode.PRIVATE)) {
                FrequencyManager<?> manager = manager(type, player, security);
                if (manager == null) {
                    continue;
                }
                for (Frequency frequency : manager.getFrequencies()) {
                    if (isLive(frequency)) {
                        entries.add(entry(frequency, counts.getOrDefault(FrequencyKey.of(frequency), 0)));
                    }
                }
            }
        }
        return new FrequencyListPayload(entries);
    }

    public static NetworkPayload buildNetwork(ServerPlayer player, RequestNetworkPayload request) {
        FrequencyManager.load();
        Frequency target = findAccessible(player, request);
        if (target == null) {
            return new NetworkPayload(NetworkSummary.empty(request.typeName(), request.security(), request.name(),
                  request.owner(), ""), List.of());
        }
        List<DeviceEntry> devices = new ArrayList<>();
        for (PdaScanner.DeviceEntryData data : PdaScanner.findDevices(player.server, target)) {
            PdaScanner.Contents c = data.contents();
            devices.add(new DeviceEntry(data.name(), data.dimension(), data.pos(), data.drives(), data.driveSlots(),
                  c.items(), c.energy(), c.fluid(), c.chemical()));
        }
        return new NetworkPayload(summary(target, devices), devices);
    }

    private static FrequencyEntry entry(Frequency frequency, int deviceCount) {
        long items = 0;
        long energy = 0;
        long fluid = 0;
        long chemical = 0;
        if (frequency instanceof QIOFrequency qio) {
            items = qio.getTotalItemCount();
        } else if (frequency instanceof InventoryFrequency inventory) {
            PdaScanner.Contents c = PdaScanner.contentsOf(inventory);
            items = c.items();
            energy = c.energy();
            fluid = c.fluid();
            chemical = c.chemical();
        } else if (frequency instanceof TeleporterFrequency teleporter) {
            deviceCount = Math.max(deviceCount, teleporter.getActiveCoords().size());
        }
        return new FrequencyEntry(frequency.getType().getName(), frequency.getSecurity().ordinal(),
              frequency.getName(), frequency.getOwner(), frequency.getOwnerName(), deviceCount, items, energy, fluid,
              chemical);
    }

    private static NetworkSummary summary(Frequency frequency, List<DeviceEntry> devices) {
        FrequencyEntry entry = entry(frequency, devices.size());
        return new NetworkSummary(entry.typeName(), entry.security(), entry.name(), entry.owner(), entry.ownerName(),
              entry.deviceCount(), entry.itemCount(), entry.energy(), entry.fluid(), entry.chemical());
    }

    private static Frequency findAccessible(ServerPlayer player, RequestNetworkPayload request) {
        FrequencyType<?> type = typeByName(request.typeName());
        if (type == null) {
            return null;
        }
        SecurityMode security = security(request.security());
        FrequencyManager<?> manager = manager(type, player, security);
        if (manager == null) {
            return null;
        }
        for (Frequency frequency : manager.getFrequencies()) {
            if (isLive(frequency) && frequency.getName().equals(request.name()) && frequency.getSecurity() == security
                  && Objects.equals(frequency.getOwner(), request.owner())) {
                if (security == SecurityMode.PRIVATE && !Objects.equals(frequency.getOwner(), player.getUUID())) {
                    return null;
                }
                return frequency;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static FrequencyManager<?> manager(FrequencyType<?> type, ServerPlayer player, SecurityMode security) {
        FrequencyManagerWrapper wrapper = type.getManagerWrapper();
        return switch (security) {
            case PUBLIC -> wrapper.getPublicManager();
            case PRIVATE -> wrapper.getPrivateManager(player.getUUID());
            case TRUSTED -> wrapper.getTrustedManager(player.getUUID());
        };
    }

    private static FrequencyType<?> typeByName(String name) {
        for (FrequencyType<?> type : FREQUENCY_TYPES) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    private static SecurityMode security(int ordinal) {
        SecurityMode[] values = SecurityMode.values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }

    private static boolean isLive(Frequency frequency) {
        return frequency != null && frequency.isValid() && !frequency.isRemoved();
    }

}
