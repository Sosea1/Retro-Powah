package com.sosea1.powah.content.thermo;

import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockThermoGenerator extends BlockPowahMachine {
    public BlockThermoGenerator(PowahTier tier) {
        super(tier, 3.5F, 8.0F);
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED; }

    @Override
    protected TileEntity createPowahTile(PowahTier tier) {
        return new TileThermoGenerator(tier);
    }
    @Override
    public boolean keepsEnergyOnBreak() {
        return true;
    }

    @Override
    protected void writePortableState(ItemStack stack, TileEntity tile) {
        super.writePortableState(stack, tile);
        if (tile instanceof TileThermoGenerator) {
            writePortableFluid(stack, ((TileThermoGenerator) tile).getTank());
        }
    }

    @Override
    protected void readPortableState(ItemStack stack, TileEntity tile) {
        super.readPortableState(stack, tile);
        if (tile instanceof TileThermoGenerator) {
            readPortableFluid(stack, ((TileThermoGenerator) tile).getTank());
        }
    }

}
