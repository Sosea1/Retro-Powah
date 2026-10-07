package com.sosea1.powah.common.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

final class ReactorMathTest {
    @Test
    void colderLiquidStrengthensDryIceCooling() {
        double water = ReactorMath.targetTemperature(990, 0, 0, true, 0, true, -32);
        double cold = ReactorMath.targetTemperature(990, 0, 0, true, -1, true, -32);
        double colder = ReactorMath.targetTemperature(990, 0, 0, true, -5, true, -32);

        assertTrue(cold < water);
        assertTrue(colder < cold);
        assertEquals(990.0D / 38.0D, colder, 1.0E-9D);
    }

    @Test
    void waterAndAbsentCoolantsKeepTheirExistingBehavior() {
        assertEquals(30.0D, ReactorMath.targetTemperature(990, 0, 0, true, 0, true, -32));
        assertEquals(990.0D, ReactorMath.targetTemperature(990, 0, 0, false, -5, true, -32));
        assertEquals(165.0D, ReactorMath.targetTemperature(990, 0, 0, true, -5, false, -32));
    }

    @Test
    void extremeCoolantAndFuelTemperaturesDoNotOverflow() {
        double temperature = ReactorMath.targetTemperature(Integer.MAX_VALUE, 180, 160,
                true, Integer.MIN_VALUE, true, Integer.MIN_VALUE);
        assertEquals(1000.0D / 4_294_967_297L, temperature, 1.0E-15D);
        assertTrue(temperature > 0.0D);
    }
}
