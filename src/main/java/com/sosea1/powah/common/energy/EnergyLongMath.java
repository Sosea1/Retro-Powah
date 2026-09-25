package com.sosea1.powah.common.energy;

/** Overflow-safe helpers for configuration-derived FE values. */
public final class EnergyLongMath {
    private EnergyLongMath() {
    }

    public static long saturatedAdd(long left, long right) {
        if (left < 0L || right < 0L) {
            throw new IllegalArgumentException("Energy values must be non-negative");
        }
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    public static long saturatedMultiply(long left, long right) {
        if (left < 0L || right < 0L) {
            throw new IllegalArgumentException("Energy values must be non-negative");
        }
        if (left == 0L || right == 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }
}
