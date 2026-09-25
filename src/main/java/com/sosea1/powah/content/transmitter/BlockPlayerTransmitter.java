package com.sosea1.powah.content.transmitter;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.material.ItemWrench;

/** Modern Powah-style two-block-tall player transmitter. Only the lower half owns a TileEntity. */
public final class BlockPlayerTransmitter extends Block {
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }

    public static final PropertyBool TOP = PropertyBool.create("top");
    private static boolean breakingPair;
    private final PowahTier tier;

    @SuppressWarnings("this-escape")
    public BlockPlayerTransmitter(PowahTier tier) {
        super(Material.IRON);
        this.tier = tier;
        setHardness(4.0F);
        setResistance(10.0F);
        setCreativeTab(PowahCreativeTab.INSTANCE);
        setDefaultState(blockState.getBaseState().withProperty(TOP, Boolean.FALSE));
    }
    public PowahTier getTier() { return tier; }
    @Override public boolean hasTileEntity(IBlockState state) { return !state.getValue(TOP).booleanValue(); }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return state.getValue(TOP).booleanValue() ? null : new TilePlayerTransmitter(tier); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, TOP); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(TOP, Boolean.valueOf((meta & 1) != 0)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(TOP).booleanValue() ? 1 : 0; }

    @Override public boolean hasComparatorInputOverride(IBlockState state) { return true; }
    @Override public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TilePlayerTransmitter ? ((TilePlayerTransmitter) tile).getComparatorPower() : 0;
    }

    @Override public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && world.isAirBlock(pos.up());
    }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!state.getValue(TOP).booleanValue()) {
            if (world.isAirBlock(pos.up())) {
                world.setBlockState(pos.up(), getDefaultState().withProperty(TOP, Boolean.TRUE), 3);
            }
            TileEntity raw = world.getTileEntity(pos);
            NBTTagCompound tag = stack.getTagCompound();
            if (raw instanceof TilePlayerTransmitter && tag != null && tag.hasKey(BlockPowahMachine.NBT_PORTABLE_ENERGY)) {
                ((TilePlayerTransmitter) raw).getEnergyBuffer().setEnergy(
                        Math.max(0L, tag.getLong(BlockPowahMachine.NBT_PORTABLE_ENERGY)));
                raw.markDirty();
            }
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (state.getValue(TOP).booleanValue()) return;
        Item item = Item.getItemFromBlock(this);
        if (item == null || item == Items.AIR) return;
        ItemStack stack = new ItemStack(item);
        TileEntity raw = world.getTileEntity(pos);
        if (raw instanceof TilePlayerTransmitter) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong(BlockPowahMachine.NBT_PORTABLE_ENERGY,
                    ((TilePlayerTransmitter) raw).getEnergyBuffer().energy());
            stack.setTagCompound(tag);
        }
        drops.add(stack);
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                               EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        BlockPos bottom = state.getValue(TOP).booleanValue() ? pos.down() : pos;
        TileEntity tile = world.getTileEntity(bottom);
        if (ItemWrench.isWrench(player.getHeldItem(hand))) {
            return true;
        }
        if (world.isRemote) return true;
        if (tile instanceof TilePlayerTransmitter) {
            player.openGui(Powah.INSTANCE, Powah.GUI_MACHINE, world, bottom.getX(), bottom.getY(), bottom.getZ());
        }
        return true;
    }
    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (state.getValue(TOP).booleanValue()) {
            BlockPos bottom = pos.down();
            if (world.getBlockState(bottom).getBlock() == this) {
                if (player != null && player.capabilities.isCreativeMode) {
                    world.setBlockToAir(bottom);
                } else {
                    world.destroyBlock(bottom, true);
                }
            }
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!state.getValue(TOP).booleanValue()) {
            BlockPowahMachine.dropItemHandlerContents(world, pos, world.getTileEntity(pos));
        }
        if (!breakingPair) {
            breakingPair = true;
            try {
                BlockPos other = state.getValue(TOP).booleanValue() ? pos.down() : pos.up();
                IBlockState otherState = world.getBlockState(other);
                if (otherState.getBlock() == this) world.setBlockToAir(other);
            } finally {
                breakingPair = false;
            }
        }
        super.breakBlock(world, pos, state);
    }
}
