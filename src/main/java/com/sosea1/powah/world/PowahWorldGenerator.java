package com.sosea1.powah.world;

import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;
import com.sosea1.powah.registry.ModContent;
import com.sosea1.powah.common.config.PowahConfig;

public final class PowahWorldGenerator implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0) return;
        generateOre(world, random, chunkX, chunkZ, ModContent.uraniniteOrePoor().getDefaultState(), PowahConfig.poorVein(), PowahConfig.poorAttempts(), PowahConfig.poorMinY(), PowahConfig.poorMaxY());
        generateOre(world, random, chunkX, chunkZ, ModContent.uraniniteOre().getDefaultState(), PowahConfig.normalVein(), PowahConfig.normalAttempts(), PowahConfig.normalMinY(), PowahConfig.normalMaxY());
        // Modern dense Uraninite targets the negative-Y floor. 1.12 has no negative Y, so use the bottom 8 blocks.
        generateOre(world, random, chunkX, chunkZ, ModContent.uraniniteOreDense().getDefaultState(), PowahConfig.denseVein(), PowahConfig.denseAttempts(), PowahConfig.denseMinY(), PowahConfig.denseMaxY());

        BlockPos center = new BlockPos((chunkX << 4) + 8, 32, (chunkZ << 4) + 8);
        if (world.getBiome(center).getTemperature(center) <= (float) PowahConfig.dryTemperature()) {
            generateOre(world, random, chunkX, chunkZ, ModContent.dryIce().getDefaultState(), PowahConfig.dryVein(), PowahConfig.dryAttempts(), PowahConfig.dryMinY(), PowahConfig.dryMaxY());
        }
    }

    private static void generateOre(World world, Random random, int chunkX, int chunkZ,
                                    net.minecraft.block.state.IBlockState state, int veinSize,
                                    int attempts, int minY, int maxY) {
        int span = Math.max(1, maxY - minY + 1);
        WorldGenMinable generator = new WorldGenMinable(state, veinSize);
        for (int i = 0; i < attempts; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int y = minY + random.nextInt(span);
            int z = (chunkZ << 4) + random.nextInt(16);
            generator.generate(world, random, new BlockPos(x, y, z));
        }
    }
}
