package com.sosea1.powah.content.ender;

import net.minecraft.tileentity.TileEntity;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockEnderCell extends BlockEnderMachine {
    public BlockEnderCell(PowahTier tier) { super(tier); }
    @Override protected TileEntity createPowahTile(PowahTier tier) { return new TileEnderCell(tier); }
}
