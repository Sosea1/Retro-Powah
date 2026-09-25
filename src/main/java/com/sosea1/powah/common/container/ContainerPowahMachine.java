package com.sosea1.powah.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.SlotItemHandler;
import com.sosea1.powah.common.block.entity.AbstractChargeableGeneratorTile;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.energy.RedstoneControlMode;
import com.sosea1.powah.content.furnator.TileFurnator;
import com.sosea1.powah.content.energycell.TileEnergyCell;
import com.sosea1.powah.content.ender.AbstractEnderTile;
import com.sosea1.powah.content.ender.TileEnderCell;
import com.sosea1.powah.content.ender.TileEnderGate;
import com.sosea1.powah.content.reactor.TileReactor;
import com.sosea1.powah.content.discharger.TileEnergyDischarger;
import com.sosea1.powah.content.transmitter.TilePlayerTransmitter;
import com.sosea1.powah.content.hopper.TileEnergyHopper;
import com.sosea1.powah.content.solar.TileSolarPanel;
import com.sosea1.powah.content.thermo.TileThermoGenerator;
import com.sosea1.powah.content.magmator.TileMagmator;

/**
 * One compact container for the early Powah machines. Long values are split into four unsigned
 * 16-bit words because the vanilla 1.12 window-property packet is short-backed.
 */
public final class ContainerPowahMachine extends Container {
    private static final int ENERGY_BASE = 0;
    private static final int CAPACITY_BASE = 4;
    private static final int AUX_BASE = 8;
    private static final int FLAGS_ID = 12;
    private static final int FLUID_AMOUNT_ID = 13;
    private static final int FLUID_CAPACITY_ID = 14;
    private static final int REACTOR_FUEL_ID = 15;
    private static final int REACTOR_TEMP_ID = 16;
    private static final int REACTOR_CARBON_ID = 17;
    private static final int REACTOR_REDSTONE_ID = 18;
    private static final int REACTOR_SOLID_ID = 19;
    private static final int REDSTONE_MODE_ID = 20;
    private static final int SIDE_MODES_ID = 21;

    public static final int ACTION_REDSTONE = 100;
    public static final int ACTION_REACTOR_GEN_MODE = 101;
    public static final int ACTION_ENDER_CHANNEL_PREV = 102;
    public static final int ACTION_ENDER_CHANNEL_NEXT = 103;
    public static final int ACTION_ENDER_CHANNEL_BASE = 130;
    public static final int ACTION_SIDE_BASE = 110;
    public static final int ACTION_SIDE_ALL = 116;

    private final AbstractEnergyTile tile;
    private final int machineSlotCount;
    private long syncedEnergy;
    private long syncedCapacity;
    private long syncedAux;
    private int syncedFlags;
    private int syncedFluidAmount;
    private int syncedFluidCapacity;
    private int syncedReactorFuel;
    private int syncedReactorTemperature;
    private int syncedReactorCarbon;
    private int syncedReactorRedstone;
    private int syncedReactorSolidCoolant;
    private int syncedRedstoneMode;
    private int syncedSideModes;

    private long lastEnergy = Long.MIN_VALUE;
    private long lastCapacity = Long.MIN_VALUE;
    private long lastAux = Long.MIN_VALUE;
    private int lastFlags = Integer.MIN_VALUE;
    private int lastFluidAmount = Integer.MIN_VALUE;
    private int lastFluidCapacity = Integer.MIN_VALUE;
    private int lastReactorFuel = Integer.MIN_VALUE;
    private int lastReactorTemperature = Integer.MIN_VALUE;
    private int lastReactorCarbon = Integer.MIN_VALUE;
    private int lastReactorRedstone = Integer.MIN_VALUE;
    private int lastReactorSolidCoolant = Integer.MIN_VALUE;
    private int lastRedstoneMode = Integer.MIN_VALUE;
    private int lastSideModes = Integer.MIN_VALUE;

    public ContainerPowahMachine(InventoryPlayer playerInventory, AbstractEnergyTile tile) {
        this.tile = tile;
        addMachineSlots(tile);
        this.machineSlotCount = inventorySlots.size();
        addPlayerSlots(playerInventory, playerInventoryY(tile));
    }

