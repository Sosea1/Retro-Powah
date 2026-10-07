package com.sosea1.powah.content.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Method;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.test.EnergyReceiverTile;
import com.sosea1.powah.test.EnergyTestWorld;

final class CableThroughputTest {
    @Test
    void networkDistributesInputAcrossSeparatelyLimitedOutputs() throws Exception {
        EnergyTestWorld world = new EnergyTestWorld();
        BlockPos pos = new BlockPos(1, 64, 0);
        TileCable ingress = new TileCable(PowahTier.NITRO), next = new TileCable(PowahTier.NITRO);
        EnergyReceiverTile first = new EnergyReceiverTile(), second = new EnergyReceiverTile();
        world.put(pos, ingress);
        world.put(pos.east(), next);
        world.put(pos.south(), first);
        world.put(pos.east().south(), second);
        Method receive = TileCable.class.getDeclaredMethod("routeEnergyIn", int.class, boolean.class, EnumFacing.class);
        receive.setAccessible(true);

        assertEquals(2_000_000, receive.invoke(ingress, 2_000_000, true, EnumFacing.WEST));
        assertEquals(0, first.storage.getEnergyStored());
        assertEquals(0, second.storage.getEnergyStored());
        assertEquals(2_000_000, receive.invoke(ingress, 2_000_000, false, EnumFacing.WEST));
        assertEquals(1_000_000, first.storage.getEnergyStored());
        assertEquals(1_000_000, second.storage.getEnergyStored());
    }
}
