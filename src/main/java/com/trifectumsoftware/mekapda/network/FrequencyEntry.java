package com.trifectumsoftware.mekapda.network;

import java.util.Objects;
import java.util.UUID;

import mekanism.api.security.SecurityMode;

public record FrequencyEntry(String typeName, int security, String name, UUID owner, String ownerName,
                             int deviceCount, long itemCount, long energy, long fluid, long chemical) {

    public SecurityMode securityMode() {
        SecurityMode[] values = SecurityMode.values();
        return values[Math.max(0, Math.min(values.length - 1, security))];
    }

    public boolean sameIdentity(String typeName, int security, String name, UUID owner) {
        return this.typeName.equals(typeName) && this.security == security && this.name.equals(name)
              && Objects.equals(this.owner, owner);
    }
}
