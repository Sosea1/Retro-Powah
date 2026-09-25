package com.sosea1.powah.content.cable;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.material.ItemWrench;

/** Cached-network cable with render-only actual-state connections; the block itself never ticks. */
public final class BlockCable extends Block {
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    private static final double CORE_MIN = 6.25D / 16.0D;
    private static final double CORE_MAX = 9.75D / 16.0D;

    private final PowahTier tier;

    public BlockCable(PowahTier tier) {
        super(Material.IRON);
        this.tier = tier;
        setHardness(1.0F);
        setResistance(3.0F);
        setCreativeTab(PowahCreativeTab.INSTANCE);
        setDefaultState(blockState.getBaseState()
                .withProperty(NORTH, Boolean.FALSE)
                .withProperty(EAST, Boolean.FALSE)
                .withProperty(SOUTH, Boolean.FALSE)
                .withProperty(WEST, Boolean.FALSE)
                .withProperty(UP, Boolean.FALSE)
                .withProperty(DOWN, Boolean.FALSE));
    }

    public PowahTier getTier() {
        return tier;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCable(tier);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** Connection booleans are derived state only; they consume no 1.12 metadata bits. */
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state
                .withProperty(NORTH, Boolean.valueOf(canAttach(world, pos, EnumFacing.NORTH)))
                .withProperty(EAST, Boolean.valueOf(canAttach(world, pos, EnumFacing.EAST)))
                .withProperty(SOUTH, Boolean.valueOf(canAttach(world, pos, EnumFacing.SOUTH)))
                .withProperty(WEST, Boolean.valueOf(canAttach(world, pos, EnumFacing.WEST)))
                .withProperty(UP, Boolean.valueOf(canAttach(world, pos, EnumFacing.UP)))
                .withProperty(DOWN, Boolean.valueOf(canAttach(world, pos, EnumFacing.DOWN)));
    }

    private boolean canAttach(IBlockAccess world, BlockPos pos, EnumFacing direction) {
        BlockPos targetPos = pos.offset(direction);
        IBlockState targetState = world.getBlockState(targetPos);
        if (targetState.getBlock() == this) {
            return true;
        }

        TileEntity target = world.getTileEntity(targetPos);
        if (target == null || target instanceof TileCable) {
            return false;
        }
        EnumFacing side = direction.getOpposite();
        if (!target.hasCapability(CapabilityEnergy.ENERGY, side)) {
            return false;
        }
        IEnergyStorage storage = target.getCapability(CapabilityEnergy.ENERGY, side);
        return storage != null;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        IBlockState actual = getActualState(state, source, pos);
        double minX = actual.getValue(WEST).booleanValue() ? 0.0D : CORE_MIN;
        double maxX = actual.getValue(EAST).booleanValue() ? 1.0D : CORE_MAX;
        double minY = actual.getValue(DOWN).booleanValue() ? 0.0D : CORE_MIN;
        double maxY = actual.getValue(UP).booleanValue() ? 1.0D : CORE_MAX;
        double minZ = actual.getValue(NORTH).booleanValue() ? 0.0D : CORE_MIN;
        double maxZ = actual.getValue(SOUTH).booleanValue() ? 1.0D : CORE_MAX;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        // No physical collision: players and mobs walk through cables freely
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    /** Opens side configuration only when the clicked cable arm reaches an external FE endpoint. */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (ItemWrench.isWrench(player.getHeldItem(hand))) return true;
        TileEntity raw = world.getTileEntity(pos);
        if (!(raw instanceof TileCable)) return false;
        TileCable cable = (TileCable) raw;

        EnumFacing hitSide = null;
        if (facing != null && cable.hasExternalEnergySide(facing)) {
            hitSide = facing;
        } else {
            EnumFacing rayHit = getHitSide(hitX, hitY, hitZ);
            if (rayHit != null && cable.hasExternalEnergySide(rayHit)) {
                hitSide = rayHit;
            } else {
                EnumFacing singleSide = null;
                int externalCount = 0;
                for (EnumFacing f : EnumFacing.values()) {
                    if (cable.hasExternalEnergySide(f)) {
                        externalCount++;
                        singleSide = f;
                    }
                }
                if (externalCount == 1) {
                    hitSide = singleSide;
                } else if (externalCount > 1) {
                    double dx = hitX - 0.5D;
                    double dy = hitY - 0.5D;
                    double dz = hitZ - 0.5D;
                    double bestDot = -100.0D;
                    for (EnumFacing f : EnumFacing.values()) {
                        if (cable.hasExternalEnergySide(f)) {
                            double dot = dx * f.getXOffset() + dy * f.getYOffset() + dz * f.getZOffset();
                            if (dot > bestDot) {
                                bestDot = dot;
                                hitSide = f;
                            }
                        }
                    }
                }
            }
        }

        if (hitSide == null || !cable.hasExternalEnergySide(hitSide)) return false;
        if (!world.isRemote) {
            player.openGui(Powah.INSTANCE, Powah.GUI_CABLE_BASE + hitSide.ordinal(), world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    public static EnumFacing getHitSide(float hitX, float hitY, float hitZ) {
        if (hitX > 0.0F && hitX < 0.4F) return EnumFacing.WEST;
        if (hitX > 0.6F && hitX < 1.0F) return EnumFacing.EAST;
        if (hitZ > 0.0F && hitZ < 0.4F) return EnumFacing.NORTH;
        if (hitZ > 0.6F && hitZ < 1.0F) return EnumFacing.SOUTH;
        if (hitY > 0.6F && hitY < 1.0F) return EnumFacing.UP;
        if (hitY > 0.0F && hitY < 0.4F) return EnumFacing.DOWN;
        return null;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }


    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileCable) {
            ((TileCable) tile).onNeighborChanged();
        }
        // Actual-state properties are render-only. This schedules a visual rebuild only
        // when topology changes; there is still no cable tick/update scan.
        world.markBlockRangeForRenderUpdate(pos, pos);
        super.neighborChanged(state, world, pos, blockIn, fromPos);
    }
}
