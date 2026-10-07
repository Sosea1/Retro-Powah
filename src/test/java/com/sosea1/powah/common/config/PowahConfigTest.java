package com.sosea1.powah.common.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.io.File;
import java.lang.reflect.Field;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.sosea1.powah.common.tier.PowahTier;
import net.minecraftforge.fml.relauncher.FMLInjectionData;

final class PowahConfigTest {
    @TempDir Path directory;
    private File previousMinecraftHome;

    @BeforeEach
    void initializeForgeConfigPath() throws Exception {
        Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        previousMinecraftHome = (File) home.get(null);
        home.set(null, directory.toFile());
    }

    @AfterEach
    void restoreDefaults() throws Exception {
        PowahConfig.initialize(directory.resolve("defaults.cfg").toFile());
        Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, previousMinecraftHome);
    }

    @Test
    void tierOverridesKeepLongValuesAndApplyGlobalMultipliers() {
        Configuration config = new Configuration(directory.resolve("custom.cfg").toFile());
        config.get("energy.energy_cell.capacity", "nitro", "3000000000", "", Property.Type.INTEGER);
        config.get("energy.reactor.generation", "starter", "42", "", Property.Type.INTEGER);
        config.get("energy.cable.transfer", "starter", "1234", "", Property.Type.INTEGER);
        config.get("energy.ender.channels", "starter", "5", "", Property.Type.INTEGER);
        config.get("energy", "capacityMultiplier", 2.0D);
        config.get("energy", "generationMultiplier", 3.0D);
        config.save();
        PowahConfig.initialize(config.getConfigFile());

        EnergyConfigSnapshot energy = PowahConfig.energy();
        assertEquals(6_000_000_000L, energy.energyCell().capacity().get(PowahTier.NITRO));
        assertEquals(126L, energy.reactor().generation().get(PowahTier.STARTER));
        assertEquals(1_234L, energy.cableTransfer().get(PowahTier.STARTER));
        assertEquals(5L, energy.enderChannels().get(PowahTier.STARTER));
        assertEquals(DefaultEnergyConfig.create().reactor().transfer().get(PowahTier.STARTER),
                energy.reactor().transfer().get(PowahTier.STARTER));
    }

    @Test
    void dimensionWhitelistDoesNotExposeMutableConfiguration() {
        Configuration config = new Configuration(directory.resolve("dimensions.cfg").toFile());
        config.get("worldgen", "allowedDimensions", new int[]{0, 7});
        config.save();
        PowahConfig.initialize(config.getConfigFile());
        int[] exposed = PowahConfig.worldgenDimensions();
        exposed[0] = -1;
        assertArrayEquals(new int[]{0, 7}, PowahConfig.worldgenDimensions());
    }
}
