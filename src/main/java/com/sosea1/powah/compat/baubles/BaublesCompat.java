package com.sosea1.powah.compat.baubles;

import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import com.sosea1.powah.ChargeableItemsEvent;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Optional 1.12 counterpart of Powah's Curios charging integration. */
public final class BaublesCompat {
    private BaublesCompat() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new BaublesCompat());
    }

    @SubscribeEvent
    public void addBaubleStacks(ChargeableItemsEvent event) {
        if (event.getPlayer() == null || BaublesCapabilities.CAPABILITY_BAUBLES == null) return;
        IBaublesItemHandler inventory = event.getPlayer().getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (inventory == null) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && !stack.isEmpty()) {
                // Keep the original stack: charging must update the equipped item's capability.
                event.getItems().add(stack);
            }
        }
    }
}
