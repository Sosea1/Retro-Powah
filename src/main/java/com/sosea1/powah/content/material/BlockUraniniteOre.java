package com.sosea1.powah.content.material;

import com.sosea1.powah.registry.PowahCreativeTab;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

/** Stone-replacing Uraninite ore variant with the classic poor/normal/dense yield split. */
public final class BlockUraniniteOre extends Block {
    private final Item drop;
    private final int baseDropCount;

    public BlockUraniniteOre(Item drop, int baseDropCount) {
        super(Material.ROCK);
        if (drop == null) throw new NullPointerException("drop");
        if (baseDropCount <= 0) throw new IllegalArgumentException("baseDropCount");
        this.drop = drop;
        this.baseDropCount = baseDropCount;
        setHardness(3.0F);
        setResistance(5.0F);
        setHarvestLevel("pickaxe", 2);
        setCreativeTab(PowahCreativeTab.INSTANCE);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return drop;
    }

    @Override
    public int quantityDropped(Random random) {
        return baseDropCount;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (fortune <= 0) return baseDropCount;
        int bonus = random.nextInt(fortune + 2) - 1;
        if (bonus < 0) bonus = 0;
        return baseDropCount * (bonus + 1);
    }

    @Override
    public int getExpDrop(IBlockState state, net.minecraft.world.IBlockAccess world,
                          net.minecraft.util.math.BlockPos pos, int fortune) {
        return RANDOM.nextInt(3) + 2;
    }
}
