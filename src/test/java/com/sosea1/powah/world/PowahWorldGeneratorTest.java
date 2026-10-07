package com.sosea1.powah.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class PowahWorldGeneratorTest {
    @BeforeAll
    static void initializeRegistries() {
        Bootstrap.register();
    }

    @Test
    void additionalSurfaceDimensionRequiresExplicitAllowance() throws Exception {
        assertTrue(canGenerate(new TestProvider(0, true, false), new int[] {0}));
        assertFalse(canGenerate(new TestProvider(7, true, false), new int[] {0}));
        assertTrue(canGenerate(new TestProvider(7, true, false), new int[] {0, 7}));
        assertFalse(canGenerate(new TestProvider(0, true, false), new int[0]));
    }

    @Test
    void allowingADimensionDoesNotMakeNetherOrNonsurfaceWorldsEligible() throws Exception {
        assertFalse(canGenerate(new TestProvider(-1, false, true), new int[] {-1}));
        assertFalse(canGenerate(new TestProvider(1, false, false), new int[] {1}));
        assertFalse(canGenerate(new TestProvider(8, true, true), new int[] {8}));
    }

    @Test
    void netherEndAndVoidBiomesRemainExcludedInsideAllowedSurfaceWorlds() throws Exception {
        assertTrue(isOverworldBiome(Biomes.PLAINS));
        assertFalse(isOverworldBiome(Biomes.HELL));
        assertFalse(isOverworldBiome(Biomes.SKY));
        assertFalse(isOverworldBiome(Biomes.VOID));
    }

    @Test
    void dryIceAcceptsColdAndSnowyTagsAboveTheTemperatureThreshold() throws Exception {
        Biome cold = biome("cold", 0.5F, BiomeDictionary.Type.COLD);
        Biome snowy = biome("snowy", 0.5F, BiomeDictionary.Type.SNOWY);
        assertTrue(isDryIceBiome(cold));
        assertTrue(isDryIceBiome(snowy));
    }

    @Test
    void dryIceKeepsTemperatureFallbackWithoutAdmittingWarmOrEndBiomes() throws Exception {
        assertTrue(isDryIceBiome(biome("cold_temperature", 0.1F, BiomeDictionary.Type.PLAINS)));
        assertFalse(isDryIceBiome(biome("warm_temperature", 0.5F, BiomeDictionary.Type.PLAINS)));
        assertFalse(isDryIceBiome(Biomes.SKY));
    }

    private static boolean canGenerate(WorldProvider provider, int[] dimensions) throws Exception {
        return invoke("canGenerateIn", new Class<?>[] {WorldProvider.class, int[].class}, provider, dimensions);
    }

    private static boolean isOverworldBiome(Biome biome) throws Exception {
        return invoke("isOverworldBiome", new Class<?>[] {Biome.class}, biome);
    }

    private static boolean isDryIceBiome(Biome biome) throws Exception {
        return invoke("isDryIceBiome", new Class<?>[] {Biome.class, BlockPos.class}, biome, new BlockPos(8, 32, 8));
    }

    private static boolean invoke(String name, Class<?>[] parameters, Object... arguments) throws Exception {
        Method method;
        try {
            method = PowahWorldGenerator.class.getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException missing) {
            fail("World generation routing has not been implemented: " + name, missing);
            return false;
        }
        method.setAccessible(true);
        return (Boolean) method.invoke(null, arguments);
    }

    private static Biome biome(String name, float temperature, BiomeDictionary.Type type) {
        Biome biome = new TestBiome(name, temperature);
        biome.setRegistryName(new ResourceLocation("minecraft", "powah_worldgen_test_" + name));
        ForgeRegistries.BIOMES.register(biome);
        BiomeDictionary.addTypes(biome, type);
        return biome;
    }

    private static final class TestBiome extends Biome {
        TestBiome(String name, float temperature) {
            super(new BiomeProperties(name).setTemperature(temperature));
        }
    }

    private static final class TestProvider extends WorldProvider {
        private final boolean surface;

        TestProvider(int dimension, boolean surface, boolean nether) {
            setDimension(dimension);
            this.surface = surface;
            this.nether = nether;
        }

        @Override
        public boolean isSurfaceWorld() {
            return surface;
        }

        @Override
        public DimensionType getDimensionType() {
            return DimensionType.OVERWORLD;
        }
    }
}
