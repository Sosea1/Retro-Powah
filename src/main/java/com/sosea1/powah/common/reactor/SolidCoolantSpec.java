package com.sosea1.powah.common.reactor;

public final class SolidCoolantSpec {
    private final double amount;
    private final int temperature;

    public SolidCoolantSpec(double amount, int temperature) {
        if (!(amount > 0.0D)) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (temperature >= 2) {
            throw new IllegalArgumentException("reactor solid coolant temperature must be < 2");
        }
        this.amount = amount;
        this.temperature = temperature;
    }

    public double amount() { return amount; }
    public int temperature() { return temperature; }
}
