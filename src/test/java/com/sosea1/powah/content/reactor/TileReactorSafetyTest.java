package com.sosea1.powah.content.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.tier.PowahTier;
import net.minecraft.util.math.BlockPos;

final class TileReactorSafetyTest {
    @Test
    void invalidStructureEvidenceOverridesAnEarlierUnknownChunk() {
        assertEquals(TileReactor.StructureStatus.INVALID,
                TileReactor.StructureStatus.UNKNOWN.merge(TileReactor.StructureStatus.INVALID));
    }

    @Test
    void onlyTheFirstPartRemovalCanStartReactorDemolition() {
        TileReactor reactor = new TileReactor(PowahTier.STARTER);

        assertTrue(reactor.beginDemolition());
        assertFalse(reactor.beginDemolition());
    }

    @Test
    void originIsAValidBoundCorePosition() {
        TileReactorPart part = new TileReactorPart(PowahTier.STARTER);
        assertFalse(part.isCoreBound());
        part.bind(BlockPos.ORIGIN, false);
        assertTrue(part.isCoreBound());
        assertEquals(BlockPos.ORIGIN, part.getCorePos());
    }

    @Test
    void onlyRetryKeepsAPendingDemolitionRequest() throws Exception {
        Class<?> resultType;
        try {
            resultType = Class.forName(TileReactor.class.getName() + "$DemolitionResult");
        } catch (ClassNotFoundException missing) {
            fail("DemolitionResult has not been implemented yet", missing);
            return;
        }
        Method shouldKeepRequest = resultType.getDeclaredMethod("shouldKeepRequest");
        shouldKeepRequest.setAccessible(true);

        assertTrue((Boolean) shouldKeepRequest.invoke(enumValue(resultType, "RETRY")));
        assertFalse((Boolean) shouldKeepRequest.invoke(enumValue(resultType, "COMPLETE")));
        assertFalse((Boolean) shouldKeepRequest.invoke(enumValue(resultType, "STALE")));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> enumType, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumType, name);
    }
}
