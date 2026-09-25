package com.sosea1.powah.common.block.entity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import com.sosea1.powah.common.energy.EnergyItemHelper;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Shared one-charge-slot base used by Powah generators that don't need a normal item inventory. */
public abstract class AbstractChargeableGeneratorTile extends AbstractEnergyTile {
    private static final String NBT_CHARGE_INVENTORY = "PowahChargeInventory";

    private final ItemStackHandler chargeInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && EnergyItemHelper.canReceiveEnergy(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            AbstractChargeableGeneratorTile.this.markDirty();
        }
    };

    protected AbstractChargeableGeneratorTile(PowahTier tier, long capacity, long transfer,
                                               EnergyPortMode defaultSideMode, EnergyPortMode unsidedMode) {
        super(tier, capacity, 0L, transfer, defaultSideMode, unsidedMode);
    }

    @Override
    public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return mode == EnergyPortMode.NONE || mode == EnergyPortMode.OUTPUT;
    }

    public final ItemStackHandler getChargeInventory() {
        return chargeInventory;
    }

    protected final long chargeStoredItem(long budget) {
        long moved = EnergyItemHelper.charge(chargeInventory.getStackInSlot(0), getEnergyBuffer(), budget);
        if (moved > 0L) {
            onEnergyChanged();
        }
        return moved;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(chargeInventory);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    protected final void writePowahData(NBTTagCompound compound) {
        compound.setTag(NBT_CHARGE_INVENTORY, chargeInventory.serializeNBT());
        writeGeneratorData(compound);
    }

    @Override
    protected final void readPowahData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_CHARGE_INVENTORY)) {
            chargeInventory.deserializeNBT(compound.getCompoundTag(NBT_CHARGE_INVENTORY));
        }
        readGeneratorData(compound);
    }

    protected void writeGeneratorData(NBTTagCompound compound) {
    }

    protected void readGeneratorData(NBTTagCompound compound) {
    }
}
