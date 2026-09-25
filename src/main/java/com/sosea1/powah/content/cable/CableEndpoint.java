package com.sosea1.powah.content.cable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** One external FE endpoint together with the cable face that feeds it. */
final class CableEndpoint {
    final BlockPos cablePos;
    final EnumFacing cableSide;
    final BlockPos pos;
    final EnumFacing side;

    CableEndpoint(BlockPos cablePos, EnumFacing cableSide, BlockPos pos, EnumFacing side) {
        this.cablePos = immutable(cablePos);
        this.cableSide = cableSide;
        this.pos = immutable(pos);
        this.side = side;
    }

    private static BlockPos immutable(BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CableEndpoint)) {
            return false;
        }
        CableEndpoint other = (CableEndpoint) object;
        return cablePos.equals(other.cablePos) && cableSide == other.cableSide
                && pos.equals(other.pos) && side == other.side;
    }

    @Override
    public int hashCode() {
        int result = cablePos.hashCode();
        result = 31 * result + cableSide.ordinal();
        result = 31 * result + pos.hashCode();
        result = 31 * result + side.ordinal();
        return result;
    }
}
