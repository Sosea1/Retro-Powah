package com.sosea1.powah.content.ender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.ender.EnderNetworkData;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.test.EnergyReceiverTile;
import com.sosea1.powah.test.EnergyTestWorld;

final class EnderThroughputTest {
    @Test
    void enderCellOutputLimitAppliesToEachSideWithoutCopyingNetworkEnergy() {
        EnergyTestWorld world = new EnergyTestWorld();
        BlockPos pos = new BlockPos(0, 64, 0);
        TileEnderCell cell = new TileEnderCell(PowahTier.STARTER);
        EnergyReceiverTile east = new EnergyReceiverTile(), west = new EnergyReceiverTile();
        world.put(pos, cell);
        world.put(pos.east(), east);
        world.put(pos.west(), west);
        UUID owner = UUID.randomUUID();
        EnderNetworkData network = EnderNetworkData.get(world);
        network.extend(owner, 0, 10_000L, 3_000L);
        cell.claim(owner, "Alice");

        cell.update();

        assertEquals(1_000, east.storage.getEnergyStored());
        assertEquals(1_000, west.storage.getEnergyStored());
        assertEquals(1_000L, network.channel(owner, 0).energy());
        assertEquals(1_000L, cell.getEnergyBuffer().energy());
    }
}
