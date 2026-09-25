package com.sosea1.powah.content.furnator;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.energy.EnergyItemHelper;
import com.sosea1.powah.common.energy.EnergyLongMath;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Coal/vanilla-fuel generator preserving modern Powah's burnTime * energyPerFuelTick model. */
public final class TileFurnator extends AbstractEnergyTile implements ITickable {
    public static final int CHARGE_SLOT = 0;
    public static final int FUEL_SLOT = 1;

    private static final String NBT_CARBON = "PowahCarbon";
    private static final String NBT_BURNING = "PowahBurning";
    private static final String NBT_INVENTORY = "PowahInventory";

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == FUEL_SLOT) {
                return ForgeEventFactory.getItemBurnTime(stack) > 0;
            }
            if (slot == CHARGE_SLOT) {
                return EnergyItemHelper.canReceiveEnergy(stack);
            }
            return false;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileFurnator.this.markDirty();
        }
    };

    private long carbon;
    private boolean burning;
    private boolean burningSyncPending;
    private int burningSyncCooldown;

    public TileFurnator() {
        this(PowahTier.STARTER);
    }

    public TileFurnator(PowahTier tier) {
        super(tier, capacity(tier), 0L, transfer(tier), EnergyPortMode.OUTPUT, EnergyPortMode.OUTPUT);
    }

    private static long capacity(PowahTier tier) {
        return Powah.energyConfig().furnator().capacity().get(tier);
    }

    private static long transfer(PowahTier tier) {
        return Powah.energyConfig().furnator().transfer().get(tier);
    }

    private static long generation(PowahTier tier) {
        return Powah.energyConfig().furnator().generation().get(tier);
    }

    @Override
    protected long getCapacityForTier(PowahTier tier) {
        return capacity(tier);
    }

    @Override
    protected long getMaxReceiveForTier(PowahTier tier) {
        return 0L;
    }

    @Override
    protected long getMaxExtractForTier(PowahTier tier) {
        return transfer(tier);
    }

    @Override
    public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return mode == EnergyPortMode.NONE || mode == EnergyPortMode.OUTPUT;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        long produced = 0L;
        if (isOperationAllowed()) {
            if (carbon == 0L) {
                tryLoadFuel();
            }

            if (carbon > 0L && !getEnergyBuffer().isFull()) {
                long requested = Math.min(carbon, generation(getTier()));
                produced = getEnergyBuffer().generate(requested, false);
                carbon -= produced;
                if (produced > 0L) {
                    onEnergyChanged();
                }
            }
        }

        boolean nowBurning = produced > 0L;
        if (burning != nowBurning) {
            burning = nowBurning;
            burningSyncPending = true;
            markDirty();
        }

        long charged = EnergyItemHelper.charge(inventory.getStackInSlot(CHARGE_SLOT), getEnergyBuffer(), transfer(getTier()));
        if (charged > 0L) {
            onEnergyChanged();
        }
        pushEnergyToAdjacent(transfer(getTier()));
        if (burningSyncPending) {
            if (++burningSyncCooldown >= 5) {
                burningSyncCooldown = 0;
                burningSyncPending = false;
                syncClientState();
            }
        } else {
            burningSyncCooldown = 0;
        }
    }

    private void tryLoadFuel() {
        ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return;
        }

        int burnTime = ForgeEventFactory.getItemBurnTime(fuel);
        if (burnTime <= 0) {
            return;
        }

        long fuelEnergy = EnergyLongMath.saturatedMultiply(burnTime, Powah.energyConfig().energyPerFuelTick());
        if (fuelEnergy <= 0L) {
            return;
        }

        ItemStack singleFuel = fuel.copy();
        singleFuel.setCount(1);
        ItemStack container = fuel.getItem().hasContainerItem(singleFuel)
                ? fuel.getItem().getContainerItem(singleFuel)
                : ItemStack.EMPTY;

        fuel.shrink(1);
        if (fuel.isEmpty()) {
            inventory.setStackInSlot(FUEL_SLOT, container);
        } else if (!container.isEmpty()) {
            net.minecraft.inventory.InventoryHelper.spawnItemStack(world,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, container);
        }

        carbon = fuelEnergy;
        markDirty();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public long getCarbon() {
        return carbon;
    }

    public boolean isBurning() {
        return burning;
    }

    @Override
    public long getGuiAuxValue() {
        return carbon;
    }

    @Override
    public int getGuiFlags() {
        return burning ? 1 : 0;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
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
        compound.setLong(NBT_CARBON, carbon);
        compound.setBoolean(NBT_BURNING, burning);
        compound.setTag(NBT_INVENTORY, inventory.serializeNBT());
    }

    @Override
    protected void readPowahData(NBTTagCompound compound) {
        carbon = Math.max(0L, compound.getLong(NBT_CARBON));
        burning = compound.getBoolean(NBT_BURNING);
        if (compound.hasKey(NBT_INVENTORY)) {
            inventory.deserializeNBT(compound.getCompoundTag(NBT_INVENTORY));
        }
    }
}
