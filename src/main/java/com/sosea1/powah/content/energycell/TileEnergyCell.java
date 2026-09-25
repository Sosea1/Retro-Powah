package com.sosea1.powah.content.energycell;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.energy.EnergyItemHelper;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Tiered FE storage with sided I/O, adjacent auto-output and two item-charging slots. */
public final class TileEnergyCell extends AbstractEnergyTile implements ITickable {
    public static final int CHARGE_SLOT_0 = 0;
    public static final int CHARGE_SLOT_1 = 1;
    private static final String NBT_INVENTORY = "PowahInventory";

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return (slot == CHARGE_SLOT_0 || slot == CHARGE_SLOT_1) && EnergyItemHelper.canReceiveEnergy(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileEnergyCell.this.markDirty();
        }
    };

    public TileEnergyCell() {
        this(PowahTier.STARTER);
    }

    public TileEnergyCell(PowahTier tier) {
        super(tier, capacity(tier), transfer(tier), transfer(tier), EnergyPortMode.BOTH, EnergyPortMode.BOTH);
    }

    private static long capacity(PowahTier tier) {
        return tier.isCreative() ? Long.MAX_VALUE : Powah.energyConfig().energyCell().capacity().get(tier);
    }

    private static long transfer(PowahTier tier) {
        return tier.isCreative() ? Long.MAX_VALUE : Powah.energyConfig().energyCell().transfer().get(tier);
    }

    @Override
    protected long getCapacityForTier(PowahTier tier) {
        return capacity(tier);
    }

    @Override
    protected long getMaxReceiveForTier(PowahTier tier) {
        return transfer(tier);
    }

    @Override
    protected long getMaxExtractForTier(PowahTier tier) {
        return transfer(tier);
    }

    @Override
    protected boolean isEnergyPortActive(EnumFacing side) {
        return isOperationAllowed();
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        if (getTier().isCreative()) {
            getEnergyBuffer().setEnergy(getEnergyBuffer().capacity());
        }

        // Each charging slot is limited by the tier transfer rate.
        chargeSlot(CHARGE_SLOT_0);
        chargeSlot(CHARGE_SLOT_1);
        if (isOperationAllowed()) {
            pushEnergyToAdjacent(getEnergyBuffer().maxExtract());
        }
    }

    private void chargeSlot(int slot) {
        long moved = EnergyItemHelper.charge(inventory.getStackInSlot(slot), getEnergyBuffer(), transfer(getTier()));
        if (moved > 0L) {
            onEnergyChanged();
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public long getStoredEnergy() { return getEnergyBuffer().energy(); }

    public void setStoredEnergy(long energy) {
        getEnergyBuffer().setEnergy(getTier().isCreative() ? Long.MAX_VALUE : Math.max(0L, energy));
        onEnergyChanged();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    protected void writePowahData(NBTTagCompound compound) {
        compound.setTag(NBT_INVENTORY, inventory.serializeNBT());
    }

    @Override
    protected void readPowahData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_INVENTORY)) {
            inventory.deserializeNBT(compound.getCompoundTag(NBT_INVENTORY));
        }
    }
}
