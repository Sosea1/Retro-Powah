package com.sosea1.powah.common.energy;

public final class EnergyIntMath {
    private EnergyIntMath() {
    }

    public static int saturatedInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
