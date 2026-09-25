package com.sosea1.powah.common.reactor;

import com.sosea1.powah.common.tier.PowahTier;

/** Pure reactor equations kept independent from Minecraft for deterministic regression tests. */
public final class ReactorMath {
    public static final double MAX_FUEL = 1000.0D;
    public static final double MAX_TEMPERATURE = 1000.0D;

    private ReactorMath() {
    }

    /** Modern Powah's internal heat/penalty factor. */
    public static double heatFactor(double temperature, boolean hasRedstone) {
        double clamped = clamp(temperature, 0.0D, MAX_TEMPERATURE);
        double redstoneFactor = hasRedstone ? 1.4D : 1.0D;
        return (clamped / MAX_TEMPERATURE * 0.98D / 2.0D) * redstoneFactor;
    }

    /** FE/t before flooring into the long-backed energy buffer. */
    public static double production(long tierGeneration, double fuel, double temperature,
                                    boolean hasCarbon, boolean hasRedstone) {
        if (tierGeneration <= 0L || fuel <= 0.0D) {
            return 0.0D;
        }
        double carbonPenalty = hasCarbon ? 1.0D : 1.2D;
        double redstonePenalty = hasRedstone ? 1.0D : 1.4D;
        double efficiency = Math.max(0.0D, 1.0D - heatFactor(temperature, hasRedstone));
        return efficiency * clamp(fuel, 0.0D, MAX_FUEL) / MAX_FUEL
                * tierGeneration / carbonPenalty / redstonePenalty;
    }

    /** Uraninite buffer consumed per generating tick. */
    public static double fuelConsumption(PowahTier tier, double temperature, boolean hasRedstone) {
        if (tier == null) {
            throw new NullPointerException("tier");
        }
        return (1.0D + tier.ordinal() * 0.25D) * heatFactor(temperature, hasRedstone);
    }

    /** Target core temperature after optional liquid+solid coolant division. */
    public static double targetTemperature(int base, int carbon, int redstone,
                                           boolean hasLiquidCoolant, int liquidTemperature,
                                           boolean hasSolidCoolant, int solidTemperature) {
        double target = Math.min(MAX_TEMPERATURE, Math.max(0, base + carbon + redstone));
        if (hasLiquidCoolant) {
            int combinedSolid = hasSolidCoolant ? solidTemperature : 0;
            int divisor = Math.abs((-liquidTemperature) + combinedSolid) + 1;
            target /= Math.max(1, divisor);
        }
        return target;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
