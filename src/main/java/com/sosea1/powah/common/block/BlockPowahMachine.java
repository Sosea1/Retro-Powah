package com.sosea1.powah.common.block;

import com.sosea1.powah.registry.PowahCreativeTab;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatList;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.material.ItemWrench;
import com.sosea1.powah.content.cable.BlockCable;

/** Common interaction shell for GUI-capable tiered Powah machines. */
public abstract class BlockPowahMachine extends Block {
    public static final String NBT_PORTABLE_ENERGY = "PowahEnergy";
    protected static final String NBT_PORTABLE_FLUID = "PowahFluid";

    private final PowahTier tier;

    @SuppressWarnings("this-escape")
    protected BlockPowahMachine(PowahTier tier, float hardness, float resistance) {
        super(Material.IRON);
        if (tier == null) {
            throw new NullPointerException("tier");
        }
        this.tier = tier;
        setHardness(hardness);
        setResistance(resistance);
        setCreativeTab(PowahCreativeTab.INSTANCE);
    }

    public final PowahTier getTier() {
        return tier;
    }

    @Override
    public final boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public final TileEntity createTileEntity(World world, IBlockState state) {
        return createPowahTile(tier);
    }

    protected abstract TileEntity createPowahTile(PowahTier tier);

    /**
     * Placement/survival hook used by fixed-facing endpoints such as Ender Gates and
     * Energizing Rods. Modern Powah requires those blocks to point at an energy
     * connector; ordinary machines remain unconstrained.
     */
    public boolean canSurviveAt(World world, BlockPos pos, IBlockState state) {
        return true;
    }

    /**
     * Forge-1.12 equivalent of modern Powah's IEnergyConnector/energy capability
     * support test. Cable blocks count even when a face is disabled, matching the
     * upstream connector contract; other blocks must expose FE on the touching side.
     */
    protected final boolean hasEnergySupport(World world, BlockPos pos, EnumFacing side) {
        if (world == null || pos == null || side == null) return false;
        BlockPos supportPos = pos.offset(side);
        // Never destroy a valid attachment merely because the adjacent chunk has not
        // been loaded yet. A later neighbor update re-validates it once available.
        if (!world.isBlockLoaded(supportPos)) return true;
        IBlockState supportState = world.getBlockState(supportPos);
        if (supportState.getBlock().isSideSolid(supportState, world, supportPos, side.getOpposite())) return true;
        if (supportState.getBlock() instanceof BlockCable) return true;
        TileEntity support = world.getTileEntity(supportPos);
        EnumFacing touchingSide = side.getOpposite();
        return support != null
                && support.hasCapability(CapabilityEnergy.ENERGY, touchingSide)
                && support.getCapability(CapabilityEnergy.ENERGY, touchingSide) != null;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (!world.isRemote && !canSurviveAt(world, pos, state)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!world.isRemote && !canSurviveAt(world, pos, state)) {
            world.destroyBlock(pos, true);
            return;
        }
        super.neighborChanged(state, world, pos, blockIn, fromPos);
    }

    /** Modern Powah blocks opt into retaining their long FE buffer in item form. */
    public boolean keepsEnergyOnBreak() {
        return false;
    }

    /** Hook for portable state beyond FE (for example Magmator/Thermo coolant). */
    protected void writePortableState(ItemStack stack, TileEntity tile) {
        if (keepsEnergyOnBreak() && tile instanceof AbstractEnergyTile) {
            NBTTagCompound tag = getOrCreateTag(stack);
            tag.setLong(NBT_PORTABLE_ENERGY, ((AbstractEnergyTile) tile).getEnergyBuffer().energy());
        }
    }

    /** Counterpart to {@link #writePortableState(ItemStack, TileEntity)}. */
    protected void readPortableState(ItemStack stack, TileEntity tile) {
        if (!keepsEnergyOnBreak() || !(tile instanceof AbstractEnergyTile)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(NBT_PORTABLE_ENERGY)) {
            ((AbstractEnergyTile) tile).getEnergyBuffer().setEnergy(Math.max(0L, tag.getLong(NBT_PORTABLE_ENERGY)));
            tile.markDirty();
        }
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             TileEntity tile, ItemStack tool) {
        // In 1.12 removedByPlayer may already have replaced the block with air by
        // harvestBlock, so getDrops(pos) can no longer see the tile's portable data.
        if (!keepsEnergyOnBreak() || tile == null) {
            super.harvestBlock(world, player, pos, state, tile, tool);
            return;
        }
        Item item = Item.getItemFromBlock(this);
        if (item != null && item != Items.AIR) {
            ItemStack drop = new ItemStack(item);
            writePortableState(drop, tile);
            spawnAsEntity(world, pos, drop);
        }
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
    }

    protected static void writePortableFluid(ItemStack stack, FluidTank tank) {
        if (tank == null) return;
        FluidStack fluid = tank.getFluid();
        NBTTagCompound root = getOrCreateTag(stack);
        if (fluid == null || fluid.amount <= 0) {
            root.removeTag(NBT_PORTABLE_FLUID);
            return;
        }
        root.setTag(NBT_PORTABLE_FLUID, fluid.writeToNBT(new NBTTagCompound()));
    }

    protected static void readPortableFluid(ItemStack stack, FluidTank tank) {
        if (tank == null) return;
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey(NBT_PORTABLE_FLUID)) return;
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(root.getCompoundTag(NBT_PORTABLE_FLUID));
        if (fluid == null || fluid.amount <= 0) return;
        tank.drain(Integer.MAX_VALUE, true);
        tank.fill(fluid, true);
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    public static long getPortableEnergy(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0L : Math.max(0L, tag.getLong(NBT_PORTABLE_ENERGY));
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        int firstNewDrop = drops.size();
        super.getDrops(drops, world, pos, state, fortune);
        if (!keepsEnergyOnBreak()) return;

        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) return;
        Item blockItem = Item.getItemFromBlock(this);
        if (blockItem == null || blockItem == Items.AIR) return;

        for (int i = firstNewDrop; i < drops.size(); i++) {
            ItemStack stack = drops.get(i);
            if (!stack.isEmpty() && stack.getItem() == blockItem) {
                writePortableState(stack, tile);
                break;
            }
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!keepsEnergyOnBreak()) return;
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null) readPortableState(stack, tile);
    }

    public static void dropItemHandlerContents(World world, BlockPos pos, TileEntity tile) {
        if (world == null || world.isRemote || tile == null
                || !tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return;
        }
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler == null) return;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack != null && !stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        dropItemHandlerContents(world, pos, world.getTileEntity(pos));
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof AbstractEnergyTile ? ((AbstractEnergyTile) tile).getComparatorPower() : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (ItemWrench.isWrench(held)) {
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile != null && tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing)) {
            IFluidHandler handler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing);
            if (handler != null && FluidUtil.interactWithFluidHandler(player, hand, handler)) {
                return true;
            }
        }
        if (world.isRemote) {
            return true;
        }
        player.openGui(Powah.INSTANCE, Powah.GUI_MACHINE, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}
