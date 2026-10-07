package com.sosea1.powah.world;

import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;
import com.sosea1.powah.registry.ModContent;
import com.sosea1.powah.common.config.PowahConfig;

public final class PowahWorldGenerator implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (!canGenerateIn(world.provider, PowahConfig.worldgenDimensions())) return;
        generateOre(world, random, chunkX, chunkZ, ModContent.uraniniteOrePoor().getDefaultState(), PowahConfig.poorVein(), PowahConfig.poorAttempts(), PowahConfig.poorMinY(), PowahConfig.poorMaxY(), false);
        generateOre(world, random, chunkX, chunkZ, ModContent.uraniniteOre().getDefaultState(), PowahConfig.normalVein(), PowahConfig.normalAttempts(), PowahConfig.normalMinY(), PowahConfig.normalMaxY(), false);
        // Modern dense Uraninite targets the negative-Y floor. 1.12 has no negative Y, so use the bottom 8 blocks.
        generateOre(world, random, chunkX, chunkZ, ModContent.uraniniteOreDense().getDefaultState(), PowahConfig.denseVein(), PowahConfig.denseAttempts(), PowahConfig.denseMinY(), PowahConfig.denseMaxY(), false);

        generateOre(world, random, chunkX, chunkZ, ModContent.dryIce().getDefaultState(), PowahConfig.dryVein(), PowahConfig.dryAttempts(), PowahConfig.dryMinY(), PowahConfig.dryMaxY(), true);
    }

    private static boolean canGenerateIn(WorldProvider provider, int[] allowedDimensions) {
        if (!provider.isSurfaceWorld() || provider.isNether()) return false;
        for (int dimension : allowedDimensions) {
            if (dimension == provider.getDimension()) return true;
        }
        return false;
    }

    private static boolean isOverworldBiome(Biome biome) {
        return !BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.END)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.VOID);
    }

    private static boolean isDryIceBiome(Biome biome, BlockPos position) {
        return isOverworldBiome(biome)
                && (BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)
                || biome.getTemperature(position) <= (float) PowahConfig.dryTemperature());
    }

    private static void generateOre(World world, Random random, int chunkX, int chunkZ,
                                    net.minecraft.block.state.IBlockState state, int veinSize,
                                    int attempts, int minY, int maxY, boolean dryIce) {
        int span = Math.max(1, maxY - minY + 1);
        WorldGenMinable generator = new WorldGenMinable(state, veinSize);
        for (int i = 0; i < attempts; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int y = minY + random.nextInt(span);
            int z = (chunkZ << 4) + random.nextInt(16);
            BlockPos position = new BlockPos(x, y, z);
            Biome biome = world.getBiome(position);
            if (dryIce ? isDryIceBiome(biome, position) : isOverworldBiome(biome)) {
                generator.generate(world, random, position);
            }
        }
    }
}
