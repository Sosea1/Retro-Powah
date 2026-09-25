package com.sosea1.powah.content.reactor;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;
import com.sosea1.powah.Powah;
import com.sosea1.powah.api.PowahApi;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.energy.EnergyItemHelper;
import com.sosea1.powah.common.energy.EnergyIntMath;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.reactor.ReactorFuelSpec;
import com.sosea1.powah.common.reactor.ReactorMath;
import com.sosea1.powah.common.reactor.SolidCoolantSpec;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.registry.ModContent;

/**
 * 1.12.2 reactor core preserving modern Powah's fuel/heat/efficiency equations.
 * The surrounding 3x4x3 shell is assembled lazily, one part every five ticks.
 */
public final class TileReactor extends AbstractEnergyTile implements ITickable {
    private static final EnumFacing[] FACINGS = EnumFacing.values();
    public static final int CHARGE_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int CARBON_SLOT = 2;
    public static final int REDSTONE_SLOT = 3;
    public static final int SOLID_COOLANT_SLOT = 4;
    public static final int TANK_CAPACITY = 1_000;

    private static final String NBT_INVENTORY = "PowahInventory";
    private static final String NBT_TANK = "PowahTank";
    private static final String NBT_FUEL = "PowahReactorFuel";
    private static final String NBT_CARBON = "PowahReactorCarbon";
    private static final String NBT_REDSTONE = "PowahReactorRedstone";
    private static final String NBT_SOLID = "PowahReactorSolidCoolant";
    private static final String NBT_SOLID_TEMP = "PowahReactorSolidCoolantTemp";
    private static final String NBT_TEMP = "PowahReactorTemperature";
    private static final String NBT_BASE_TEMP = "PowahReactorBaseTemp";
    private static final String NBT_CARBON_TEMP = "PowahReactorCarbonTemp";
    private static final String NBT_REDSTONE_TEMP = "PowahReactorRedstoneTemp";
    private static final String NBT_RUNNING = "PowahReactorRunning";
    private static final String NBT_GEN_MODE = "PowahReactorGenMode";
    private static final String NBT_GENERATE = "PowahReactorGenerate";
    private static final String NBT_BUILT = "PowahReactorBuilt";
    private static final String NBT_BUILD_INDEX = "PowahReactorBuildIndex";
    private static final String NBT_ASSEMBLY_AUTHORIZED = "PowahReactorAssemblyAuthorized";
    private static final String NBT_ASSEMBLY_REFUNDABLE = "PowahReactorAssemblyRefundable";

