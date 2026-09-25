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
                TileReactor core = ((TileReactorPart) raw).getCore();
                if (core != null && core.isBuilt()) {
                    // Destroying the core invokes its normal 36-block refund and
                    // removes every linked shell part without duplicate drops.
                    return BlockProtection.canBreak(player, world, core.getPos())
                            && world.destroyBlock(core.getPos(), true);
                }
            }
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
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
