package com.sosea1.powah.content.solar;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.registry.ModContent;

public final class BlockSolarPanel extends BlockPowahMachine {
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED; }

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    private static final AxisAlignedBB PANEL_SHAPE = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D / 16.0D, 1.0D);

    public BlockSolarPanel(PowahTier tier) {
        super(tier, 3.0F, 6.0F);
        setDefaultState(blockState.getBaseState()
                .withProperty(NORTH, Boolean.TRUE)
                .withProperty(EAST, Boolean.TRUE)
                .withProperty(SOUTH, Boolean.TRUE)
                .withProperty(WEST, Boolean.TRUE));
    }

    @Override
    protected TileEntity createPowahTile(PowahTier tier) {
        return new TileSolarPanel(tier);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                             float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return createConnectedState(world, pos);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(NORTH, Boolean.valueOf((meta & 1) != 0))
                .withProperty(EAST, Boolean.valueOf((meta & 2) != 0))
                .withProperty(SOUTH, Boolean.valueOf((meta & 4) != 0))
                .withProperty(WEST, Boolean.valueOf((meta & 8) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (state.getValue(NORTH).booleanValue()) meta |= 1;
        if (state.getValue(EAST).booleanValue()) meta |= 2;
        if (state.getValue(SOUTH).booleanValue()) meta |= 4;
        if (state.getValue(WEST).booleanValue()) meta |= 8;
        return meta;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        updateConnections(world, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        updateConnections(world, pos, state);
    }

    private void updateConnections(World world, BlockPos pos, IBlockState oldState) {
        if (world.isRemote || world.getBlockState(pos).getBlock() != this) {
            return;
        }
        IBlockState updated = createConnectedState(world, pos);
        if (!updated.equals(oldState)) {
            world.setBlockState(pos, updated, 2);
        }
    }

    private IBlockState createConnectedState(IBlockAccess world, BlockPos pos) {
        return getDefaultState()
                .withProperty(NORTH, Boolean.valueOf(!canAttach(world, pos, EnumFacing.NORTH)))
                .withProperty(EAST, Boolean.valueOf(!canAttach(world, pos, EnumFacing.EAST)))
                .withProperty(SOUTH, Boolean.valueOf(!canAttach(world, pos, EnumFacing.SOUTH)))
                .withProperty(WEST, Boolean.valueOf(!canAttach(world, pos, EnumFacing.WEST)));
    }

    private boolean canAttach(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return world.getBlockState(pos.offset(side)).getBlock() == this;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return PANEL_SHAPE;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity rawTile = world.getTileEntity(pos);
        if (rawTile instanceof TileSolarPanel) {
            TileSolarPanel tile = (TileSolarPanel) rawTile;
            ItemStack held = player.getHeldItem(hand);

            if (!tile.hasLensOfEnder() && !held.isEmpty() && held.getItem() == ModContent.lensOfEnder()) {
                if (!world.isRemote) {
                    tile.setLensOfEnder(true);
                    if (!player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }
                }
                return true;
            }

            if (tile.hasLensOfEnder() && player.isSneaking() && held.isEmpty()) {
                if (!world.isRemote) {
                    tile.setLensOfEnder(false);
                    ItemStack lens = new ItemStack(ModContent.lensOfEnder());
                    if (!player.inventory.addItemStackToInventory(lens)) {
                        spawnAsEntity(world, pos, lens);
                    }
                }
                return true;
            }
        }
        return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity rawTile = world.getTileEntity(pos);
        if (!world.isRemote && rawTile instanceof TileSolarPanel && ((TileSolarPanel) rawTile).hasLensOfEnder()) {
            spawnAsEntity(world, pos, new ItemStack(ModContent.lensOfEnder()));
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean keepsEnergyOnBreak() {
        return true;
    }
}
