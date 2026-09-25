package com.sosea1.powah.content.reactor;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import com.sosea1.powah.common.block.BlockProtection;
import net.minecraft.world.World;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.material.ItemWrench;

/** Hidden shell block created by the reactor core. It intentionally drops nothing. */
public final class BlockReactorPart extends Block {
    private final PowahTier tier;

    public BlockReactorPart(PowahTier tier) {
        super(Material.IRON);
        this.tier = tier;
        setHardness(4.0F);
        setResistance(20.0F);
        setCreativeTab((CreativeTabs) null);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public net.minecraft.util.EnumBlockRenderType getRenderType(IBlockState state) {
        return net.minecraft.util.EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public net.minecraft.block.state.BlockFaceShape getBlockFaceShape(net.minecraft.world.IBlockAccess world, IBlockState state, BlockPos pos, net.minecraft.util.EnumFacing face) {
        return net.minecraft.block.state.BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean addLandingEffects(IBlockState state, net.minecraft.world.WorldServer world, BlockPos pos,
                                     IBlockState landedState, net.minecraft.entity.EntityLivingBase entity, int count) {
        return true;
    }

    @Override
    public boolean addRunningEffects(IBlockState state, World world, BlockPos pos, net.minecraft.entity.Entity entity) {
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) { return true; }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) { return new TileReactorPart(tier); }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Items.AIR; }

    @Override
    public int quantityDropped(Random random) { return 0; }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest) {
        if (!world.isRemote) {
            TileEntity raw = world.getTileEntity(pos);
            if (raw instanceof TileReactorPart) {
                TileReactorPart part = (TileReactorPart) raw;
                BlockPos corePos = part.getCorePos();
                if (part.isCoreBound() && !world.isBlockLoaded(corePos)) {
                    // A player break is cancellable; do not break half of a reactor
                    // while its core is unavailable, and never force-load the chunk.
                    return false;
                }
                TileEntity coreTile = part.isCoreBound() ? world.getTileEntity(corePos) : null;
                if (coreTile instanceof TileReactor) {
                    TileReactor core = (TileReactor) coreTile;
                    // Destroying the core invokes its normal 36-block refund and
                    // removes every linked shell part without duplicate drops.
                    if (!BlockProtection.canBreak(player, world, corePos)) return false;
                    TileReactor.DemolitionResult result = core.tryDemolitionFromPart();
                    if (result == TileReactor.DemolitionResult.COMPLETE) return true;
                    if (result == TileReactor.DemolitionResult.RETRY) return false;
                }
            }
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        // Chunk.setBlockState calls breakBlock while the old tile is still
        // available. Capture the link before super removes that tile.
        TileEntity raw = world.getTileEntity(pos);
        boolean coreBound = raw instanceof TileReactorPart && ((TileReactorPart) raw).isCoreBound();
        BlockPos corePos = coreBound ? ((TileReactorPart) raw).getCorePos().toImmutable() : BlockPos.ORIGIN;
        if (!world.isRemote && coreBound) {
            ReactorPartLifecycle.requestDemolition(world, corePos);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity raw = world.getTileEntity(pos);
        if (raw instanceof TileReactorPart) {
            TileReactor core = ((TileReactorPart) raw).getCore();
            if (ItemWrench.isWrench(player.getHeldItem(hand)) && core != null) {
                return true;
            }
            if (world.isRemote) {
                return true;
            }
            if (core != null) {
                BlockPos corePos = core.getPos();
                player.openGui(Powah.INSTANCE, Powah.GUI_MACHINE, world,
                        corePos.getX(), corePos.getY(), corePos.getZ());
            }
        }
        return true;
    }
}
