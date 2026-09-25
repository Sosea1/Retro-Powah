package com.sosea1.powah.common.ender;

/** Mutable long-backed state of one owner-scoped Ender channel. Transfer limits live on endpoints. */
public final class EnderChannelState {
    /** Matches modern Powah Energy.MAX while keeping arithmetic safely below Long.MAX_VALUE. */
    public static final long MAX_CAPACITY = 9_000_000_000_000_000_000L;

    private long energy;
    private long capacity;

    public long energy() { return energy; }
    public long capacity() { return capacity; }

    public long receive(long amount, long transfer, boolean simulate) {
        require(amount);
        require(transfer);
        long accepted = Math.min(amount, Math.min(transfer, capacity - energy));
        if (!simulate) energy += accepted;
        return accepted;
    }

    public long extract(long amount, long transfer, boolean simulate) {
        require(amount);
        require(transfer);
        long extracted = Math.min(amount, Math.min(transfer, energy));
        if (!simulate) energy -= extracted;
        return extracted;
    }

    /**
     * Permanently increases channel capacity and optionally imports stored FE from the extender.
     * Returns false instead of consuming an extender when the modern Powah global energy ceiling
     * would be exceeded.
     */
    public boolean extend(long addedCapacity, long importedEnergy) {
        require(addedCapacity);
        require(importedEnergy);
        if (addedCapacity == 0L || addedCapacity > MAX_CAPACITY || capacity > MAX_CAPACITY - addedCapacity) {
            return false;
        }
        capacity += addedCapacity;
        long room = capacity - energy;
        energy += Math.min(room, importedEnergy);
        return true;
    }

    public void restore(long storedEnergy, long storedCapacity) {
        require(storedEnergy);
        require(storedCapacity);
        capacity = Math.min(storedCapacity, MAX_CAPACITY);
        energy = Math.min(storedEnergy, capacity);
    }

    public boolean canExtend(long addedCapacity) {
        return addedCapacity > 0L && addedCapacity <= MAX_CAPACITY && capacity <= MAX_CAPACITY - addedCapacity;
    }

    private static void require(long value) {
        if (value < 0L) throw new IllegalArgumentException("Ender energy values must be non-negative");
    }
}
