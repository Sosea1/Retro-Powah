package com.sosea1.powah.content.transmitter;

import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
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

/** Charges the inventory of the player referenced by a Binding Card. */
public final class TilePlayerTransmitter extends AbstractEnergyTile implements ITickable {
    public static final int CARD_SLOT = 0;
    private static final String NBT_INVENTORY = "PowahInventory";
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemBindingCard
                    && ((ItemBindingCard) stack.getItem()).isBound(stack);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override protected void onContentsChanged(int slot) { TilePlayerTransmitter.this.cooldown = 0; TilePlayerTransmitter.this.markDirty(); }
    };
    private int cooldown;
    private long lastMoved;

    public TilePlayerTransmitter() { this(PowahTier.STARTER); }
    public TilePlayerTransmitter(PowahTier tier) {
        super(tier, capacity(tier), transfer(tier), transfer(tier), EnergyPortMode.INPUT, EnergyPortMode.INPUT);
    }
    private static long capacity(PowahTier tier) { return Powah.energyConfig().playerTransmitter().capacity().get(tier); }
    private static long transfer(PowahTier tier) { return Powah.energyConfig().playerTransmitter().transfer().get(tier); }
    private static long charging(PowahTier tier) { return Powah.energyConfig().playerTransmitterCharging().get(tier); }
    @Override protected long getCapacityForTier(PowahTier tier) { return capacity(tier); }
    @Override protected long getMaxReceiveForTier(PowahTier tier) { return transfer(tier); }
    @Override protected long getMaxExtractForTier(PowahTier tier) { return transfer(tier); }

    @Override public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return mode == EnergyPortMode.NONE || mode == EnergyPortMode.INPUT;
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (!isOperationAllowed()) { lastMoved = 0L; cooldown = 19; return; }
        if (cooldown-- > 0) return;
        long moved = 0L;
        ItemStack cardStack = inventory.getStackInSlot(CARD_SLOT);
        if (!cardStack.isEmpty() && cardStack.getItem() instanceof ItemBindingCard) {
            ItemBindingCard card = (ItemBindingCard) cardStack.getItem();
            UUID id = card.getBoundPlayer(cardStack);
            MinecraftServer server = world.getMinecraftServer();
            EntityPlayerMP player = id == null || server == null ? null : server.getPlayerList().getPlayerByUUID(id);
            if (player != null && (card.isDimensional() || player.dimension == world.provider.getDimension())) {
                moved = chargePlayer(player);
            }
        }
        cooldown = moved > 0L ? 0 : 19;
        if (lastMoved != moved) { lastMoved = moved; markDirty(); }
    }

    public long chargePlayer(EntityPlayerMP player) {
        if (player == null || getEnergyBuffer().isEmpty()) return 0L;
        long moved = EnergyItemHelper.chargePlayer(player, getEnergyBuffer(), charging(getTier()));
        if (moved > 0L) onEnergyChanged();
        return moved;
    }

    public ItemStackHandler getInventory() { return inventory; }
    @Override public long getGuiAuxValue() { return lastMoved; }
    @Override public int getGuiFlags() {
        ItemStack stack = inventory.getStackInSlot(CARD_SLOT);
        return !stack.isEmpty() && stack.getItem() instanceof ItemBindingCard && ((ItemBindingCard) stack.getItem()).isDimensional() ? 1 : 0;
    }
    @Override public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        return super.getCapability(capability, facing);
    }
    @Override protected void writePowahData(NBTTagCompound compound) { compound.setTag(NBT_INVENTORY, inventory.serializeNBT()); }
    @Override protected void readPowahData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_INVENTORY)) inventory.deserializeNBT(compound.getCompoundTag(NBT_INVENTORY));
    }
}
