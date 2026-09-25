package com.sosea1.powah.common.config;

public final class DefaultEnergyConfigProvider implements EnergyConfigProvider {
    private final EnergyConfigSnapshot snapshot = DefaultEnergyConfig.create();

    @Override
    public EnergyConfigSnapshot get() {
        return snapshot;
    }
}
