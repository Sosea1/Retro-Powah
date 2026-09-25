package com.sosea1.powah.common.config;

/** Baseline values matching Powah 1.21.1 defaults (v6.2.x config layout). */
public final class DefaultEnergyConfig {
    private DefaultEnergyConfig() {}

    public static EnergyConfigSnapshot create() {
        TieredLongValues generatorCapacity = values(20_000L, 80_000L, 200_000L, 800_000L, 2_000_000L, 8_000_000L, 40_000_000L);
        TieredLongValues generatorTransfer = values(80L, 320L, 800L, 3_200L, 8_000L, 32_000L, 160_000L);
        TieredLongValues activeProduction = values(20L, 80L, 200L, 800L, 2_000L, 8_000L, 40_000L);
        TieredLongValues passiveProduction = values(20L, 60L, 100L, 200L, 400L, 800L, 2_000L);

        EnergyConfigSnapshot.Profile furnator = profile(generatorCapacity, generatorTransfer, activeProduction);
        EnergyConfigSnapshot.Profile magmator = profile(generatorCapacity, generatorTransfer, activeProduction);
        EnergyConfigSnapshot.Profile solar = profile(generatorCapacity, generatorTransfer, passiveProduction);
        EnergyConfigSnapshot.Profile thermo = profile(generatorCapacity, generatorTransfer, passiveProduction);

        TieredLongValues reactorCapacity = values(250_000L, 1_000_000L, 2_500_000L, 10_000_000L, 25_000_000L, 100_000_000L, 500_000_000L);
        TieredLongValues reactorTransfer = values(1_000L, 4_000L, 10_000L, 40_000L, 100_000L, 400_000L, 2_000_000L);
        TieredLongValues reactorProduction = values(250L, 1_000L, 2_500L, 10_000L, 25_000L, 100_000L, 500_000L);
        EnergyConfigSnapshot.Profile reactor = profile(reactorCapacity, reactorTransfer, reactorProduction);

        TieredLongValues storageCapacity = values(1_000_000L, 4_000_000L, 10_000_000L, 40_000_000L, 100_000_000L, 400_000_000L, 2_000_000_000L);
        TieredLongValues storageTransfer = values(1_000L, 4_000L, 10_000L, 40_000L, 100_000L, 400_000L, 2_000_000L);
        EnergyConfigSnapshot.Profile storage = EnergyConfigSnapshot.Profile.storage(storageCapacity, storageTransfer);
        EnergyConfigSnapshot.Profile energizingRod = EnergyConfigSnapshot.Profile.storage(
                values(10_000L, 40_000L, 100_000L, 400_000L, 1_000_000L, 4_000_000L, 20_000_000L),
                values(100L, 400L, 1_000L, 4_000L, 10_000L, 40_000L, 200_000L));
        TieredLongValues cableTransfer = values(500L, 2_000L, 5_000L, 20_000L, 50_000L, 200_000L, 1_000_000L);
        TieredLongValues channels = values(1L, 2L, 3L, 5L, 7L, 9L, 12L);

        return new EnergyConfigSnapshot(
                30L, 1.0D,
                furnator, magmator, reactor, solar, thermo,
                storage, storage, storage, storage, storage, energizingRod,
                cableTransfer, storageTransfer, cableTransfer,
                cableTransfer, cableTransfer, channels);
    }

    private static EnergyConfigSnapshot.Profile profile(TieredLongValues capacity, TieredLongValues transfer, TieredLongValues generation) {
        return new EnergyConfigSnapshot.Profile(capacity, transfer, generation);
    }

    private static TieredLongValues values(long starter, long basic, long hardened, long blazing, long niotic, long spirited, long nitro) {
        return new TieredLongValues(starter, basic, hardened, blazing, niotic, spirited, nitro);
    }
}
