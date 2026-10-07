package com.sosea1.powah.common.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Method;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.reactor.TileReactor;
import com.sosea1.powah.test.EnergyReceiverTile;
import com.sosea1.powah.test.EnergyTestWorld;

final class EnergyThroughputTest {
    @Test
    void rejectedEnergyIsRefundedAndDisabledOrUnloadedOutputsAreSkipped() {
        EnergyTestWorld world = new EnergyTestWorld();
        Sender sender = new Sender();
        BlockPos pos = new BlockPos(0, 64, 0);
        EnergyReceiverTile east = new EnergyReceiverTile(250), west = new EnergyReceiverTile();
        EnergyReceiverTile north = new EnergyReceiverTile();
        world.put(pos, sender);
        world.put(pos.east(), east);
        world.put(pos.west(), west);
        world.put(pos.north(), north);
        world.unload(pos.north());
        sender.setSideMode(EnumFacing.WEST, EnergyPortMode.NONE);
        sender.getEnergyBuffer().setEnergy(3_000L);

        assertEquals(250L, sender.send(1_000L));
        assertEquals(2_750L, sender.getEnergyBuffer().energy());
        assertEquals(250, east.storage.getEnergyStored());
        assertEquals(0, west.storage.getEnergyStored());
        assertEquals(0, north.storage.getEnergyStored());
    }

    @Test
    void machineTransferLimitAppliesToEachOutputSide() {
        EnergyTestWorld world = new EnergyTestWorld();
        Sender sender = new Sender();
        BlockPos pos = new BlockPos(0, 64, 0);
        EnergyReceiverTile east = new EnergyReceiverTile(), west = new EnergyReceiverTile();
        world.put(pos, sender);
        world.put(pos.east(), east);
        world.put(pos.west(), west);
        sender.getEnergyBuffer().setEnergy(3_000L);

        assertEquals(2_000L, sender.send(1_000L));
        assertEquals(1_000, east.storage.getEnergyStored());
        assertEquals(1_000, west.storage.getEnergyStored());
        assertEquals(1_000L, sender.getEnergyBuffer().energy());
    }

    @Test
    void reactorTransferLimitAppliesToEachExtractor() throws Exception {
        EnergyTestWorld world = new EnergyTestWorld();
        TileReactor reactor = new TileReactor(PowahTier.STARTER);
        BlockPos pos = new BlockPos(0, 64, 0);
        EnergyReceiverTile east = new EnergyReceiverTile(), west = new EnergyReceiverTile();
        world.put(pos, reactor);
        world.put(pos.east(2), east);
        world.put(pos.west(2), west);
        reactor.getEnergyBuffer().setEnergy(3_000L);
        Method push = TileReactor.class.getDeclaredMethod("pushReactorEnergy", long.class);
        push.setAccessible(true);

        assertEquals(2_000L, push.invoke(reactor, 1_000L));
        assertEquals(1_000, east.storage.getEnergyStored());
        assertEquals(1_000, west.storage.getEnergyStored());
        assertEquals(1_000L, reactor.getEnergyBuffer().energy());
    }

    private static final class Sender extends AbstractEnergyTile {
        Sender() { super(PowahTier.STARTER, 10_000L, 0L, 1_000L, EnergyPortMode.OUTPUT, EnergyPortMode.OUTPUT); }
        long send(long limit) { return pushEnergyToAdjacent(limit); }
        @Override protected long getCapacityForTier(PowahTier tier) { return 10_000L; }
        @Override protected long getMaxReceiveForTier(PowahTier tier) { return 0L; }
        @Override protected long getMaxExtractForTier(PowahTier tier) { return 1_000L; }
    }
}
