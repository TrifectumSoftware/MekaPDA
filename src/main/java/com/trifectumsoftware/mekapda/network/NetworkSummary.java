package com.trifectumsoftware.mekapda.network;

import java.util.UUID;

public record NetworkSummary(String typeName, int security, String name, UUID owner, String ownerName,
                             int devices, long itemCount, long energy, long fluid, long chemical) {

    public static NetworkSummary empty(String typeName, int security, String name, UUID owner, String ownerName) {
        return new NetworkSummary(typeName, security, name, owner, ownerName, 0, 0, 0, 0, 0);
    }
}
