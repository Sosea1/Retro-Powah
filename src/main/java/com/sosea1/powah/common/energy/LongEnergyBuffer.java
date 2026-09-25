package com.sosea1.powah.common.energy;

/**
 * Long-backed energy storage used internally by every machine/network.
 * Forge 1.12 exposes int-based FE; conversion is intentionally isolated in ForgeEnergyAdapter.
 */
public final class LongEnergyBuffer {
    private long energy;
    private long capacity;
    private long maxReceive;
    private long maxExtract;

    public LongEnergyBuffer(long capacity, long maxReceive, long maxExtract) {
        setLimits(capacity, maxReceive, maxExtract);
    }

    public long receive(long amount, boolean simulate) {
        requireAmount(amount);
        long accepted = Math.min(amount, Math.min(maxReceive, capacity - energy));
        if (!simulate) {
            energy += accepted;
        }
        return accepted;
    }

    public long extract(long amount, boolean simulate) {
        requireAmount(amount);
        long extracted = Math.min(amount, Math.min(maxExtract, energy));
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    /** Internal generation path; ignores receive transfer rate but never capacity. */
    public long generate(long amount, boolean simulate) {
        requireAmount(amount);
        long accepted = Math.min(amount, capacity - energy);
        if (!simulate) {
            energy += accepted;
        }
        return accepted;
    }

    /** Internal consumption path; ignores extract transfer rate. */
    public long consume(long amount, boolean simulate) {
        requireAmount(amount);
        long extracted = Math.min(amount, energy);
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    public void setEnergy(long energy) {
        if (energy < 0L) {
            throw new IllegalArgumentException("energy must be non-negative");
        }
        this.energy = Math.min(energy, capacity);
    }

    public void setLimits(long capacity, long maxReceive, long maxExtract) {
        if (capacity < 0L || maxReceive < 0L || maxExtract < 0L) {
            throw new IllegalArgumentException("Energy limits must be non-negative");
        }
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        if (energy > capacity) {
            energy = capacity;
        }
    }

    public long energy() { return energy; }
    public long capacity() { return capacity; }
    public long maxReceive() { return maxReceive; }
    public long maxExtract() { return maxExtract; }
    public long space() { return capacity - energy; }
    public boolean isEmpty() { return energy == 0L; }
    public boolean isFull() { return energy >= capacity; }

    private static void requireAmount(long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
    }
}
