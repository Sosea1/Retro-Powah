package com.sosea1.powah.content.reactor;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.IBlockAccess;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockReactor extends BlockPowahMachine {
    public BlockReactor(PowahTier tier) {
        super(tier, 4.0F, 20.0F);
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
    public net.minecraft.block.state.BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, net.minecraft.util.EnumFacing face) {
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
    protected TileEntity createPowahTile(PowahTier tier) {
        return new TileReactor(tier);
    }

    @Override
    public int quantityDropped(Random random) { return 1; }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        TileEntity raw = world.getTileEntity(pos);
        int count = raw instanceof TileReactor && ((TileReactor) raw).isAssemblyRefundable()
                ? ItemReactorBlock.STRUCTURE_BLOCKS : 1;
        drops.add(new ItemStack(Item.getItemFromBlock(this), count));
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             TileEntity tile, ItemStack tool) {
        int count = tile instanceof TileReactor && ((TileReactor) tile).isAssemblyRefundable()
                ? ItemReactorBlock.STRUCTURE_BLOCKS : 1;
        Item item = Item.getItemFromBlock(this);
        if (item != null) spawnAsEntity(world, pos, new ItemStack(item, count));
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity raw = world.getTileEntity(pos);
        if (raw instanceof TileReactor) {
            ((TileReactor) raw).demolish();
        }
        super.breakBlock(world, pos, state);
    }
}
