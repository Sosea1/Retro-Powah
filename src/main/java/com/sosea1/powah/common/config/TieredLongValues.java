package com.sosea1.powah.common.config;

import java.util.Arrays;
import com.sosea1.powah.common.tier.PowahTier;

/** Immutable values for the seven non-creative Powah tiers. */
public final class TieredLongValues {
    private static final int NORMAL_TIER_COUNT = 7;
    private final long[] values;

    public TieredLongValues(long starter, long basic, long hardened, long blazing,
                            long niotic, long spirited, long nitro) {
        this.values = new long[]{starter, basic, hardened, blazing, niotic, spirited, nitro};
        for (long value : values) {
            if (value < 0L) {
                throw new IllegalArgumentException("Tier values must be non-negative");
            }
        }
    }

    public long get(PowahTier tier) {
        if (tier == null) {
            throw new NullPointerException("tier");
        }
        if (!tier.isNormal()) {
            throw new IllegalArgumentException("Creative tier does not have a finite numeric value");
        }
        return values[tier.index()];
    }

    public TieredLongValues multiply(long factor) {
        if (factor < 0L) {
            throw new IllegalArgumentException("factor must be non-negative");
        }
        long[] result = new long[NORMAL_TIER_COUNT];
        for (int i = 0; i < NORMAL_TIER_COUNT; i++) {
            result[i] = Math.multiplyExact(values[i], factor);
        }
        return new TieredLongValues(result[0], result[1], result[2], result[3], result[4], result[5], result[6]);
    }

    public long[] copyValues() {
        return Arrays.copyOf(values, values.length);
    }
}
