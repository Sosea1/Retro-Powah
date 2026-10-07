package com.sosea1.powah.content.material;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Solid reactor coolant and cold-biome worldgen block. */
public final class BlockDryIce extends Block {
    public BlockDryIce() {
        super(Material.PACKED_ICE);
        setHardness(2.0F);
        // Match modern strength(..., 8): 1.12 scales resistance by 3/5.
        setResistance(8.0F * 5.0F / 3.0F);
        setHarvestLevel("pickaxe", 0);
        setCreativeTab(PowahCreativeTab.INSTANCE);
        setLightOpacity(3);
    }
}
