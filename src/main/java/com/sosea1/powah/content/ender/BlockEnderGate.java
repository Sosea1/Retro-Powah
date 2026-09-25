package com.sosea1.powah.content.ender;

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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockEnderGate extends BlockEnderMachine {
    private static final AxisAlignedBB UP = new AxisAlignedBB(0.375D, 0.96875D, 0.375D, 0.625D, 1.0D, 0.625D);
    private static final AxisAlignedBB DOWN = new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 0.03125D, 0.625D);
    private static final AxisAlignedBB NORTH = new AxisAlignedBB(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 0.03125D);
    private static final AxisAlignedBB SOUTH = new AxisAlignedBB(0.375D, 0.375D, 0.96875D, 0.625D, 0.625D, 1.0D);
    private static final AxisAlignedBB EAST = new AxisAlignedBB(0.96875D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
    private static final AxisAlignedBB WEST = new AxisAlignedBB(0.0D, 0.375D, 0.375D, 0.03125D, 0.625D, 0.625D);

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case UP: return UP;
            case DOWN: return DOWN;
            case SOUTH: return SOUTH;
            case EAST: return EAST;
            case WEST: return WEST;
            case NORTH:
            default: return NORTH;
        }
    }

    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockEnderGate(PowahTier tier) {
        super(tier);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override protected TileEntity createPowahTile(PowahTier tier) { return new TileEnderGate(tier); }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, facing.getOpposite());
    }

    @Override
    public boolean canSurviveAt(World world, BlockPos pos, IBlockState state) {
        return hasEnergySupport(world, pos, state.getValue(FACING));
    }

    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex(); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEnderGate) ((TileEnderGate) tile).setFacing(state.getValue(FACING));
    }
}
