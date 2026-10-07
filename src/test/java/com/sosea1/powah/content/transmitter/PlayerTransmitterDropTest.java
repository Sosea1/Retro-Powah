package com.sosea1.powah.content.transmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.registries.GameData;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.test.EnergyTestWorld;

final class PlayerTransmitterDropTest {
    @Test
    void twoHalvesProduceOnlyOneBlockWithStoredEnergy() {
        EnergyTestWorld world = new EnergyTestWorld();
        BlockPlayerTransmitter block = new BlockPlayerTransmitter(PowahTier.NITRO);
        TilePlayerTransmitter tile = new TilePlayerTransmitter(PowahTier.NITRO);
        BlockPos bottom = new BlockPos(0, 64, 0);
        world.put(bottom, tile);
        tile.getEnergyBuffer().setEnergy(1_999_999_999L);
        Item item = new ItemBlock(block);
        Item previous = GameData.getBlockItemMap().put(block, item);
        try {
            IBlockState top = block.getDefaultState().withProperty(BlockPlayerTransmitter.TOP, true);
            assertTrue(block.hasTileEntity(block.getDefaultState()));
            assertFalse(block.hasTileEntity(top));
            NonNullList<ItemStack> drops = NonNullList.create();
            block.getDrops(drops, world, bottom.up(), top, 0);
            block.getDrops(drops, world, bottom, block.getDefaultState(), 0);
            assertEquals(1, drops.size());
            assertEquals(1, drops.get(0).getCount());
            assertEquals(item, drops.get(0).getItem());
            assertEquals(1_999_999_999L, BlockPowahMachine.getPortableEnergy(drops.get(0)));
        } finally {
            if (previous == null) GameData.getBlockItemMap().remove(block);
            else GameData.getBlockItemMap().put(block, previous);
        }
    }
}