    private void addMachineSlots(AbstractEnergyTile machine) {
        if (machine instanceof TileFurnator) {
            TileFurnator furnator = (TileFurnator) machine;
            addSlotToContainer(new SlotItemHandler(furnator.getInventory(), TileFurnator.CHARGE_SLOT, 4, 54));
            addSlotToContainer(new SlotItemHandler(furnator.getInventory(), TileFurnator.FUEL_SLOT, 87, 18));
        } else if (machine instanceof TileEnergyCell) {
            TileEnergyCell cell = (TileEnergyCell) machine;
            addSlotToContainer(new SlotItemHandler(cell.getInventory(), TileEnergyCell.CHARGE_SLOT_0, 4, 4));
            addSlotToContainer(new SlotItemHandler(cell.getInventory(), TileEnergyCell.CHARGE_SLOT_1, 4, 29));
        } else if (machine instanceof TileEnderCell) {
            TileEnderCell cell = (TileEnderCell) machine;
            addSlotToContainer(new SlotItemHandler(cell.getInventory(), AbstractEnderTile.EXTENDER_SLOT, -22, 52));
            addSlotToContainer(new SlotItemHandler(cell.getInventory(), AbstractEnderTile.CHARGE_SLOT_0, 4, 4));
            addSlotToContainer(new SlotItemHandler(cell.getInventory(), AbstractEnderTile.CHARGE_SLOT_1, 4, 29));
        } else if (machine instanceof TileEnderGate) {
            TileEnderGate gate = (TileEnderGate) machine;
            addSlotToContainer(new SlotItemHandler(gate.getInventory(), AbstractEnderTile.CHARGE_SLOT_0, 4, 4));
            addSlotToContainer(new SlotItemHandler(gate.getInventory(), AbstractEnderTile.CHARGE_SLOT_1, 4, 29));
        } else if (machine instanceof TilePlayerTransmitter) {
            TilePlayerTransmitter transmitter = (TilePlayerTransmitter) machine;
            addSlotToContainer(new SlotItemHandler(transmitter.getInventory(), TilePlayerTransmitter.CARD_SLOT, 4, 29));
        } else if (machine instanceof TileEnergyDischarger) {
            TileEnergyDischarger discharger = (TileEnergyDischarger) machine;
            for (int slot = 0; slot < TileEnergyDischarger.SLOT_COUNT; slot++) {
                addSlotToContainer(new SlotItemHandler(discharger.getInventory(), slot, 5 + slot * 25, 54));
            }
        } else if (machine instanceof TileReactor) {
            TileReactor reactor = (TileReactor) machine;
            addSlotToContainer(new SlotItemHandler(reactor.getInventory(), TileReactor.CHARGE_SLOT, 4, 54));
            addSlotToContainer(new SlotItemHandler(reactor.getInventory(), TileReactor.FUEL_SLOT, 73, 29));
            addSlotToContainer(new SlotItemHandler(reactor.getInventory(), TileReactor.CARBON_SLOT, 31, 6));
            addSlotToContainer(new SlotItemHandler(reactor.getInventory(), TileReactor.REDSTONE_SLOT, 31, 52));
            addSlotToContainer(new SlotItemHandler(reactor.getInventory(), TileReactor.SOLID_COOLANT_SLOT, 120, 52));
        } else if (machine instanceof TileSolarPanel) {
            TileSolarPanel solar = (TileSolarPanel) machine;
            addSlotToContainer(new SlotItemHandler(solar.getChargeInventory(), 0, 4, 29));
        } else if (machine instanceof AbstractChargeableGeneratorTile) {
            AbstractChargeableGeneratorTile generator = (AbstractChargeableGeneratorTile) machine;
            addSlotToContainer(new SlotItemHandler(generator.getChargeInventory(), 0, 4, 54));
        }
    }

    private static int playerInventoryY(AbstractEnergyTile machine) {
        if (machine instanceof TileFurnator || machine instanceof TileMagmator
                || machine instanceof TileThermoGenerator || machine instanceof TileReactor
                || machine instanceof TileEnergyDischarger) {
            return 84;
        }
        if (machine instanceof TileEnderCell || machine instanceof TileEnderGate) {
            return 82;
        }
        return 59;
    }

