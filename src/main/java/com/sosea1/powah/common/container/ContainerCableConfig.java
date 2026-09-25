package com.sosea1.powah.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.energy.RedstoneControlMode;
import com.sosea1.powah.content.cable.TileCable;

/**
 * Tiny server-authoritative cable-face configuration container.
 *
 * <p>The selected face is encoded in the GUI id by {@link PowahGuiHandler}; no custom packet is
 * needed on 1.12. Mode/redstone changes use the vanilla enchant-button packet and are revalidated
 * against the live cable/adjacent FE endpoint on the server.</p>
 */
public final class ContainerCableConfig extends Container {
    private static final int MODE_PROPERTY = 0;
    private static final int REDSTONE_PROPERTY = 1;

    public static final int ACTION_NEXT_MODE = 0;
    public static final int ACTION_REDSTONE = 1;

    private final TileCable cable;
    private final EnumFacing side;
    private int syncedMode;
    private int syncedRedstone;
    private int lastMode = Integer.MIN_VALUE;
    private int lastRedstone = Integer.MIN_VALUE;

    public ContainerCableConfig(InventoryPlayer playerInventory, TileCable cable, EnumFacing side) {
        if (cable == null) throw new NullPointerException("cable");
        if (side == null) throw new NullPointerException("side");
        this.cable = cable;
        this.side = side;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, MODE_PROPERTY, cable.getSideMode(side).ordinal());
        listener.sendWindowProperty(this, REDSTONE_PROPERTY, cable.getRedstoneControlMode().ordinal());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int mode = cable.getSideMode(side).ordinal();
        int redstone = cable.getRedstoneControlMode().ordinal();
        for (IContainerListener listener : listeners) {
            if (mode != lastMode) listener.sendWindowProperty(this, MODE_PROPERTY, mode);
            if (redstone != lastRedstone) listener.sendWindowProperty(this, REDSTONE_PROPERTY, redstone);
        }
        lastMode = mode;
        lastRedstone = redstone;
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == MODE_PROPERTY) {
            syncedMode = data & 0xFFFF;
        } else if (id == REDSTONE_PROPERTY) {
            syncedRedstone = data & 0xFFFF;
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer playerIn, int id) {
        if (!canInteractWith(playerIn)) return false;
        if (id == ACTION_NEXT_MODE) {
            cable.cycleSideMode(side);
            return true;
        }
        if (id == ACTION_REDSTONE) {
            RedstoneControlMode current = cable.getRedstoneControlMode();
            RedstoneControlMode[] values = RedstoneControlMode.values();
            cable.setRedstoneControlMode(values[(current.ordinal() + 1) % values.length]);
            return true;
        }
        return false;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        if (cable.getWorld() == null) return false;
        TileEntity current = cable.getWorld().getTileEntity(cable.getPos());
        return current == cable
                && playerIn.getDistanceSq(cable.getPos()) <= 64.0D
                && cable.hasExternalEnergySide(side);
    }

    public TileCable getCable() {
        return cable;
    }

    public EnumFacing getSide() {
        return side;
    }

    public EnergyPortMode getSyncedMode() {
        EnergyPortMode[] values = EnergyPortMode.values();
        return syncedMode >= 0 && syncedMode < values.length ? values[syncedMode] : EnergyPortMode.BOTH;
    }

    public RedstoneControlMode getSyncedRedstoneMode() {
        RedstoneControlMode[] values = RedstoneControlMode.values();
        return syncedRedstone >= 0 && syncedRedstone < values.length
                ? values[syncedRedstone] : RedstoneControlMode.IGNORED;
    }
}
