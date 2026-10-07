package com.sosea1.powah.content.cable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.test.EnergyReceiverTile;
import com.sosea1.powah.test.EnergyTestWorld;

final class CableSelectionTest {
    @Test
    void lShapedCableDoesNotCaptureRayThroughEmptyCorner() {
        EnergyTestWorld world = new EnergyTestWorld();
        BlockCable cable = new BlockCable(PowahTier.STARTER);
        BlockPos pos = new BlockPos(0, 64, 0);
        world.put(pos.east(), new EnergyReceiverTile());
        world.put(pos.south(), new EnergyReceiverTile());

        assertNull(cable.collisionRayTrace(cable.getDefaultState(), world, pos,
                new Vec3d(0.8D, 66D, 0.8D), new Vec3d(0.8D, 63D, 0.8D)));
        assertNotNull(cable.collisionRayTrace(cable.getDefaultState(), world, pos,
                new Vec3d(0.5D, 66D, 0.5D), new Vec3d(0.5D, 63D, 0.5D)));
        assertNotNull(cable.collisionRayTrace(cable.getDefaultState(), world, pos,
                new Vec3d(0.8D, 66D, 0.5D), new Vec3d(0.8D, 63D, 0.5D)));
    }

    @Test
    void selectionDoesNotQueryUnloadedNeighbor() {
        EnergyTestWorld world = new EnergyTestWorld();
        BlockCable cable = new BlockCable(PowahTier.STARTER);
        BlockPos pos = new BlockPos(0, 64, 0);
        world.put(pos.east(), new EnergyReceiverTile());
        world.unload(pos.east());
        assertFalse(cable.getActualState(cable.getDefaultState(), world, pos).getValue(BlockCable.EAST));
    }
}