    private void addPlayerSlots(InventoryPlayer inventory, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new net.minecraft.inventory.Slot(inventory, col + row * 9 + 9,
                        8 + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new net.minecraft.inventory.Slot(inventory, col, 8 + col * 18, y + 58));
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendLong(listener, ENERGY_BASE, tile.getEnergyBuffer().energy());
        sendLong(listener, CAPACITY_BASE, tile.getEnergyBuffer().capacity());
        sendLong(listener, AUX_BASE, tile.getGuiAuxValue());
        listener.sendWindowProperty(this, FLAGS_ID, tile.getGuiFlags());
        listener.sendWindowProperty(this, FLUID_AMOUNT_ID, tile.getGuiFluidAmount());
        listener.sendWindowProperty(this, FLUID_CAPACITY_ID, tile.getGuiFluidCapacity());
        listener.sendWindowProperty(this, REDSTONE_MODE_ID, tile.getRedstoneControlMode().ordinal());
        listener.sendWindowProperty(this, SIDE_MODES_ID, packSideModes(tile));
        sendReactor(listener);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        long energy = tile.getEnergyBuffer().energy();
        long capacity = tile.getEnergyBuffer().capacity();
        long aux = tile.getGuiAuxValue();
        int flags = tile.getGuiFlags();
        int fluidAmount = tile.getGuiFluidAmount();
        int fluidCapacity = tile.getGuiFluidCapacity();
        int reactorFuel = reactorValue(0);
        int reactorTemp = reactorValue(1);
        int reactorCarbon = reactorValue(2);
        int reactorRedstone = reactorValue(3);
        int reactorSolid = reactorValue(4);
        int redstoneMode = tile.getRedstoneControlMode().ordinal();
        int sideModes = packSideModes(tile);

        for (IContainerListener listener : listeners) {
            if (energy != lastEnergy) {
                sendLong(listener, ENERGY_BASE, energy);
            }
            if (capacity != lastCapacity) {
                sendLong(listener, CAPACITY_BASE, capacity);
            }
            if (aux != lastAux) {
                sendLong(listener, AUX_BASE, aux);
            }
            if (flags != lastFlags) {
                listener.sendWindowProperty(this, FLAGS_ID, flags);
            }
            if (fluidAmount != lastFluidAmount) {
                listener.sendWindowProperty(this, FLUID_AMOUNT_ID, fluidAmount);
            }
            if (fluidCapacity != lastFluidCapacity) {
                listener.sendWindowProperty(this, FLUID_CAPACITY_ID, fluidCapacity);
            }
            if (reactorFuel != lastReactorFuel) listener.sendWindowProperty(this, REACTOR_FUEL_ID, reactorFuel);
            if (reactorTemp != lastReactorTemperature) listener.sendWindowProperty(this, REACTOR_TEMP_ID, reactorTemp);
            if (reactorCarbon != lastReactorCarbon) listener.sendWindowProperty(this, REACTOR_CARBON_ID, reactorCarbon);
            if (reactorRedstone != lastReactorRedstone) listener.sendWindowProperty(this, REACTOR_REDSTONE_ID, reactorRedstone);
            if (reactorSolid != lastReactorSolidCoolant) listener.sendWindowProperty(this, REACTOR_SOLID_ID, reactorSolid);
            if (redstoneMode != lastRedstoneMode) listener.sendWindowProperty(this, REDSTONE_MODE_ID, redstoneMode);
            if (sideModes != lastSideModes) listener.sendWindowProperty(this, SIDE_MODES_ID, sideModes);
        }
        lastEnergy = energy;
        lastCapacity = capacity;
        lastAux = aux;
        lastFlags = flags;
        lastFluidAmount = fluidAmount;
        lastFluidCapacity = fluidCapacity;
        lastReactorFuel = reactorFuel;
        lastReactorTemperature = reactorTemp;
        lastReactorCarbon = reactorCarbon;
        lastReactorRedstone = reactorRedstone;
        lastReactorSolidCoolant = reactorSolid;
        lastRedstoneMode = redstoneMode;
        lastSideModes = sideModes;
    }

