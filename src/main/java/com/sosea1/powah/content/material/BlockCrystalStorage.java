package com.sosea1.powah.content.material;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

public final class BlockCrystalStorage extends Block {
    public BlockCrystalStorage() {
        super(Material.ROCK);
        setHardness(4.0F);
        setResistance(10.0F);
        setCreativeTab(PowahCreativeTab.INSTANCE);
    }
}
