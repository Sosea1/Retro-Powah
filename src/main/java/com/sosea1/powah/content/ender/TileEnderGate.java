package com.sosea1.powah.content.ender;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;

public final class TileEnderGate extends AbstractEnderTile {
    private static final String NBT_FACING = "EnderFacing";
    // Legacy-NBT fallback only. The live blockstate is the authoritative facing.
    private EnumFacing legacyFacing = EnumFacing.NORTH;

    public TileEnderGate() { this(PowahTier.STARTER); }
    public TileEnderGate(PowahTier tier) { super(tier); }

    public EnumFacing getFacing() {
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockEnderGate && state.getPropertyKeys().contains(BlockEnderGate.FACING)) {
                return state.getValue(BlockEnderGate.FACING);
            }
        }
        return legacyFacing;
    }

    /** Retained for old placement/migration paths; runtime energy routing reads the blockstate directly. */
    public void setFacing(EnumFacing facing) {
        if (facing != null && legacyFacing != facing) {
            legacyFacing = facing;
            markDirty();
        }
    }

    @Override protected long networkTransfer(PowahTier tier) { return Powah.energyConfig().enderGateTransfer().get(tier); }
    @Override protected boolean supportsCapacityExtender() { return false; }
    @Override protected boolean isEnergySide(EnumFacing side) { return side != null && side == getFacing(); }

    @Override protected void writeEnderData(NBTTagCompound compound) { compound.setByte(NBT_FACING, (byte) getFacing().getIndex()); }
    @Override protected void readEnderData(NBTTagCompound compound) {
        int index = compound.getByte(NBT_FACING) & 0xFF;
        if (index < EnumFacing.values().length) legacyFacing = EnumFacing.values()[index];
    }
}