    private void sendLong(IContainerListener listener, int baseId, long value) {
        for (int word = 0; word < 4; word++) {
            listener.sendWindowProperty(this, baseId + word,
                    (int) ((value >>> (word * 16)) & 0xFFFFL));
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id >= ENERGY_BASE && id < ENERGY_BASE + 4) {
            syncedEnergy = replaceWord(syncedEnergy, id - ENERGY_BASE, data);
        } else if (id >= CAPACITY_BASE && id < CAPACITY_BASE + 4) {
            syncedCapacity = replaceWord(syncedCapacity, id - CAPACITY_BASE, data);
        } else if (id >= AUX_BASE && id < AUX_BASE + 4) {
            syncedAux = replaceWord(syncedAux, id - AUX_BASE, data);
        } else if (id == FLAGS_ID) {
            syncedFlags = data & 0xFFFF;
        } else if (id == FLUID_AMOUNT_ID) {
            syncedFluidAmount = data & 0xFFFF;
        } else if (id == FLUID_CAPACITY_ID) {
            syncedFluidCapacity = data & 0xFFFF;
        } else if (id == REACTOR_FUEL_ID) {
            syncedReactorFuel = data & 0xFFFF;
        } else if (id == REACTOR_TEMP_ID) {
            syncedReactorTemperature = data & 0xFFFF;
        } else if (id == REACTOR_CARBON_ID) {
            syncedReactorCarbon = data & 0xFFFF;
        } else if (id == REACTOR_REDSTONE_ID) {
            syncedReactorRedstone = data & 0xFFFF;
        } else if (id == REACTOR_SOLID_ID) {
            syncedReactorSolidCoolant = data & 0xFFFF;
        } else if (id == REDSTONE_MODE_ID) {
            syncedRedstoneMode = data & 0xFFFF;
        } else if (id == SIDE_MODES_ID) {
            syncedSideModes = data & 0xFFFF;
        }
    }

    private static int packSideModes(AbstractEnergyTile tile) {
        int packed = 0;
        for (EnumFacing side : EnumFacing.values()) {
            packed |= (tile.getSideMode(side).ordinal() & 0x3) << (side.ordinal() * 2);
        }
        return packed;
    }

    private static EnergyPortMode nextSupportedMode(AbstractEnergyTile tile, EnumFacing side, EnergyPortMode current) {
        EnergyPortMode candidate = current;
        for (int step = 0; step < EnergyPortMode.values().length; step++) {
            candidate = candidate.next();
            if (tile.isSideModeSupported(side, candidate)) {
                return candidate;
            }
        }
        return current;
    }

    @Override
    public boolean enchantItem(EntityPlayer playerIn, int id) {
        if (!canInteractWith(playerIn)) {
            return false;
        }
        if (id == ACTION_REDSTONE) {
            RedstoneControlMode current = tile.getRedstoneControlMode();
            RedstoneControlMode[] values = RedstoneControlMode.values();
            tile.setRedstoneControlMode(values[(current.ordinal() + 1) % values.length]);
            return true;
        }
        if (id == ACTION_REACTOR_GEN_MODE && tile instanceof TileReactor) {
            TileReactor reactor = (TileReactor) tile;
            reactor.setGenModeOn(!reactor.isGenModeOn());
            return true;
        }
        if ((id == ACTION_ENDER_CHANNEL_PREV || id == ACTION_ENDER_CHANNEL_NEXT)
                && tile instanceof AbstractEnderTile) {
            AbstractEnderTile ender = (AbstractEnderTile) tile;
            if (!ender.isOwner(playerIn.getUniqueID())) {
                return false;
            }
            int max = ender.getMaxChannels();
            if (max <= 1) {
                return false;
            }
            int delta = id == ACTION_ENDER_CHANNEL_NEXT ? 1 : -1;
            int next = (ender.getChannel() + delta + max) % max;
            return ender.setChannel(next);
        }
        if (id >= ACTION_ENDER_CHANNEL_BASE
                && id < ACTION_ENDER_CHANNEL_BASE + com.sosea1.powah.common.ender.EnderNetworkData.MAX_CHANNELS
                && tile instanceof AbstractEnderTile) {
            AbstractEnderTile ender = (AbstractEnderTile) tile;
            return ender.isOwner(playerIn.getUniqueID())
                    && id - ACTION_ENDER_CHANNEL_BASE < ender.getMaxChannels()
                    && ender.setChannel(id - ACTION_ENDER_CHANNEL_BASE);
        }
        if (id >= ACTION_SIDE_BASE && id < ACTION_SIDE_BASE + 6) {
            EnumFacing side = EnumFacing.byIndex(id - ACTION_SIDE_BASE);
            if (!tile.canConfigureSide(side)) {
                return false;
            }
            tile.setSideMode(side, nextSupportedMode(tile, side, tile.getSideMode(side)));
            return true;
        }
        if (id == ACTION_SIDE_ALL) {
            EnumFacing basis = EnumFacing.UP;
            EnergyPortMode next = nextSupportedMode(tile, basis, tile.getSideMode(basis));
            boolean changed = false;
            for (EnumFacing side : EnumFacing.values()) {
                if (tile.canConfigureSide(side) && tile.isSideModeSupported(side, next)) {
                    tile.setSideMode(side, next);
                    changed = true;
                }
            }
            return changed;
        }
        return super.enchantItem(playerIn, id);
    }

