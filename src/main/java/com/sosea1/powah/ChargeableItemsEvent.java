package com.sosea1.powah;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired when Powah gathers a player's chargeable item stacks.
 *
 * <p>Add extra stacks to {@link #getItems()} to let Player Transmitters and active Batteries
 * charge equipment provided by addon inventories that are not part of vanilla player slots.</p>
 */
public final class ChargeableItemsEvent extends Event {
    private final EntityPlayer player;
    private final List<ItemStack> items = new ArrayList<ItemStack>();

    public ChargeableItemsEvent(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
