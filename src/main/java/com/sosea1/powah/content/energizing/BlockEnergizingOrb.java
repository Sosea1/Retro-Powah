package com.sosea1.powah.content.energizing;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sosea1.powah.content.material.ItemWrench;

/** Physical no-GUI Energizing Orb. Right click inserts one item or removes the next available item. */
public final class BlockEnergizingOrb extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    private static final AxisAlignedBB SHAPE_DOWN = new AxisAlignedBB(2.5D / 16.0D, 0.0D, 2.5D / 16.0D, 13.5D / 16.0D, 14.25D / 16.0D, 13.5D / 16.0D);
    private static final AxisAlignedBB SHAPE_UP = new AxisAlignedBB(2.5D / 16.0D, 1.75D / 16.0D, 2.5D / 16.0D, 13.5D / 16.0D, 1.0D, 13.5D / 16.0D);
    private static final AxisAlignedBB SHAPE_NORTH = new AxisAlignedBB(2.5D / 16.0D, 2.5D / 16.0D, 0.0D, 13.5D / 16.0D, 13.5D / 16.0D, 14.25D / 16.0D);
    private static final AxisAlignedBB SHAPE_SOUTH = new AxisAlignedBB(2.5D / 16.0D, 2.5D / 16.0D, 1.75D / 16.0D, 13.5D / 16.0D, 13.5D / 16.0D, 1.0D);
    private static final AxisAlignedBB SHAPE_WEST = new AxisAlignedBB(0.0D, 2.5D / 16.0D, 2.5D / 16.0D, 14.25D / 16.0D, 13.5D / 16.0D, 13.5D / 16.0D);
    private static final AxisAlignedBB SHAPE_EAST = new AxisAlignedBB(1.75D / 16.0D, 2.5D / 16.0D, 2.5D / 16.0D, 1.0D, 13.5D / 16.0D, 13.5D / 16.0D);

    public BlockEnergizingOrb() {
        super(Material.IRON);
        setHardness(4.0F);
        setResistance(10.0F);
        setCreativeTab(PowahCreativeTab.INSTANCE);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEnergizingOrb();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, facing.getOpposite());
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
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, net.minecraft.world.IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case UP: return SHAPE_UP;
            case NORTH: return SHAPE_NORTH;
            case SOUTH: return SHAPE_SOUTH;
            case WEST: return SHAPE_WEST;
            case EAST: return SHAPE_EAST;
            case DOWN:
            default: return SHAPE_DOWN;
        }
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }


    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (ItemWrench.isWrench(player.getHeldItem(hand))) return true;
        TileEntity raw = world.getTileEntity(pos);
        if (!(raw instanceof TileEnergizingOrb)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }

        TileEnergizingOrb orb = (TileEnergizingOrb) raw;
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || !orb.getInventory().getStackInSlot(TileEnergizingOrb.OUTPUT_SLOT).isEmpty()) {
            ItemStack removed = orb.removeNext();
            if (!removed.isEmpty() && !player.inventory.addItemStackToInventory(removed)) {
                player.dropItem(removed, false);
            }
            return true;
        }

        if (orb.addNextInput(held)) {
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            return true;
        }
        return true;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (!world.isRemote) {
            TileEnergizingRod.linkNearbyUnlinkedRods(world, pos, TileEnergizingRod.configuredRange());
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity raw = world.getTileEntity(pos);
        if (raw instanceof TileEnergizingOrb) {
            TileEnergizingOrb orb = (TileEnergizingOrb) raw;
            for (int slot = 0; slot < orb.getInventory().getSlots(); slot++) {
                ItemStack stack = orb.getInventory().getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                }
            }
        }
        if (!world.isRemote) {
            TileEnergizingRod.unlinkRodsFromOrb(world, pos, TileEnergizingRod.configuredRange());
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos) {
        TileEntity raw = world.getTileEntity(pos);
        if (!(raw instanceof TileEnergizingOrb)) {
            return 0;
        }
        int nonEmpty = 0;
        TileEnergizingOrb orb = (TileEnergizingOrb) raw;
        for (int slot = 0; slot < orb.getInventory().getSlots(); slot++) {
            if (!orb.getInventory().getStackInSlot(slot).isEmpty()) {
                nonEmpty++;
            }
        }
        return Math.min(15, nonEmpty);
    }
}
