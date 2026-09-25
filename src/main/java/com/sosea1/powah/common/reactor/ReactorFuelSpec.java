package com.sosea1.powah.common.reactor;

public final class ReactorFuelSpec {
    private final double fuelAmount;
    private final int temperature;

    public ReactorFuelSpec(double fuelAmount, int temperature) {
        if (!(fuelAmount > 0.0D)) {
            throw new IllegalArgumentException("fuelAmount must be > 0");
        }
        if (temperature < 0) {
            throw new IllegalArgumentException("temperature must be >= 0");
        }
        this.fuelAmount = fuelAmount;
        this.temperature = temperature;
    }

    public double fuelAmount() { return fuelAmount; }
    public int temperature() { return temperature; }
}
