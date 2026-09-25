package com.sosea1.powah.content.energizing;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import com.sosea1.powah.common.energy.LongEnergyBuffer;
import com.sosea1.powah.common.recipe.EnergizingRecipe;
import com.sosea1.powah.common.recipe.EnergizingRecipeManager;

/** Seven-slot Energizing Orb: slot 0 output, slots 1..6 exact one-item inputs. */
public final class TileEnergizingOrb extends TileEntity {
    public static final int OUTPUT_SLOT = 0;
    public static final int FIRST_INPUT_SLOT = 1;
    public static final int LAST_INPUT_SLOT = 6;
    private static final String NBT_INVENTORY = "PowahInventory";
    private static final String NBT_PROGRESS = "PowahEnergizingProgress";

    private final LongEnergyBuffer progress = new LongEnergyBuffer(0L, 0L, 0L);
    private boolean mutatingInventory;
    private EnergizingRecipe recipe;
    private long lastSyncedProgress = -1L;
    private int clientSyncCooldown = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEnergizingOrb.this.markDirty();
            if (!mutatingInventory) {
                TileEnergizingOrb.this.onInventoryChanged();
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == OUTPUT_SLOT ? 64 : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Forge's contract asks whether the item can ever be inserted here; current fullness/output
            // state belongs in insertItem(), not this predicate.
            return slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT && stack != null && !stack.isEmpty();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty() || slot < FIRST_INPUT_SLOT || slot > LAST_INPUT_SLOT) {
                return stack;
            }
            if (!getStackInSlot(OUTPUT_SLOT).isEmpty() || !getStackInSlot(slot).isEmpty()) {
                return stack;
            }
            java.util.List<ItemStack> current = new java.util.ArrayList<ItemStack>(6);
            for (int s = FIRST_INPUT_SLOT; s <= LAST_INPUT_SLOT; s++) {
                if (s == slot) continue;
                ItemStack existing = getStackInSlot(s);
                if (!existing.isEmpty()) {
                    current.add(existing);
                }
            }
            ItemStack candidate = stack.copy();
            candidate.setCount(1);
            current.add(candidate);

            if (!EnergizingRecipeManager.instance().hasPotentialRecipe(current)) {
                return stack;
            }

            ItemStack one = stack.copy();
            one.setCount(1);
            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            if (!simulate) {
                setStackInSlot(slot, one);
            }
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };

    public IItemHandler getInventory() {
        return inventory;
    }

    /** Direction from the block center toward the floating orb, opposite the attached/base face. */
    public EnumFacing getOrbUp() {
        if (world != null) {
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockEnergizingOrb && state.getPropertyKeys().contains(BlockEnergizingOrb.FACING)) {
                return state.getValue(BlockEnergizingOrb.FACING).getOpposite();
            }
        }
        return EnumFacing.UP;
    }

    public boolean containsRecipe() {
        return recipe != null;
    }

    public EnergizingRecipe getRecipe() {
        return recipe;
    }

    public long getProgress() {
        return progress.energy();
    }

    public long getRequiredEnergy() {
        return progress.capacity();
    }

    public long fillEnergy(long amount) {
        if (amount <= 0L || recipe == null || progress.capacity() <= 0L) {
            return 0L;
        }
        long accepted = progress.generate(amount, false);
        if (accepted > 0L) {
            markDirty();
            if (world != null && !world.isRemote) {
                long cur = progress.energy();
                if (cur != lastSyncedProgress) {
                    if (++clientSyncCooldown >= 5 || progress.isFull()) {
                        clientSyncCooldown = 0;
                        lastSyncedProgress = cur;
                        syncClientInventory();
                    }
                }
            }
        }
        if (progress.isFull()) {
            finishRecipe();
        }
        return accepted;
    }

    public boolean addNextInput(ItemStack source) {
        if (source == null || source.isEmpty() || !inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        for (int slot = FIRST_INPUT_SLOT; slot <= LAST_INPUT_SLOT; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                ItemStack one = source.copy();
                one.setCount(1);
                ItemStack remainder = inventory.insertItem(slot, one, false);
                return remainder.isEmpty();
            }
        }
        return false;
    }

    public ItemStack removeNext() {
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            ItemStack copy = output.copy();
            inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return copy;
        }
        for (int slot = LAST_INPUT_SLOT; slot >= FIRST_INPUT_SLOT; slot--) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    private void onInventoryChanged() {
        progress.setLimits(0L, 0L, 0L);
        progress.setEnergy(0L);
        recipe = EnergizingRecipeManager.instance().find(inventory);
        if (recipe != null) {
            long required = recipe.scaledEnergy();
            progress.setLimits(required, required, 0L);
        }
        syncClientInventory();
    }

    private void finishRecipe() {
        if (recipe == null) {
            return;
        }
        ItemStack result = recipe.output();
        mutatingInventory = true;
        try {
            for (int slot = FIRST_INPUT_SLOT; slot <= LAST_INPUT_SLOT; slot++) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
            inventory.setStackInSlot(OUTPUT_SLOT, result);
        } finally {
            mutatingInventory = false;
        }
        recipe = null;
        progress.setLimits(0L, 0L, 0L);
        progress.setEnergy(0L);
        markDirty();
        syncClientInventory();
    }


    private void syncClientInventory() {
        if (world != null && !world.isRemote) {
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
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
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag(NBT_INVENTORY, inventory.serializeNBT());
        compound.setLong(NBT_PROGRESS, progress.energy());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        mutatingInventory = true;
        try {
            if (compound.hasKey(NBT_INVENTORY)) {
                inventory.deserializeNBT(compound.getCompoundTag(NBT_INVENTORY));
            }
        } finally {
            mutatingInventory = false;
        }
        recipe = EnergizingRecipeManager.instance().find(inventory);
        if (recipe == null) {
            progress.setLimits(0L, 0L, 0L);
            progress.setEnergy(0L);
        } else {
            long required = recipe.scaledEnergy();
            progress.setLimits(required, required, 0L);
            progress.setEnergy(Math.max(0L, compound.getLong(NBT_PROGRESS)));
        }
    }
}
