package com.sosea1.powah.content.energizing;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockEnergizingRod extends BlockPowahMachine {
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }

    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockEnergizingRod(PowahTier tier) {
        super(tier, 4.0F, 10.0F);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN));
    }

    @Override
    protected TileEntity createPowahTile(PowahTier tier) {
        return new TileEnergizingRod(tier);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (!world.isRemote) {
            TileEntity raw = world.getTileEntity(pos);
            if (raw instanceof TileEnergizingRod) {
                ((TileEnergizingRod) raw).tryLinkNearestOrb(TileEnergizingRod.configuredRange());
            }
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public boolean canSurviveAt(World world, BlockPos pos, IBlockState state) {
        return hasEnergySupport(world, pos, state.getValue(FACING));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int index = meta % 6;
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(index));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX,
                                             float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, facing.getOpposite());
    }
    @Override
    public boolean keepsEnergyOnBreak() {
        return true;
    }

}
