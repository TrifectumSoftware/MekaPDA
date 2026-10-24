package com.trifectumsoftware.mekapda.server;

import java.util.UUID;

import mekanism.common.lib.frequency.Frequency;

public record FrequencyKey(String typeName, int security, String name, UUID owner) {

    public static FrequencyKey of(Frequency frequency) {
        return new FrequencyKey(frequency.getType().getName(), frequency.getSecurity().ordinal(), frequency.getName(),
              frequency.getOwner());
    }
}
