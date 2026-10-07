package com.sosea1.powah.compat.crafttweaker;

/** Argument checks that do not require CraftTweaker or a running Minecraft instance. */
final class ScriptValidation {
    private ScriptValidation() {}

    static String resourceName(String name, String defaultNamespace) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Resource name must be non-empty");
        }
        String fullName = name.indexOf(':') < 0 ? defaultNamespace + ':' + name : name;
        if (!fullName.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) {
            throw new IllegalArgumentException("Invalid resource name: " + name);
        }
        return fullName;
    }

    static void positiveAmount(double amount) {
        if (!(amount > 0.0D) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Amount must be finite and > 0");
        }
    }

    static void positiveEnergy(long energy) {
        if (energy <= 0L) {
            throw new IllegalArgumentException("Energy must be > 0");
        }
    }

    static void ingredientCount(int count) {
        if (count < 1 || count > 6) {
            throw new IllegalArgumentException("Energizing recipes require 1..6 ingredients");
        }
    }

    static void heatTemperature(int temperature) {
        if (temperature <= 0) throw new IllegalArgumentException("Heat source temperature must be > 0");
    }

    static void fuelTemperature(int temperature) {
        if (temperature < 0) throw new IllegalArgumentException("Reactor fuel temperature must be >= 0");
    }

    static void solidCoolantTemperature(int temperature) {
        if (temperature >= 2) throw new IllegalArgumentException("Solid coolant temperature must be < 2");
    }

    static void oreName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ore dictionary name must be non-empty");
        }
    }
}
