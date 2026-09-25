package com.sosea1.powah.content.material;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/** Solid reactor coolant and cold-biome worldgen block. */
public final class BlockDryIce extends Block {
    public BlockDryIce() {
        super(Material.PACKED_ICE);
        setHardness(0.7F);
        setResistance(2.5F);
        setHarvestLevel("pickaxe", 0);
        setCreativeTab(PowahCreativeTab.INSTANCE);
        setLightOpacity(3);
    }
}
