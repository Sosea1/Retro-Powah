package com.sosea1.powah.content.hopper;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

/** Six-directional item-charging hopper. */
public final class BlockEnergyHopper extends BlockPowahMachine {
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }

    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockEnergyHopper(PowahTier tier) {
        super(tier, 4.0F, 10.0F);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN));
    }
    @Override protected TileEntity createPowahTile(PowahTier tier) { return new TileEnergyHopper(tier); }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                                       int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, facing.getOpposite());
    }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex(); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEnergyHopper) ((TileEnergyHopper) tile).setFacing(state.getValue(FACING));
    }
    @Override
    public boolean keepsEnergyOnBreak() {
        return true;
    }

}
