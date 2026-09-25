package com.sosea1.powah.common.config;

/** Immutable energy settings consumed by gameplay code. */
public final class EnergyConfigSnapshot {
    private final long energyPerFuelTick;
    private final double energizingRatio;

    private final Profile furnator;
    private final Profile magmator;
    private final Profile reactor;
    private final Profile solarPanel;
    private final Profile thermoGenerator;

    private final Profile battery;
    private final Profile energyCell;
    private final Profile discharger;
    private final Profile energyHopper;
    private final Profile playerTransmitter;
    private final Profile energizingRod;

    private final TieredLongValues cableTransfer;
    private final TieredLongValues enderCellTransfer;
    private final TieredLongValues enderGateTransfer;
    private final TieredLongValues hopperCharging;
    private final TieredLongValues playerTransmitterCharging;
    private final TieredLongValues enderChannels;

    public EnergyConfigSnapshot(
            long energyPerFuelTick,
            double energizingRatio,
            Profile furnator,
            Profile magmator,
            Profile reactor,
            Profile solarPanel,
            Profile thermoGenerator,
            Profile battery,
            Profile energyCell,
            Profile discharger,
            Profile energyHopper,
            Profile playerTransmitter,
            Profile energizingRod,
            TieredLongValues cableTransfer,
            TieredLongValues enderCellTransfer,
            TieredLongValues enderGateTransfer,
            TieredLongValues hopperCharging,
            TieredLongValues playerTransmitterCharging,
            TieredLongValues enderChannels) {
        if (energyPerFuelTick < 0L) throw new IllegalArgumentException("energyPerFuelTick must be non-negative");
        if (!(energizingRatio > 0.0D) || Double.isNaN(energizingRatio) || Double.isInfinite(energizingRatio)) {
            throw new IllegalArgumentException("energizingRatio must be finite and > 0");
        }
        this.energyPerFuelTick = energyPerFuelTick;
        this.energizingRatio = energizingRatio;
        this.furnator = required(furnator, "furnator");
        this.magmator = required(magmator, "magmator");
        this.reactor = required(reactor, "reactor");
        this.solarPanel = required(solarPanel, "solarPanel");
        this.thermoGenerator = required(thermoGenerator, "thermoGenerator");
        this.battery = required(battery, "battery");
        this.energyCell = required(energyCell, "energyCell");
        this.discharger = required(discharger, "discharger");
        this.energyHopper = required(energyHopper, "energyHopper");
        this.playerTransmitter = required(playerTransmitter, "playerTransmitter");
        this.energizingRod = required(energizingRod, "energizingRod");
        this.cableTransfer = required(cableTransfer, "cableTransfer");
        this.enderCellTransfer = required(enderCellTransfer, "enderCellTransfer");
        this.enderGateTransfer = required(enderGateTransfer, "enderGateTransfer");
        this.hopperCharging = required(hopperCharging, "hopperCharging");
        this.playerTransmitterCharging = required(playerTransmitterCharging, "playerTransmitterCharging");
        this.enderChannels = required(enderChannels, "enderChannels");
    }

    private static <T> T required(T value, String name) {
        if (value == null) throw new NullPointerException(name);
        return value;
    }

    public long energyPerFuelTick() { return energyPerFuelTick; }
    public double energizingRatio() { return energizingRatio; }
    public Profile furnator() { return furnator; }
    public Profile magmator() { return magmator; }
    public Profile reactor() { return reactor; }
    public Profile solarPanel() { return solarPanel; }
    public Profile thermoGenerator() { return thermoGenerator; }
    public Profile battery() { return battery; }
    public Profile energyCell() { return energyCell; }
    public Profile discharger() { return discharger; }
    public Profile energyHopper() { return energyHopper; }
    public Profile playerTransmitter() { return playerTransmitter; }
    public Profile energizingRod() { return energizingRod; }
    public TieredLongValues cableTransfer() { return cableTransfer; }
    public TieredLongValues enderCellTransfer() { return enderCellTransfer; }
    public TieredLongValues enderGateTransfer() { return enderGateTransfer; }
    public TieredLongValues hopperCharging() { return hopperCharging; }
    public TieredLongValues playerTransmitterCharging() { return playerTransmitterCharging; }
    public TieredLongValues enderChannels() { return enderChannels; }

    /** Immutable per-tier energy profile for one Powah machine/device family. */
    public static final class Profile {
        private static final TieredLongValues ZERO = new TieredLongValues(0L, 0L, 0L, 0L, 0L, 0L, 0L);

        private final TieredLongValues capacity;
        private final TieredLongValues transfer;
        private final TieredLongValues generation;

        public Profile(TieredLongValues capacity, TieredLongValues transfer, TieredLongValues generation) {
            this.capacity = EnergyConfigSnapshot.required(capacity, "capacity");
            this.transfer = EnergyConfigSnapshot.required(transfer, "transfer");
            this.generation = EnergyConfigSnapshot.required(generation, "generation");
        }

        public static Profile storage(TieredLongValues capacity, TieredLongValues transfer) {
            return new Profile(capacity, transfer, ZERO);
        }

        public TieredLongValues capacity() { return capacity; }
        public TieredLongValues transfer() { return transfer; }
        public TieredLongValues generation() { return generation; }
    }
}
