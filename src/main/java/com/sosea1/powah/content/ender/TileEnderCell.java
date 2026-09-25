package com.sosea1.powah.content.ender;

import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;

public final class TileEnderCell extends AbstractEnderTile {
    public TileEnderCell() { this(PowahTier.STARTER); }
    public TileEnderCell(PowahTier tier) { super(tier); }
    @Override protected long networkTransfer(PowahTier tier) { return Powah.energyConfig().enderCellTransfer().get(tier); }
    @Override protected boolean supportsCapacityExtender() { return true; }
}