    private static long replaceWord(long current, int word, int data) {
        int shift = word * 16;
        long mask = 0xFFFFL << shift;
        return (current & ~mask) | (((long) data & 0xFFFFL) << shift);
    }


    private void sendReactor(IContainerListener listener) {
        if (!(tile instanceof TileReactor)) return;
        listener.sendWindowProperty(this, REACTOR_FUEL_ID, reactorValue(0));
        listener.sendWindowProperty(this, REACTOR_TEMP_ID, reactorValue(1));
        listener.sendWindowProperty(this, REACTOR_CARBON_ID, reactorValue(2));
        listener.sendWindowProperty(this, REACTOR_REDSTONE_ID, reactorValue(3));
        listener.sendWindowProperty(this, REACTOR_SOLID_ID, reactorValue(4));
    }

    private int reactorValue(int type) {
        if (!(tile instanceof TileReactor)) return 0;
        TileReactor reactor = (TileReactor) tile;
        double value;
        switch (type) {
            case 0: value = reactor.getFuel(); break;
            case 1: value = reactor.getTemperature(); break;
            case 2: value = reactor.getCarbon(); break;
            case 3: value = reactor.getRedstone(); break;
            case 4: value = reactor.getSolidCoolant(); break;
            default: value = 0.0D;
        }
        if (!(value > 0.0D)) return 0;
        return value >= 65535.0D ? 65535 : (int) value;
    }

    public AbstractEnergyTile getTile() {
        return tile;
    }

    public long getSyncedEnergy() {
        return syncedEnergy;
    }

    public long getSyncedCapacity() {
        return syncedCapacity;
    }

    public long getSyncedAux() {
        return syncedAux;
    }

    public int getSyncedFlags() {
        return syncedFlags;
    }

    public int getSyncedFluidAmount() {
        return syncedFluidAmount;
    }

    public int getSyncedFluidCapacity() {
        return syncedFluidCapacity;
    }

    public int getSyncedReactorFuel() { return syncedReactorFuel; }
    public int getSyncedReactorTemperature() { return syncedReactorTemperature; }
    public int getSyncedReactorCarbon() { return syncedReactorCarbon; }
    public int getSyncedReactorRedstone() { return syncedReactorRedstone; }
    public int getSyncedReactorSolidCoolant() { return syncedReactorSolidCoolant; }

    public RedstoneControlMode getSyncedRedstoneMode() {
        RedstoneControlMode[] values = RedstoneControlMode.values();
        return syncedRedstoneMode < values.length ? values[syncedRedstoneMode] : RedstoneControlMode.IGNORED;
    }

    public EnergyPortMode getSyncedSideMode(EnumFacing side) {
        int ordinal = (syncedSideModes >>> (side.ordinal() * 2)) & 0x3;
        EnergyPortMode[] values = EnergyPortMode.values();
        return ordinal < values.length ? values[ordinal] : EnergyPortMode.NONE;
    }

    public boolean hasConfigurableSides() {
        for (EnumFacing side : EnumFacing.values()) {
            if (tile.canConfigureSide(side)) return true;
        }
        return false;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        TileEntity current = tile.getWorld() == null ? null : tile.getWorld().getTileEntity(tile.getPos());
        if (current != tile || playerIn.getDistanceSq(tile.getPos()) > 64.0D) return false;
        if (tile instanceof AbstractEnderTile) {
            AbstractEnderTile ender = (AbstractEnderTile) tile;
            return ender.getOwner() == null || ender.isOwner(playerIn.getUniqueID());
        }
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        if (index < 0 || index >= inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        net.minecraft.inventory.Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getStack();
        ItemStack original = source.copy();
        boolean moved;
        if (index < machineSlotCount) {
            // Machine -> player inventory/hotbar. Reverse traversal mirrors vanilla containers.
            moved = mergeItemStack(source, machineSlotCount, inventorySlots.size(), true);
        } else {
            // Player -> machine. mergeItemStack respects Slot#isItemValid and per-slot limits,
            // so fuels, chargeables, Binding Cards, Ender extenders, coolant, etc. route safely.
            moved = machineSlotCount > 0 && mergeItemStack(source, 0, machineSlotCount, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(playerIn, source);
        return original;
    }
}
