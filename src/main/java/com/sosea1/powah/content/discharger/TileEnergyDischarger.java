package com.sosea1.powah.content.discharger;

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

/** Seven-slot Powah discharger, matching modern Powah's per-stack tier transfer semantics. */
public final class TileEnergyDischarger extends AbstractEnergyTile implements ITickable {
    public static final int SLOT_COUNT = 7;
    private static final String NBT_INVENTORY = "PowahInventory";
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return EnergyItemHelper.canExtractEnergy(stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override protected void onContentsChanged(int slot) { TileEnergyDischarger.this.cooldown = 0; TileEnergyDischarger.this.markDirty(); }
    };
    private int cooldown;
    private long lastMoved;

    public TileEnergyDischarger() { this(PowahTier.STARTER); }
    public TileEnergyDischarger(PowahTier tier) {
        super(tier, capacity(tier), 0L, transfer(tier), EnergyPortMode.OUTPUT, EnergyPortMode.OUTPUT);
    }

    private static long capacity(PowahTier tier) { return Powah.energyConfig().discharger().capacity().get(tier); }
    private static long transfer(PowahTier tier) { return Powah.energyConfig().discharger().transfer().get(tier); }
    @Override protected long getCapacityForTier(PowahTier tier) { return capacity(tier); }
    @Override protected long getMaxReceiveForTier(PowahTier tier) { return 0L; }
    @Override protected long getMaxExtractForTier(PowahTier tier) { return transfer(tier); }

    @Override public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return mode == EnergyPortMode.NONE || mode == EnergyPortMode.OUTPUT;
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (cooldown-- > 0) return;
        long discharged = 0L;
        if (isOperationAllowed() && !getEnergyBuffer().isFull()) {
            for (int slot = 0; slot < SLOT_COUNT && !getEnergyBuffer().isFull(); slot++) {
                discharged += EnergyItemHelper.discharge(inventory.getStackInSlot(slot), getEnergyBuffer(), transfer(getTier()));
            }
        }
        if (discharged > 0L) {
            // EnergyItemHelper mutates both the item capability and this raw buffer directly.
            // Persist those mutations even when the amount moved is identical to the previous cycle.
            onEnergyChanged();
        }
        // Redstone gates item discharging, not the output of energy already stored in the machine.
        long moved = discharged + pushEnergyToAdjacent(transfer(getTier()));
        cooldown = moved > 0L ? 0 : 19;
        if (lastMoved != moved) { lastMoved = moved; markDirty(); }
    }

    public ItemStackHandler getInventory() { return inventory; }
    @Override public long getGuiAuxValue() { return lastMoved; }

    @Override public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
    @Override protected void writePowahData(NBTTagCompound compound) {
        compound.setTag(NBT_INVENTORY, inventory.serializeNBT());
    }
    @Override protected void readPowahData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_INVENTORY)) inventory.deserializeNBT(compound.getCompoundTag(NBT_INVENTORY));
    }
}
