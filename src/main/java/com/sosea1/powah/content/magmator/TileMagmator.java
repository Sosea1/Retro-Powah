package com.sosea1.powah.content.magmator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidTank;
import com.sosea1.powah.Powah;
import com.sosea1.powah.api.PowahApi;
import com.sosea1.powah.common.block.entity.AbstractChargeableGeneratorTile;
import com.sosea1.powah.common.energy.EnergyLongMath;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Fluid generator following modern Powah's 100 mB fuel-buffer model. */
public final class TileMagmator extends AbstractChargeableGeneratorTile implements ITickable {
    public static final int TANK_CAPACITY = 4_000;
    private static final int DRAIN_BATCH = 100;
    private static final String NBT_TANK = "PowahTank";
    private static final String NBT_FUEL_ENERGY = "PowahFluidEnergy";
    private static final String NBT_BURNING = "PowahBurning";

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            TileMagmator.this.markDirty();
            TileMagmator.this.fluidSyncPending = true;
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && PowahApi.isMagmaticFluid(fluid.getFluid());
        }
    };
    private long fuelEnergy;
    private boolean burning;
    private int burningTicks;
    private boolean fluidSyncPending;
    private int fluidSyncCooldown;
    private int lastSyncedFluidLevel = -1;
    private Fluid lastSyncedFluid;

    public TileMagmator() {
        this(PowahTier.STARTER);
    }

    public TileMagmator(PowahTier tier) {
        super(tier, capacity(tier), transfer(tier), EnergyPortMode.OUTPUT, EnergyPortMode.OUTPUT);
    }

    private static long capacity(PowahTier tier) {
        return Powah.energyConfig().magmator().capacity().get(tier);
    }

    private static long transfer(PowahTier tier) {
        return Powah.energyConfig().magmator().transfer().get(tier);
    }

    private static long generation(PowahTier tier) {
        return Powah.energyConfig().magmator().generation().get(tier);
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
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        long produced = 0L;
        if (isOperationAllowed()) {
            if (fuelEnergy <= 0L) {
                loadFluidBatch();
            }

            if (fuelEnergy > 0L && !getEnergyBuffer().isFull()) {
                long request = Math.min(fuelEnergy, generation(getTier()));
                // Wait until the buffer has room for a full generation quantum.
                if (getEnergyBuffer().capacity() - getEnergyBuffer().energy() >= request) {
                    produced = getEnergyBuffer().generate(request, false);
                    fuelEnergy -= produced;
                    if (produced > 0L) {
                        burningTicks = 5;
                        onEnergyChanged();
                    }
                }
            }
        }

        boolean nowBurning = burningTicks > 0;
        if (burningTicks > 0) {
            burningTicks--;
        }
        if (burning != nowBurning) {
            burning = nowBurning;
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockMagmator
                    && state.getValue(BlockMagmator.LIT).booleanValue() != nowBurning) {
                world.setBlockState(pos, state.withProperty(BlockMagmator.LIT, Boolean.valueOf(nowBurning)), 2);
            }
            markDirty();
        }
        chargeStoredItem(transfer(getTier()));
        pushEnergyToAdjacent(transfer(getTier()));

        if (fluidSyncPending) {
            if (++fluidSyncCooldown >= 5) {
                fluidSyncCooldown = 0;
                fluidSyncPending = false;
                FluidStack visibleFluid = tank.getFluid();
                Fluid fluid = visibleFluid == null ? null : visibleFluid.getFluid();
                int level = visibleFluid == null || tank.getCapacity() <= 0
                        ? 0 : Math.min(16, (visibleFluid.amount * 16 + tank.getCapacity() - 1) / tank.getCapacity());
                if (fluid != lastSyncedFluid || level != lastSyncedFluidLevel) {
                    lastSyncedFluid = fluid;
                    lastSyncedFluidLevel = level;
                    syncClientState();
                }
            }
        } else {
            fluidSyncCooldown = 0;
        }
    }

    private void loadFluidBatch() {
        FluidStack fluid = tank.getFluid();
        if (fluid == null || fluid.amount <= 0) {
            return;
        }
        long energyPer100 = PowahApi.getMagmaticFluidEnergyPer100Mb(fluid.getFluid());
        if (energyPer100 <= 0L) {
            return;
        }
        int amount = Math.min(DRAIN_BATCH, fluid.amount);
        long batchEnergy = EnergyLongMath.saturatedMultiply(energyPer100, amount) / DRAIN_BATCH;
        if (batchEnergy <= 0L) {
            return;
        }
        FluidStack drained = tank.drain(amount, true);
        if (drained != null && drained.amount > 0) {
            fuelEnergy = batchEnergy;
            markDirty();
        }
    }

    public FluidTank getTank() {
        return tank;
    }

    public long getFuelEnergy() {
        return fuelEnergy;
    }

    public boolean isBurning() {
        return burning;
    }

    @Override
    public long getGuiAuxValue() {
        return fuelEnergy;
    }

    @Override
    public int getGuiFlags() {
        return burning ? 1 : 0;
    }

    @Override
    public int getGuiFluidAmount() {
        return tank.getFluidAmount();
    }

    @Override
    public int getGuiFluidCapacity() {
        return tank.getCapacity();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast((IFluidHandler) tank);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    protected void writeGeneratorData(NBTTagCompound compound) {
        compound.setTag(NBT_TANK, tank.writeToNBT(new NBTTagCompound()));
        compound.setLong(NBT_FUEL_ENERGY, fuelEnergy);
        compound.setBoolean(NBT_BURNING, burning);
    }

    @Override
    protected void readGeneratorData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_TANK)) {
            tank.readFromNBT(compound.getCompoundTag(NBT_TANK));
        }
        fuelEnergy = Math.max(0L, compound.getLong(NBT_FUEL_ENERGY));
        burning = compound.getBoolean(NBT_BURNING);
    }
}
