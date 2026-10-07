package com.sosea1.powah.content.material;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

public final class BlockCrystalStorage extends Block {
    public BlockCrystalStorage() {
        super(Material.ROCK);
        setHardness(2.0F);
        setResistance(20.0F * 5.0F / 3.0F);
        setCreativeTab(PowahCreativeTab.INSTANCE);
    }
}
