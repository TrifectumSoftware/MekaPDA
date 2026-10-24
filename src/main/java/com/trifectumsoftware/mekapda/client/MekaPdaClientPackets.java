package com.trifectumsoftware.mekapda.client;

import com.trifectumsoftware.mekapda.network.FrequencyListPayload;
import com.trifectumsoftware.mekapda.network.NetworkPayload;

public final class MekaPdaClientPackets {

    private MekaPdaClientPackets() {
    }

    public static void onFrequencyList(FrequencyListPayload payload) {
        MekaPdaClientState.onFrequencyList(payload.entries());
    }

    public static void onNetwork(NetworkPayload payload) {
        MekaPdaClientState.onNetwork(payload);
    }
}
