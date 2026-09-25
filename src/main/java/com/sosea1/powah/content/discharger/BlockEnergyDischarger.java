package com.sosea1.powah.content.discharger;

import net.minecraft.tileentity.TileEntity;
import com.sosea1.powah.common.block.BlockHorizontalPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

/** Tiered FE item discharger: extracts item energy into a machine buffer and exposes it outward. */
public final class BlockEnergyDischarger extends BlockHorizontalPowahMachine {
    public BlockEnergyDischarger(PowahTier tier) {
        super(tier, 4.0F, 10.0F);
    }

    @Override
    protected TileEntity createPowahTile(PowahTier tier) {
        return new TileEnergyDischarger(tier);
    }

    @Override
    public boolean keepsEnergyOnBreak() {
        return true;
    }
}
