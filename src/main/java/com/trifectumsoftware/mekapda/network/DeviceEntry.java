package com.trifectumsoftware.mekapda.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record DeviceEntry(String name, ResourceLocation dimension, BlockPos pos, int drives, int driveSlots,
                          long itemCount, long energy, long fluid, long chemical) {
}
