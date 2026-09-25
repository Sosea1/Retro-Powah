package com.sosea1.powah.content.magmator;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import com.sosea1.powah.common.block.BlockHorizontalPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockMagmator extends BlockHorizontalPowahMachine {
    public static final PropertyBool LIT = PropertyBool.create("lit");

    public BlockMagmator(PowahTier tier) {
        super(tier, 3.5F, 8.0F);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(LIT, Boolean.FALSE));
    }

    @Override
    protected TileEntity createPowahTile(PowahTier tier) {
        return new TileMagmator(tier);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LIT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(LIT, Boolean.valueOf((meta & 4) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT).booleanValue() ? 4 : 0);
    }

    @Override
    public boolean keepsEnergyOnBreak() {
        return true;
    }

    @Override
    protected void writePortableState(ItemStack stack, TileEntity tile) {
        super.writePortableState(stack, tile);
        if (tile instanceof TileMagmator) {
            writePortableFluid(stack, ((TileMagmator) tile).getTank());
        }
    }

    @Override
    protected void readPortableState(ItemStack stack, TileEntity tile) {
        super.readPortableState(stack, tile);
        if (tile instanceof TileMagmator) {
            readPortableFluid(stack, ((TileMagmator) tile).getTank());
        }
    }
}
