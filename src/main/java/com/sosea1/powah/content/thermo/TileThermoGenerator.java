package com.sosea1.powah.content.thermo;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidTank;
import com.sosea1.powah.Powah;
import com.sosea1.powah.api.PowahApi;
import com.sosea1.powah.common.block.entity.AbstractChargeableGeneratorTile;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Passive heat/coolant generator following modern Powah's heatRatio * coolantRatio model. */
public final class TileThermoGenerator extends AbstractChargeableGeneratorTile implements ITickable {
    public static final int TANK_CAPACITY = 4_000;
    private static final String NBT_TANK = "PowahTank";
    private static final String NBT_GENERATING = "PowahGenerating";

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            TileThermoGenerator.this.markDirty();
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && PowahApi.isCoolant(fluid.getFluid());
        }
    };
    private long generating;

    public TileThermoGenerator() {
        this(PowahTier.STARTER);
    }

    public TileThermoGenerator(PowahTier tier) {
        super(tier, capacity(tier), transfer(tier), EnergyPortMode.OUTPUT, EnergyPortMode.OUTPUT);
    }

    private static long capacity(PowahTier tier) {
        return Powah.energyConfig().thermoGenerator().capacity().get(tier);
    }

    private static long transfer(PowahTier tier) {
        return Powah.energyConfig().thermoGenerator().transfer().get(tier);
    }

    private static long generation(PowahTier tier) {
        return Powah.energyConfig().thermoGenerator().generation().get(tier);
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
        // Export first so generation can use capacity freed during this tick.
        chargeStoredItem(transfer(getTier()));
        pushEnergyToAdjacent(transfer(getTier()));

        long nowGenerating = isOperationAllowed() && !getEnergyBuffer().isFull() ? calculateGeneration() : 0L;
        if (generating != nowGenerating) {
            generating = nowGenerating;
            markDirty();
        }

        if (generating > 0L && !getEnergyBuffer().isFull()) {
            long produced = getEnergyBuffer().generate(generating, false);
            if (produced > 0L) {
                onEnergyChanged();
                if (world.getTotalWorldTime() % 40L == 0L) {
                    tank.drain(1, true);
                    markDirty();
                }
            }
        }
    }

    private long calculateGeneration() {
        FluidStack coolant = tank.getFluid();
        if (coolant == null || coolant.amount <= 0 || !PowahApi.isCoolant(coolant.getFluid())) {
            return 0L;
        }
        Block heatBlock = world.getBlockState(pos.down()).getBlock();
        int heat = PowahApi.getHeatSourceTemperature(heatBlock);
        if (heat <= 0) {
            return 0L;
        }

        int coolantTemperature = PowahApi.getCoolantTemperature(coolant.getFluid());
        double heatRatio = heat / 1000.0D;
        double coolantRatio = Math.max(1.0D, -coolantTemperature / 2.0D);
        double value = heatRatio * coolantRatio * generation(getTier());
        if (value <= 0.0D) {
            return 0L;
        }
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) value;
    }

    public FluidTank getTank() {
        return tank;
    }

    public long getGenerating() {
        return generating;
    }

    @Override
    public long getGuiAuxValue() {
        return generating;
    }

    @Override
    public int getGuiFlags() {
        return generating > 0L ? 1 : 0;
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
        compound.setLong(NBT_GENERATING, generating);
    }

    @Override
    protected void readGeneratorData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_TANK)) {
            tank.readFromNBT(compound.getCompoundTag(NBT_TANK));
        }
        generating = Math.max(0L, compound.getLong(NBT_GENERATING));
    }
}
