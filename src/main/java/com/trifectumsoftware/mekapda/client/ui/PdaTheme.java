package com.trifectumsoftware.mekapda.client.ui;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PdaTheme {

    public static final int PANEL = 0xFF020602;
    public static final int PANEL_INNER = 0xFF000000;
    public static final int HOVER = 0xFF0A2A10;
    public static final int EDGE = 0xFF2F6B36;
    public static final int EDGE_SOFT = 0x662F6B36;

    public static final int EDGE_FADE = 0xFF030704;
    public static final int ACCENT = 0xFF75FF89;
    public static final int ACCENT_DIM = 0x8075FF89;
    public static final int TEXT = 0xFF75FF89;
    public static final int TEXT_DIM = 0xFF59C15F;
    public static final int TEXT_MUTED = 0xFF2F6B36;

    public static final ResourceLocation ICON_PUBLIC = own("textures/gui/public.png");
    public static final ResourceLocation ICON_PRIVATE = own("textures/gui/private.png");
    public static final ResourceLocation ICON_TRUSTED = own("textures/gui/protected.png");
    public static final ResourceLocation ICON_ITEMS = own("textures/gui/items.png");
    public static final ResourceLocation ICON_ENERGY = own("textures/gui/energy.png");
    public static final ResourceLocation ICON_FLUID = own("textures/gui/fluids.png");
    public static final ResourceLocation ICON_CHEMICAL = own("textures/gui/chemicals.png");
    public static final ResourceLocation ICON_SORTING = own("textures/gui/sorting.png");
    public static final ResourceLocation ICON_CONFIG = own("textures/gui/config.png");
    public static final ResourceLocation ICON_BACK = own("textures/gui/back.png");

    private PdaTheme() {
    }

    private static ResourceLocation own(String path) {
        return ResourceLocation.fromNamespaceAndPath("mekapda", path);
    }

    public static ResourceLocation securityIcon(int securityOrdinal) {
        return switch (securityOrdinal) {
            case 2 -> ICON_TRUSTED;
            case 1 -> ICON_PRIVATE;
            default -> ICON_PUBLIC;
        };
    }

    public static String typeLabel(String typeName) {
        String key = switch (typeName) {
            case "QIO" -> "mekapda.type.qio";
            case "Inventory" -> "mekapda.type.inventory";
            case "Teleporter" -> "mekapda.type.teleporter";
            default -> "mekapda.type.qio";
        };
        return Component.translatable(key).getString();
    }

    public static String format(long value) {
        if (value < 1000) {
            return Long.toString(value);
        }
        double amount = value;
        int unit = 0;
        String[] units = {"k", "M", "B", "T", "Q"};
        while (amount >= 1000 && unit < units.length - 1) {
            amount /= 1000;
            unit++;
        }
        if (amount >= 100) {
            return String.format(Locale.ROOT, "%.0f%s", amount, units[unit]);
        } else if (amount >= 10) {
            return String.format(Locale.ROOT, "%.1f%s", amount, units[unit]);
        }
        return String.format(Locale.ROOT, "%.2f%s", amount, units[unit]);
    }
}
