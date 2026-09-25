package com.sosea1.powah.common.energy;

import net.minecraftforge.energy.IEnergyStorage;

/** Adapts long-backed storage to Forge's int-based FE API. */
public final class ForgeEnergyAdapter implements IEnergyStorage {
    private static final Runnable NO_OP = new Runnable() {
        @Override
        public void run() {
        }
    };

    private final LongEnergyBuffer buffer;
    private final EnergyPortModeProvider modeProvider;
    private final Runnable mutationListener;

    public ForgeEnergyAdapter(LongEnergyBuffer buffer, final EnergyPortMode mode) {
        this(buffer, fixedMode(mode), NO_OP);
    }

    public ForgeEnergyAdapter(LongEnergyBuffer buffer, EnergyPortModeProvider modeProvider, Runnable mutationListener) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (modeProvider == null) {
            throw new NullPointerException("modeProvider");
        }
        if (mutationListener == null) {
            throw new NullPointerException("mutationListener");
        }
        this.buffer = buffer;
        this.modeProvider = modeProvider;
        this.mutationListener = mutationListener;
    }

    private static EnergyPortModeProvider fixedMode(final EnergyPortMode mode) {
        if (mode == null) {
            throw new NullPointerException("mode");
        }
        return new EnergyPortModeProvider() {
            @Override
            public EnergyPortMode get() {
                return mode;
            }
        };
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0) {
            return 0;
        }
        int received = (int) buffer.receive(maxReceive, simulate);
        if (!simulate && received > 0) {
            mutationListener.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) {
            return 0;
        }
        int extracted = (int) buffer.extract(maxExtract, simulate);
        if (!simulate && extracted > 0) {
            mutationListener.run();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return EnergyIntMath.saturatedInt(buffer.energy());
    }

    @Override
    public int getMaxEnergyStored() {
        return EnergyIntMath.saturatedInt(buffer.capacity());
    }

    @Override
    public boolean canExtract() {
        EnergyPortMode mode = modeProvider.get();
        return mode != null && mode.canExtract() && buffer.maxExtract() > 0L;
    }

    @Override
    public boolean canReceive() {
        EnergyPortMode mode = modeProvider.get();
        return mode != null && mode.canReceive() && buffer.maxReceive() > 0L;
    }
}
