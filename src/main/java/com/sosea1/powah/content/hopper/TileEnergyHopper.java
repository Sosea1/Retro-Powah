package com.sosea1.powah.content.hopper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.energy.EnergyItemHelper;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Charges every FE-capable item in the inventory directly in front of the hopper. */
public final class TileEnergyHopper extends AbstractEnergyTile implements ITickable {
    private static final String NBT_FACING = "HopperFacing";
    // Legacy-NBT fallback only. The live blockstate is the authoritative facing.
    private EnumFacing legacyFacing = EnumFacing.DOWN;
    private int cooldown;
    private long lastMoved;

    public TileEnergyHopper() { this(PowahTier.STARTER); }
    public TileEnergyHopper(PowahTier tier) {
        super(tier, capacity(tier), transfer(tier), transfer(tier), EnergyPortMode.INPUT, EnergyPortMode.INPUT);
    }
    private static long capacity(PowahTier tier) { return Powah.energyConfig().energyHopper().capacity().get(tier); }
    private static long transfer(PowahTier tier) { return Powah.energyConfig().energyHopper().transfer().get(tier); }
    private static long charging(PowahTier tier) { return Powah.energyConfig().hopperCharging().get(tier); }
    @Override protected long getCapacityForTier(PowahTier tier) { return capacity(tier); }
    @Override protected long getMaxReceiveForTier(PowahTier tier) { return transfer(tier); }
    @Override protected long getMaxExtractForTier(PowahTier tier) { return transfer(tier); }

    public EnumFacing getFacing() {
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockEnergyHopper && state.getPropertyKeys().contains(BlockEnergyHopper.FACING)) {
                return state.getValue(BlockEnergyHopper.FACING);
            }
        }
        return legacyFacing;
    }

    /** Retained for old placement/migration paths; runtime logic reads the blockstate directly. */
    public void setFacing(EnumFacing facing) {
        if (facing != null && legacyFacing != facing) {
            legacyFacing = facing;
            markDirty();
        }
    }

    @Override public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return mode == EnergyPortMode.NONE || mode == EnergyPortMode.INPUT;
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (!isOperationAllowed()) { lastMoved = 0L; cooldown = 19; return; }
        if (cooldown-- > 0) return;
        long moved = chargeFacingInventory();
        cooldown = moved > 0L ? 0 : 19;
        if (lastMoved != moved) { lastMoved = moved; markDirty(); }
    }

    public long chargeFacingInventory() {
        if (getEnergyBuffer().isEmpty()) return 0L;
        EnumFacing facing = getFacing();
        BlockPos targetPos = pos.offset(facing);
        if (!world.isBlockLoaded(targetPos)) return 0L;
        TileEntity target = world.getTileEntity(targetPos);
        if (target == null) return 0L;
        long perStack = charging(getTier());
        long total = 0L;
        if (target.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
            IItemHandler handler = target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
            if (handler != null) {
                for (int slot = 0; slot < handler.getSlots() && !getEnergyBuffer().isEmpty(); slot++) {
                    total += EnergyItemHelper.charge(handler.getStackInSlot(slot), getEnergyBuffer(), perStack);
                }
                if (total > 0L) {
                    target.markDirty();
                    onEnergyChanged();
                }
                return total;
            }
        }
        if (target instanceof IInventory) {
            IInventory inventory = (IInventory) target;
            for (int slot = 0; slot < inventory.getSizeInventory() && !getEnergyBuffer().isEmpty(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                total += EnergyItemHelper.charge(stack, getEnergyBuffer(), perStack);
            }
            if (total > 0L) { inventory.markDirty(); onEnergyChanged(); }
        }
        return total;
    }

    @Override public long getGuiAuxValue() { return lastMoved; }
    @Override protected void writePowahData(NBTTagCompound compound) { compound.setByte(NBT_FACING, (byte) getFacing().getIndex()); }
    @Override protected void readPowahData(NBTTagCompound compound) {
        int index = compound.getByte(NBT_FACING) & 0xFF;
        if (index < EnumFacing.values().length) legacyFacing = EnumFacing.values()[index];
    }
}
