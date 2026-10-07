package com.sosea1.powah.test;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

public final class EnergyReceiverTile extends TileEntity {
    public final EnergyStorage storage;

    public EnergyReceiverTile() { this(10_000_000); }
    public EnergyReceiverTile(int capacity) { storage = new EnergyStorage(capacity); }

    @Override public boolean hasCapability(Capability<?> capability, EnumFacing side) {
        return capability == CapabilityEnergy.ENERGY;
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        return capability == CapabilityEnergy.ENERGY ? (T) storage : null;
    }
}