    private final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            switch (slot) {
                case CHARGE_SLOT:
                    return EnergyItemHelper.canReceiveEnergy(stack);
                case FUEL_SLOT:
                    return PowahApi.getReactorFuel(stack.getItem()) != null;
                case CARBON_SLOT:
                    return isCarbon(stack);
                case REDSTONE_SLOT:
                    return isRedstone(stack);
                case SOLID_COOLANT_SLOT:
                    return isValidSolidCoolant(stack);
                default:
                    return false;
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileReactor.this.markDirty();
        }
    };

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            TileReactor.this.markDirty();
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && PowahApi.isCoolant(fluid.getFluid());
        }
    };

    private double fuel;
    private double carbon;
    private double redstone;
    private double solidCoolant;
    private int solidCoolantTemp;
    private double temperature;
    private int baseTemp;
    private int carbonTemp;
    private int redstoneTemp;
    private long currentProduction;
    private boolean running;
    private boolean genModeOn;
    private boolean generate = true;
    private boolean built;
    private boolean assemblyAuthorized;
    private boolean assemblyRefundable;
    private int buildIndex;
    private int buildCooldown;
    private long ticks;
    private int lastClientVisualState = Integer.MIN_VALUE;

    public TileReactor() {
        this(PowahTier.STARTER);
    }

    public TileReactor(PowahTier tier) {
        super(tier, capacity(tier), 0L, transfer(tier), EnergyPortMode.OUTPUT, EnergyPortMode.OUTPUT);
    }

    private static long capacity(PowahTier tier) {
        return Powah.energyConfig().reactor().capacity().get(tier);
    }

    private static long transfer(PowahTier tier) {
        return Powah.energyConfig().reactor().transfer().get(tier);
    }

    private static long generation(PowahTier tier) {
        return Powah.energyConfig().reactor().generation().get(tier);
    }

    @Override
    protected long getCapacityForTier(PowahTier tier) { return capacity(tier); }

    @Override
    protected long getMaxReceiveForTier(PowahTier tier) { return 0L; }

    @Override
    protected long getMaxExtractForTier(PowahTier tier) { return transfer(tier); }

    @Override
    public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return mode == EnergyPortMode.NONE || mode == EnergyPortMode.OUTPUT;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        ticks++;

        if (!built && assemblyAuthorized) {
            buildStep();
        } else if (ticks % 40L == 0L && !validateStructure()) {
            built = false;
            buildIndex = 0;
            markDirty();
        }

        pushReactorEnergy(transfer(getTier()));
        long charged = EnergyItemHelper.charge(inventory.getStackInSlot(CHARGE_SLOT), getEnergyBuffer(), transfer(getTier()));
        if (charged > 0L) {
            onEnergyChanged();
        }

        boolean nowRunning = false;
        currentProduction = 0L;
        if (built && generate && isOperationAllowed()) {
            // Bootstrap the internal fuel buffer before testing it. Previously this
            // call lived behind fuel > 0, so the first Uraninite could never start
            // an empty reactor.
            processFuel();
            if (fuel > 0.0D) {
                boolean generating = !getEnergyBuffer().isFull();
                processCarbon(generating);
                processRedstone(generating);
                processTemperature(generating);

                if (generating) {
                    double consumption = ReactorMath.fuelConsumption(getTier(), temperature, redstone > 0.0D);
                    fuel = Math.max(0.0D, fuel - Math.min(fuel, consumption));
                    long request = safeFloor(ReactorMath.production(
                            generation(getTier()), fuel, temperature, carbon > 0.0D, redstone > 0.0D));
                    long produced = getEnergyBuffer().generate(request, false);
                    currentProduction = produced;
                    if (produced > 0L) {
                        onEnergyChanged();
                    }
                }
                nowRunning = true;
            }
        }

        if (running != nowRunning) {
            running = nowRunning;
            markDirty();
        }
        checkGenerationMode();
        syncVisualStateIfNeeded();
    }


    private long pushReactorEnergy(long budget) {
        if (budget <= 0L || world == null || world.isRemote || getEnergyBuffer().isEmpty()) {
            return 0L;
        }
        long remaining = Math.min(budget, Math.min(getEnergyBuffer().energy(), getEnergyBuffer().maxExtract()));
        long transferred = 0L;
        int start = (int) (world.getTotalWorldTime() % FACINGS.length);
        for (int i = 0; i < FACINGS.length && remaining > 0L; i++) {
            EnumFacing side = FACINGS[(start + i) % FACINGS.length];
            if (!getSideMode(side).canExtract()) {
                continue;
            }
            int distance = side.getAxis().isHorizontal() ? 2 : side == EnumFacing.UP ? 4 : 1;
            BlockPos targetPos = pos.offset(side, distance);
            if (!world.isBlockLoaded(targetPos)) {
                continue;
            }
            TileEntity target = world.getTileEntity(targetPos);
            if (target == null || !target.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) {
                continue;
            }
            IEnergyStorage storage = target.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (storage == null || !storage.canReceive()) {
                continue;
            }
            int offer = EnergyIntMath.saturatedInt(remaining);
            long extracted = getEnergyBuffer().consume(offer, false);
            if (extracted <= 0L) {
                continue;
            }
            int accepted = Math.max(0, Math.min(EnergyIntMath.saturatedInt(extracted),
                    storage.receiveEnergy(EnergyIntMath.saturatedInt(extracted), false)));
            long delivered = Math.min(extracted, accepted);
            if (extracted > delivered) {
                getEnergyBuffer().generate(extracted - delivered, false);
            }
            transferred += delivered;
            remaining -= delivered;
        }
        if (transferred > 0L) {
            onEnergyChanged();
        }
        return transferred;
    }

    private void processFuel() {
        ItemStack stack = inventory.getStackInSlot(FUEL_SLOT);
        if (!stack.isEmpty()) {
            ReactorFuelSpec spec = PowahApi.getReactorFuel(stack.getItem());
            if (spec != null && (fuel <= 0.0D || fuel + spec.fuelAmount() <= ReactorMath.MAX_FUEL)) {
                fuel = Math.min(ReactorMath.MAX_FUEL, fuel + spec.fuelAmount());
                baseTemp = spec.temperature();
                stack.shrink(1);
                markDirty();
            }
        }
        if (fuel <= 0.0D) {
            baseTemp = 0;
        }
    }

    private void processCarbon(boolean generating) {
        if (carbon <= 0.0D) {
            ItemStack stack = inventory.getStackInSlot(CARBON_SLOT);
            int burn = carbonBurnTime(stack);
            if (burn > 0 && !hasContainerItem(stack)) {
                carbon = burn;
                carbonTemp = 180;
                stack.shrink(1);
                markDirty();
            }
        }
        if (carbon > 0.0D && generating) {
            carbon = Math.max(0.0D, carbon - 1.0D);
        }
        if (carbon <= 0.0D) {
            carbonTemp = 0;
        }
    }

    private void processRedstone(boolean generating) {
        if (redstone <= 0.0D) {
            ItemStack stack = inventory.getStackInSlot(REDSTONE_SLOT);
            if (isRedstoneDust(stack)) {
                redstone = 18.0D;
                redstoneTemp = 120;
                stack.shrink(1);
                markDirty();
            } else if (isRedstoneBlock(stack)) {
                redstone = 162.0D;
                redstoneTemp = 120;
                stack.shrink(1);
                markDirty();
            }
        }
        if (redstone > 0.0D && generating && ticks % 40L == 0L) {
            redstone = Math.max(0.0D, redstone - 1.0D);
        }
        if (redstone <= 0.0D) {
            redstoneTemp = 0;
        }
    }

    private void processTemperature(boolean generating) {
        if (solidCoolant <= 0.0D) {
            ItemStack stack = inventory.getStackInSlot(SOLID_COOLANT_SLOT);
            SolidCoolantSpec spec = stack.isEmpty() ? null : PowahApi.getSolidCoolant(stack.getItem());
            if (isValidSolidCoolant(spec)) {
                solidCoolant = spec.amount();
                solidCoolantTemp = spec.temperature();
                stack.shrink(1);
                markDirty();
            }
        }

        boolean liquid = tank.getFluidAmount() > 0 && tank.getFluid() != null
                && PowahApi.isCoolant(tank.getFluid().getFluid());
        if (solidCoolant > 0.0D && liquid && generating && ticks % 40L == 0L) {
            solidCoolant = Math.max(0.0D, solidCoolant - 1.0D);
        }
        if (solidCoolant <= 0.0D) {
            solidCoolantTemp = 0;
        }

        int liquidTemp = liquid ? PowahApi.getCoolantTemperature(tank.getFluid().getFluid()) : 0;
        double target = ReactorMath.targetTemperature(baseTemp, carbonTemp, redstoneTemp,
                liquid, liquidTemp, solidCoolant > 0.0D, solidCoolantTemp);
        if (temperature < target) {
            temperature = Math.min(target, temperature + 1.0D);
        } else if (temperature > target) {
            long cadence = liquid ? (solidCoolant > 0.0D ? 1L : 3L) : 5L;
            if (ticks % cadence == 0L) {
                temperature = Math.max(target, temperature - 1.0D);
            }
        }
    }

    private void checkGenerationMode() {
        if (!genModeOn) {
            generate = true;
            return;
        }
        if (getEnergyBuffer().isFull()) {
            generate = false;
        } else if (getEnergyBuffer().capacity() > 0L
                && getEnergyBuffer().energy() * 100L / getEnergyBuffer().capacity() <= 70L) {
            generate = true;
        }
    }

    private void buildStep() {
        if (buildCooldown-- > 0) {
            return;
        }
        buildCooldown = 5;
        while (buildIndex < 36) {
            BlockPos target = structurePosition(buildIndex++);
            if (target.equals(pos)) {
                continue;
            }
            if (!world.isBlockLoaded(target)) {
                buildIndex--;
                buildCooldown = 20;
                return;
            }
            TileEntity existingTile = world.getTileEntity(target);
            if (world.getBlockState(target).getBlock() == ModContent.reactorPart(getTier())
                    && existingTile instanceof TileReactorPart) {
                TileReactorPart part = (TileReactorPart) existingTile;
                if (!BlockPos.ORIGIN.equals(part.getCorePos()) && !pos.equals(part.getCorePos())) {
                    buildIndex--;
                    buildCooldown = 20;
                    return;
                }
                part.bind(pos, isExtractorPosition(target));
                return;
            }
            if (!world.getBlockState(target).getBlock().isReplaceable(world, target)) {
                buildIndex--;
                buildCooldown = 20;
                return;
            }
            if (!world.setBlockState(target, ModContent.reactorPart(getTier()).getDefaultState(), 3)) {
                buildIndex--;
                buildCooldown = 20;
                return;
            }
            TileEntity tile = world.getTileEntity(target);
            if (tile instanceof TileReactorPart) {
                ((TileReactorPart) tile).bind(pos, isExtractorPosition(target));
            } else {
                buildIndex--;
                buildCooldown = 20;
                return;
            }
            return;
        }
        built = validateStructure();
        if (!built) {
            buildIndex = 0;
            buildCooldown = 20;
        }
        markDirty();
    }

    private boolean validateStructure() {
        for (int i = 0; i < 36; i++) {
            BlockPos target = structurePosition(i);
            if (target.equals(pos)) {
                continue;
            }
            if (!world.isBlockLoaded(target)) {
                return false;
            }
            if (world.getBlockState(target).getBlock() != ModContent.reactorPart(getTier())) {
                return false;
            }
            TileEntity raw = world.getTileEntity(target);
            if (!(raw instanceof TileReactorPart) || !pos.equals(((TileReactorPart) raw).getCorePos())) {
                return false;
            }
        }
        return true;
    }

    /** Tears down the passive shell without converting stored fuel into a different item. */
    public void demolish() {
        if (world == null || world.isRemote) {
            return;
        }
        demolishParts();
        markDirty();
    }

    private void syncVisualStateIfNeeded() {
        int visualState = (built ? 1 : 0) | (running ? 2 : 0) | (fuel > 0.0D ? 4 : 0);
        if (visualState != lastClientVisualState) {
            lastClientVisualState = visualState;
            syncClientState();
        }
    }

    public void demolishParts() {
        if (world == null || world.isRemote) {
            return;
        }
        for (int i = 0; i < 36; i++) {
            BlockPos target = structurePosition(i);
            if (target.equals(pos)) {
                continue;
            }
            if (!world.isBlockLoaded(target)) {
                continue;
            }
            TileEntity raw = world.getTileEntity(target);
            if (world.getBlockState(target).getBlock() == ModContent.reactorPart(getTier())
                    && raw instanceof TileReactorPart && pos.equals(((TileReactorPart) raw).getCorePos())) {
                world.setBlockToAir(target);
            }
        }
    }

    private BlockPos structurePosition(int index) {
        int y = index / 9;
        int rem = index % 9;
        int x = rem / 3 - 1;
        int z = rem % 3 - 1;
        return pos.add(x, y, z);
    }

    private boolean isExtractorPosition(BlockPos target) {
        return target.equals(pos.up(3)) || target.equals(pos.north()) || target.equals(pos.south())
                || target.equals(pos.east()) || target.equals(pos.west());
    }


    private static boolean isValidSolidCoolant(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isValidSolidCoolant(PowahApi.getSolidCoolant(stack.getItem()));
    }

    private static boolean isValidSolidCoolant(SolidCoolantSpec spec) {
        return spec != null && spec.amount() > 0 && spec.temperature() < 2;
    }

    private static boolean hasContainerItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStack one = stack.copy();
        one.setCount(1);
        Item item = stack.getItem();
        return item.hasContainerItem(one);
    }

    private static boolean isCarbon(ItemStack stack) {
        return carbonBurnTime(stack) > 0 && !hasContainerItem(stack);
    }

    private static int carbonBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int burn = ForgeEventFactory.getItemBurnTime(stack);
        if (burn <= 0) burn = TileEntityFurnace.getItemBurnTime(stack);
        // Cleanroom builds can return zero from the Forge hook for vanilla coal.
        if (burn <= 0 && stack.getItem() == Items.COAL) burn = 1_600;
        return Math.max(0, burn);
    }

    private static boolean isRedstone(ItemStack stack) {
        return isRedstoneDust(stack) || isRedstoneBlock(stack);
    }

    private static boolean isRedstoneDust(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.REDSTONE) return true;
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("dustRedstone".equals(OreDictionary.getOreName(id))) return true;
        }
        return false;
    }

    private static boolean isRedstoneBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK)) return true;
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("blockRedstone".equals(OreDictionary.getOreName(id))) return true;
        }
        return false;
    }

    private static long safeFloor(double value) {
        if (!(value > 0.0D)) {
            return 0L;
        }
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) value;
    }

    public ItemStackHandler getInventory() { return inventory; }
    public FluidTank getTank() { return tank; }
    public double getFuel() { return fuel; }
    public double getCarbon() { return carbon; }
    public double getRedstone() { return redstone; }
    public double getSolidCoolant() { return solidCoolant; }
    public double getTemperature() { return temperature; }
    public long getCurrentProduction() { return currentProduction; }
    public boolean isRunning() { return running; }
    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
                pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 4.0D, pos.getZ() + 2.0D);
    }

    public boolean isBuilt() { return built; }
    public boolean isAssemblyAuthorized() { return assemblyAuthorized; }
    public boolean isAssemblyRefundable() { return assemblyAuthorized && assemblyRefundable; }
    public void authorizeAssembly() { authorizeAssembly(true); }
    public void authorizeAssembly(boolean refundable) {
        boolean changed = !assemblyAuthorized || assemblyRefundable != refundable;
        assemblyAuthorized = true;
        assemblyRefundable = refundable;
        if (changed) markDirty();
    }
    public boolean isGenModeOn() { return genModeOn; }
    public boolean isGeneratingEnabled() { return generate; }

    public void setGenModeOn(boolean enabled) {
        if (genModeOn != enabled) {
            genModeOn = enabled;
            if (!enabled) {
                generate = true;
            }
            markDirty();
        }
    }

    @Override
    public long getGuiAuxValue() { return currentProduction; }

    @Override
    public int getGuiFlags() {
        return (running ? 1 : 0) | (genModeOn ? 2 : 0) | (built ? 4 : 0) | (generate ? 8 : 0);
    }

    @Override
    public int getGuiFluidAmount() { return tank.getFluidAmount(); }

    @Override
    public int getGuiFluidCapacity() { return tank.getCapacity(); }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast((IFluidHandler) tank);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    protected void writePowahData(NBTTagCompound compound) {
        compound.setTag(NBT_INVENTORY, inventory.serializeNBT());
        compound.setTag(NBT_TANK, tank.writeToNBT(new NBTTagCompound()));
        compound.setDouble(NBT_FUEL, fuel);
        compound.setDouble(NBT_CARBON, carbon);
        compound.setDouble(NBT_REDSTONE, redstone);
        compound.setDouble(NBT_SOLID, solidCoolant);
        compound.setInteger(NBT_SOLID_TEMP, solidCoolantTemp);
        compound.setDouble(NBT_TEMP, temperature);
        compound.setInteger(NBT_BASE_TEMP, baseTemp);
        compound.setInteger(NBT_CARBON_TEMP, carbonTemp);
        compound.setInteger(NBT_REDSTONE_TEMP, redstoneTemp);
        compound.setBoolean(NBT_RUNNING, running);
        compound.setBoolean(NBT_GEN_MODE, genModeOn);
        compound.setBoolean(NBT_GENERATE, generate);
        compound.setBoolean(NBT_BUILT, built);
        compound.setBoolean(NBT_ASSEMBLY_AUTHORIZED, assemblyAuthorized);
        compound.setBoolean(NBT_ASSEMBLY_REFUNDABLE, assemblyRefundable);
        compound.setInteger(NBT_BUILD_INDEX, buildIndex);
    }

    @Override
    protected void readPowahData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_INVENTORY)) {
            inventory.deserializeNBT(compound.getCompoundTag(NBT_INVENTORY));
        }
        if (compound.hasKey(NBT_TANK)) {
            tank.readFromNBT(compound.getCompoundTag(NBT_TANK));
        }
        fuel = clamp(compound.getDouble(NBT_FUEL), 0.0D, ReactorMath.MAX_FUEL);
        carbon = Math.max(0.0D, compound.getDouble(NBT_CARBON));
        redstone = Math.max(0.0D, compound.getDouble(NBT_REDSTONE));
        solidCoolant = Math.max(0.0D, compound.getDouble(NBT_SOLID));
        solidCoolantTemp = compound.getInteger(NBT_SOLID_TEMP);
        temperature = clamp(compound.getDouble(NBT_TEMP), 0.0D, ReactorMath.MAX_TEMPERATURE);
        baseTemp = Math.max(0, compound.getInteger(NBT_BASE_TEMP));
        carbonTemp = Math.max(0, compound.getInteger(NBT_CARBON_TEMP));
        redstoneTemp = Math.max(0, compound.getInteger(NBT_REDSTONE_TEMP));
        running = compound.getBoolean(NBT_RUNNING);
        genModeOn = compound.getBoolean(NBT_GEN_MODE);
        generate = !compound.hasKey(NBT_GENERATE) || compound.getBoolean(NBT_GENERATE);
        built = compound.getBoolean(NBT_BUILT);
        buildIndex = Math.max(0, Math.min(36, compound.getInteger(NBT_BUILD_INDEX)));
        // Existing assembled structures were paid for before
        // the authorization flag existed. A fresh TileEntity never enters this branch.
        boolean legacyPaidStructure = built || buildIndex > 0;
        assemblyAuthorized = compound.hasKey(NBT_ASSEMBLY_AUTHORIZED)
                ? compound.getBoolean(NBT_ASSEMBLY_AUTHORIZED)
                : legacyPaidStructure;
        assemblyRefundable = compound.hasKey(NBT_ASSEMBLY_REFUNDABLE)
                ? compound.getBoolean(NBT_ASSEMBLY_REFUNDABLE)
                : legacyPaidStructure;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
