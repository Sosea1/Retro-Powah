package com.sosea1.powah.compat.crafttweaker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

final class ScriptValidationTest {
    @Test
    void resourceNamesUseTheRequestedNamespaceAndRejectMalformedIds() {
        assertEquals("powah:recipes/example", ScriptValidation.resourceName("recipes/example", "powah"));
        assertEquals("other:example", ScriptValidation.resourceName("other:example", "powah"));
        for (String invalid : new String[] {null, "", " ", "Example", ":example", "example:", "a:b:c", "a b", "a?"}) {
            assertThrows(IllegalArgumentException.class, () -> ScriptValidation.resourceName(invalid, "powah"));
        }
    }

    @Test
    void fuelAndCoolantAmountsRejectNonfiniteOrNonpositiveValues() {
        assertDoesNotThrow(() -> ScriptValidation.positiveAmount(12.0D));
        assertDoesNotThrow(() -> ScriptValidation.positiveAmount(Double.MIN_VALUE));
        for (double invalid : new double[] {0.0D, -1.0D, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> ScriptValidation.positiveAmount(invalid));
        }
    }

    @Test
    void energizingRecipesRequireBetweenOneAndSixInputs() {
        assertDoesNotThrow(() -> ScriptValidation.ingredientCount(1));
        assertDoesNotThrow(() -> ScriptValidation.ingredientCount(6));
        for (int invalid : new int[] {-1, 0, 7}) {
            assertThrows(IllegalArgumentException.class, () -> ScriptValidation.ingredientCount(invalid));
        }
    }

    @Test
    void energyValuesMustBePositiveWithoutTruncatingLongValues() {
        assertDoesNotThrow(() -> ScriptValidation.positiveEnergy(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> ScriptValidation.positiveEnergy(0L));
        assertThrows(IllegalArgumentException.class, () -> ScriptValidation.positiveEnergy(-1L));
    }

    @Test
    void temperaturesRespectMachineRegistryBounds() {
        assertDoesNotThrow(() -> ScriptValidation.heatTemperature(1));
        assertThrows(IllegalArgumentException.class, () -> ScriptValidation.heatTemperature(0));
        assertDoesNotThrow(() -> ScriptValidation.fuelTemperature(0));
        assertThrows(IllegalArgumentException.class, () -> ScriptValidation.fuelTemperature(-1));
        assertDoesNotThrow(() -> ScriptValidation.solidCoolantTemperature(1));
        assertThrows(IllegalArgumentException.class, () -> ScriptValidation.solidCoolantTemperature(2));
    }

    @Test
    void oreNamesCannotBeNullOrBlank() {
        assertDoesNotThrow(() -> ScriptValidation.oreName("ingotIron"));
        for (String invalid : new String[] {null, "", " \t"}) {
            assertThrows(IllegalArgumentException.class, () -> ScriptValidation.oreName(invalid));
        }
    }
}
