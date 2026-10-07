package com.sosea1.powah.content.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class MaterialBlockParityTest {
    @BeforeAll
    static void initializeRegistries() {
        Bootstrap.register();
    }

    @Test
    void miningUraniniteDoesNotAwardExperienceAtAnyFortuneLevel() {
        for (int count : new int[] {1, 2, 4}) {
            BlockUraniniteOre ore = new BlockUraniniteOre(new Item(), count);
            for (int fortune : new int[] {0, 1, 3}) {
                assertEquals(0, ore.getExpDrop(ore.getDefaultState(), null, BlockPos.ORIGIN, fortune));
            }
        }
    }

    @Test
    void uraniumGradesRetainTheirDifferentMiningHardness() {
        Item drop = new Item();
        assertEquals(3.0F, new BlockUraniniteOre(drop, 1).getDefaultState().getBlockHardness(null, BlockPos.ORIGIN));
        assertEquals(3.2F, new BlockUraniniteOre(drop, 2).getDefaultState().getBlockHardness(null, BlockPos.ORIGIN));
        assertEquals(4.0F, new BlockUraniniteOre(drop, 4).getDefaultState().getBlockHardness(null, BlockPos.ORIGIN));
    }

    @Test
    void oreAndDryIceAbsorbTheSameExplosionStrengthAsUpstream() {
        for (int count : new int[] {1, 2, 4}) {
            BlockUraniniteOre ore = new BlockUraniniteOre(new Item(), count);
            assertEquals(8.0F, ore.getExplosionResistance(null, BlockPos.ORIGIN, null, null), 0.0001F);
        }
        BlockDryIce dryIce = new BlockDryIce();
        assertEquals(8.0F, dryIce.getExplosionResistance(null, BlockPos.ORIGIN, null, null), 0.0001F);
        assertEquals(2.0F, dryIce.getDefaultState().getBlockHardness(null, BlockPos.ORIGIN));
    }
}
