package com.sosea1.powah.api.energy.endernetwork;

import net.minecraft.item.ItemStack;

/**
 * Adds permanent capacity to an Ender Network channel and may import stored FE.
 */
public interface IEnderExtender {
    long getExtendedCapacity(ItemStack stack);
    long getExtendedEnergy(ItemStack stack);
}
