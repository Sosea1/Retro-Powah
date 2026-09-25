package com.sosea1.powah.content.ender;

import java.util.UUID;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import com.sosea1.powah.Powah;
import com.sosea1.powah.api.energy.endernetwork.IEnderExtender;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.ender.EnderChannelState;
import com.sosea1.powah.common.ender.EnderNetworkData;
import com.sosea1.powah.common.energy.EnergyIntMath;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.energy.LongEnergyBuffer;
import com.sosea1.powah.common.tier.PowahTier;

/** Shared owner/channel endpoint for Ender Cell and Ender Gate. */
public abstract class AbstractEnderTile extends AbstractEnergyTile implements ITickable {
    public static final int EXTENDER_SLOT = 0;
    public static final int CHARGE_SLOT_0 = 1;
    public static final int CHARGE_SLOT_1 = 2;
    private static final String NBT_OWNER = "EnderOwner";
    private static final String NBT_OWNER_NAME = "EnderOwnerName";
    private static final String NBT_CHANNEL = "EnderChannel";
    private static final String NBT_INVENTORY = "EnderInventory";
    private static final EnumFacing[] FACINGS = EnumFacing.values();

    private final IEnergyStorage[] networkAdapters = new IEnergyStorage[7];
    private EnderNetworkData networkCache;
    private UUID owner;
    private String ownerName = "";
    private int channel;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public int getSlotLimit(int slot) { return 1; }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == EXTENDER_SLOT) return supportsCapacityExtender() && canAcceptCapacityExtender(stack);
            return (slot == CHARGE_SLOT_0 || slot == CHARGE_SLOT_1) && canReceiveEnergy(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            AbstractEnderTile.this.markDirty();
        }
    };

    protected AbstractEnderTile(PowahTier tier) {
        super(tier, 0L, 0L, 0L, EnergyPortMode.NONE, EnergyPortMode.NONE);
        for (final EnumFacing facing : FACINGS) networkAdapters[facing.ordinal()] = createAdapter(facing);
        networkAdapters[6] = createAdapter(null);
    }

    protected abstract long networkTransfer(PowahTier tier);
    protected abstract boolean supportsCapacityExtender();
    protected boolean isEnergySide(EnumFacing side) { return true; }

    @Override
    public boolean canConfigureSide(EnumFacing side) {
        return false;
    }

    private IEnergyStorage createAdapter(final EnumFacing side) {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                if (maxReceive <= 0 || !isEnergySide(side) || !isOperationAllowed()) return 0;
                EnderNetworkData network = network();
                if (network == null || owner == null) return 0;
                long moved = network.receive(owner, channel, maxReceive, networkTransfer(getTier()), simulate);
                if (!simulate && moved > 0L) syncMirror();
                return EnergyIntMath.saturatedInt(moved);
            }
            @Override public int extractEnergy(int maxExtract, boolean simulate) {
                if (maxExtract <= 0 || !isEnergySide(side) || !isOperationAllowed()) return 0;
                EnderNetworkData network = network();
                if (network == null || owner == null) return 0;
                long moved = network.extract(owner, channel, maxExtract, networkTransfer(getTier()), simulate);
                if (!simulate && moved > 0L) syncMirror();
                return EnergyIntMath.saturatedInt(moved);
            }
            @Override public int getEnergyStored() {
                EnderChannelState state = state();
                return state == null ? 0 : EnergyIntMath.saturatedInt(state.energy());
            }
            @Override public int getMaxEnergyStored() {
                EnderChannelState state = state();
                return state == null ? 0 : EnergyIntMath.saturatedInt(state.capacity());
            }
            @Override public boolean canExtract() { return isEnergySide(side) && owner != null && isOperationAllowed(); }
            @Override public boolean canReceive() { return isEnergySide(side) && owner != null && isOperationAllowed(); }
        };
    }

    @Override protected long getCapacityForTier(PowahTier tier) { return 0L; }
    @Override protected long getMaxReceiveForTier(PowahTier tier) { return 0L; }
    @Override protected long getMaxExtractForTier(PowahTier tier) { return 0L; }

    public final void claim(UUID id, String name) {
        if (id == null) throw new NullPointerException("id");
        if (owner == null) {
            owner = id;
            ownerName = name == null ? "" : name;
            markDirty();
            syncMirror();
        }
    }

    public final boolean isOwner(UUID id) { return owner != null && owner.equals(id); }
    public final UUID getOwner() { return owner; }
    public final String getOwnerName() { return ownerName; }
    public final int getChannel() { return channel; }
    public final int getMaxChannels() {
        long configured = Powah.energyConfig().enderChannels().get(getTier());
        return (int) Math.max(1L, Math.min(EnderNetworkData.MAX_CHANNELS, configured));
    }

    public final boolean setChannel(int newChannel) {
        if (newChannel < 0 || newChannel >= getMaxChannels() || newChannel == channel) return false;
        channel = newChannel;
        markDirty();
        syncMirror();
        return true;
    }

    public final int cycleChannel() {
        channel = (channel + 1) % getMaxChannels();
        markDirty();
        syncMirror();
        return channel;
    }

    public final ItemStackHandler getInventory() { return inventory; }

    @Override
    public void update() {
        if (world == null || world.isRemote || owner == null) return;
        if (supportsCapacityExtender()) consumeCapacityExtender();
        chargeItem(CHARGE_SLOT_0);
        chargeItem(CHARGE_SLOT_1);
        if (isOperationAllowed()) {
            pushNetworkEnergy();
        }
        syncMirror();
    }

    private void consumeCapacityExtender() {
        ItemStack stack = inventory.getStackInSlot(EXTENDER_SLOT);
        long capacity = getExtenderCapacity(stack);
        if (capacity <= 0L) return;
        long imported = getExtenderStoredEnergy(stack);
        EnderNetworkData network = network();
        if (network == null) return;
        if (network.extend(owner, channel, capacity, Math.min(capacity, imported))) {
            inventory.extractItem(EXTENDER_SLOT, 1, false);
            world.playSound(null, pos, SoundEvents.ENTITY_ENDEREYE_DEATH, SoundCategory.BLOCKS, 1.0F, 1.0F);
            syncMirror();
        }
    }

    private void chargeItem(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!canReceiveEnergy(stack)) return;
        IEnergyStorage target = stack.getCapability(CapabilityEnergy.ENERGY, null);
        EnderNetworkData network = network();
        if (target == null || network == null) return;
        int offer = EnergyIntMath.saturatedInt(networkTransfer(getTier()));
        int accepted = target.receiveEnergy(offer, true);
        if (accepted <= 0) return;
        long extracted = network.extract(owner, channel, accepted, networkTransfer(getTier()), false);
        if (extracted <= 0L) return;
        int actual = target.receiveEnergy(EnergyIntMath.saturatedInt(extracted), false);
        if (actual > 0) {
            markDirty();
        }
        if (actual < extracted) {
            network.receive(owner, channel, extracted - Math.max(0, actual), Long.MAX_VALUE, false);
        }
    }

    private void pushNetworkEnergy() {
        EnderNetworkData network = network();
        if (network == null) return;
        long remaining = Math.min(networkTransfer(getTier()), network.channel(owner, channel).energy());
        if (remaining <= 0L) return;
        int start = (int) (world.getTotalWorldTime() % FACINGS.length);
        for (int i = 0; i < FACINGS.length && remaining > 0L; i++) {
            EnumFacing side = FACINGS[(start + i) % FACINGS.length];
            if (!isEnergySide(side)) continue;
            BlockPos targetPos = pos.offset(side);
            if (!world.isBlockLoaded(targetPos)) continue;
            TileEntity targetTile = world.getTileEntity(targetPos);
            if (targetTile == null || targetTile == this) continue;
            if (targetTile instanceof AbstractEnderTile) {
                AbstractEnderTile other = (AbstractEnderTile) targetTile;
                if (owner.equals(other.owner) && channel == other.channel) continue;
            }
            if (!targetTile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) continue;
            IEnergyStorage target = targetTile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (target == null || !target.canReceive()) continue;
            int accepted = target.receiveEnergy(EnergyIntMath.saturatedInt(remaining), true);
            if (accepted <= 0) continue;
            long extracted = network.extract(owner, channel, accepted, remaining, false);
            int actual = target.receiveEnergy(EnergyIntMath.saturatedInt(extracted), false);
            if (actual < extracted) network.receive(owner, channel, extracted - Math.max(0, actual), Long.MAX_VALUE, false);
            remaining -= Math.max(0, actual);
        }
    }

    private EnderNetworkData network() {
        if (world == null || world.isRemote) {
            return null;
        }
        if (networkCache == null) {
            networkCache = EnderNetworkData.get(world);
        }
        return networkCache;
    }

    @Override
    public void invalidate() {
        networkCache = null;
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        networkCache = null;
        super.onChunkUnload();
    }

    private EnderChannelState state() {
        EnderNetworkData network = network();
        return network == null || owner == null ? null : network.channel(owner, channel);
    }

    private void syncMirror() {
        EnderChannelState state = state();
        long capacity = state == null ? 0L : state.capacity();
        long stored = state == null ? 0L : state.energy();
        long transfer = networkTransfer(getTier());
        LongEnergyBuffer mirror = getEnergyBuffer();
        if (mirror.capacity() == capacity
                && mirror.maxReceive() == transfer
                && mirror.maxExtract() == transfer
                && mirror.energy() == stored) {
            return;
        }
        mirror.setLimits(capacity, transfer, transfer);
        mirror.setEnergy(stored);
        onEnergyChanged();
    }

    private static boolean canReceiveEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasCapability(CapabilityEnergy.ENERGY, null)) return false;
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return storage != null && storage.canReceive();
    }


    private boolean canAcceptCapacityExtender(ItemStack stack) {
        long addedCapacity = getExtenderCapacity(stack);
        if (addedCapacity <= 0L) return false;
        EnderChannelState state = state();
        return state == null || state.canExtend(addedCapacity);
    }

    private static long getExtenderCapacity(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IEnderExtender)) return 0L;
        return Math.max(0L, ((IEnderExtender) stack.getItem()).getExtendedCapacity(stack));
    }

    private static long getExtenderStoredEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IEnderExtender)) return 0L;
        return Math.max(0L, ((IEnderExtender) stack.getItem()).getExtendedEnergy(stack));
    }

    @Override public long getGuiAuxValue() { return channel + 1L; }
    @Override public int getGuiFlags() { return getMaxChannels(); }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return isEnergySide(facing);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return isEnergySide(facing) ? CapabilityEnergy.ENERGY.cast(networkAdapters[facing == null ? 6 : facing.ordinal()]) : null;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        return super.getCapability(capability, facing);
    }

    @Override
    protected void writePowahData(NBTTagCompound compound) {
        if (owner != null) compound.setString(NBT_OWNER, owner.toString());
        compound.setString(NBT_OWNER_NAME, ownerName);
        compound.setInteger(NBT_CHANNEL, channel);
        compound.setTag(NBT_INVENTORY, inventory.serializeNBT());
        writeEnderData(compound);
    }

    @Override
    protected void readPowahData(NBTTagCompound compound) {
        String id = compound.getString(NBT_OWNER);
        if (id != null && !id.isEmpty()) {
            try { owner = UUID.fromString(id); } catch (IllegalArgumentException ignored) { owner = null; }
        }
        ownerName = compound.getString(NBT_OWNER_NAME);
        channel = Math.max(0, Math.min(getMaxChannels() - 1, compound.getInteger(NBT_CHANNEL)));
        if (compound.hasKey(NBT_INVENTORY)) inventory.deserializeNBT(compound.getCompoundTag(NBT_INVENTORY));
        readEnderData(compound);
    }

    protected void writeEnderData(NBTTagCompound compound) { }
    protected void readEnderData(NBTTagCompound compound) { }
}
