package com.sosea1.powah.common.ender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

final class EnderNetworkDataTest {
    @Test
    void persistenceKeepsLongEnergyIsolatedByOwnerAndChannel() {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        EnderNetworkData network = new EnderNetworkData();
        assertTrue(network.extend(alice, 11, 8_000_000_000L, 5_000_000_000L));
        assertTrue(network.extend(alice, 0, 1_000L, 700L));
        assertTrue(network.extend(bob, 11, 2_000L, 900L));

        EnderNetworkData restored = new EnderNetworkData();
        restored.readFromNBT(network.writeToNBT(new NBTTagCompound()));
        assertEquals(8_000_000_000L, restored.channel(alice, 11).capacity());
        assertEquals(5_000_000_000L, restored.channel(alice, 11).energy());
        assertEquals(700L, restored.channel(alice, 0).energy());
        assertEquals(900L, restored.channel(bob, 11).energy());
        assertEquals(0L, restored.channel(bob, 0).energy());
        restored.readFromNBT(network.writeToNBT(new NBTTagCompound()));
        assertEquals(8_000_000_000L, restored.channel(alice, 11).capacity());
    }

    @Test
    void simulationsDoNotMoveEnergyOrMarkDataDirty() {
        UUID owner = UUID.randomUUID();
        EnderNetworkData network = new EnderNetworkData();
        network.extend(owner, 0, 1_000L, 500L);
        network.setDirty(false);
        assertEquals(100L, network.receive(owner, 0, 300L, 100L, true));
        assertEquals(200L, network.extract(owner, 0, 300L, 200L, true));
        assertEquals(500L, network.channel(owner, 0).energy());
        assertEquals(false, network.isDirty());
        assertEquals(100L, network.receive(owner, 0, 300L, 100L, false));
        assertEquals(600L, network.channel(owner, 0).energy());
        assertTrue(network.isDirty());
    }
}
