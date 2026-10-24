package com.trifectumsoftware.mekapda.client;

import java.util.List;

import com.trifectumsoftware.mekapda.network.FrequencyEntry;
import com.trifectumsoftware.mekapda.network.NetworkPayload;
import com.trifectumsoftware.mekapda.network.RequestFrequenciesPayload;
import com.trifectumsoftware.mekapda.network.RequestNetworkPayload;
import com.trifectumsoftware.mekapda.client.ui.PdaView;

import net.neoforged.neoforge.network.PacketDistributor;

public final class MekaPdaClientState {

    private static List<FrequencyEntry> frequencies = List.of();
    private static PdaView view;

    private MekaPdaClientState() {
    }

    public static List<FrequencyEntry> frequencies() {
        return frequencies;
    }

    public static void attach(PdaView newView) {
        view = newView;
    }

    public static void detach(PdaView oldView) {
        if (view == oldView) {
            view = null;
        }
    }

    public static void requestFrequencies() {
        PacketDistributor.sendToServer(new RequestFrequenciesPayload());
    }

    public static void requestNetwork(FrequencyEntry entry) {
        PacketDistributor.sendToServer(new RequestNetworkPayload(entry.typeName(), entry.security(), entry.name(),
              entry.owner()));
    }

    static void onFrequencyList(List<FrequencyEntry> entries) {
        frequencies = List.copyOf(entries);
        if (view != null) {
            view.onFrequencyList(frequencies);
        }
    }

    static void onNetwork(NetworkPayload payload) {
        if (view != null) {
            view.onNetwork(payload);
        }
    }
}
