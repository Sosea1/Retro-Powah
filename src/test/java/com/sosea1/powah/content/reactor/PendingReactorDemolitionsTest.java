package com.sosea1.powah.content.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

final class PendingReactorDemolitionsTest {
    @Test
    void requestsDeduplicateAndSurviveNbtRoundTrip() throws Exception {
        Class<?> type = loadTargetOrFail();
        Object data = newData(type);
        BlockPos core = new BlockPos(17, 64, -1);

        invoke(data, "add", new Class<?>[] {BlockPos.class}, core);
        invoke(data, "add", new Class<?>[] {BlockPos.class}, core);
        NBTTagCompound saved = (NBTTagCompound) invoke(data, "writeToNBT",
                new Class<?>[] {NBTTagCompound.class}, new NBTTagCompound());

        Object restored = newData(type);
        invoke(restored, "readFromNBT", new Class<?>[] {NBTTagCompound.class}, saved);
        Set<?> matchingChunk = (Set<?>) invoke(restored, "positionsInChunk",
                new Class<?>[] {int.class, int.class}, 1, -1);
        assertEquals(1, matchingChunk.size());
        assertTrue((Boolean) invoke(restored, "contains", new Class<?>[] {BlockPos.class}, core));
        assertTrue((Boolean) invoke(restored, "remove", new Class<?>[] {BlockPos.class}, core));
        assertFalse((Boolean) invoke(restored, "contains", new Class<?>[] {BlockPos.class}, core));
    }

    @Test
    void chunkFilterDoesNotReturnRequestsFromOtherChunks() throws Exception {
        Class<?> type = loadTargetOrFail();
        Object data = newData(type);
        BlockPos east = new BlockPos(17, 64, -1);
        BlockPos west = new BlockPos(-17, 64, -1);
        invoke(data, "add", new Class<?>[] {BlockPos.class}, east);
        invoke(data, "add", new Class<?>[] {BlockPos.class}, west);

        Set<?> matchingChunk = (Set<?>) invoke(data, "positionsInChunk",
                new Class<?>[] {int.class, int.class}, 1, -1);
        assertEquals(1, matchingChunk.size());
        assertTrue(matchingChunk.contains(east));
    }

    private static Class<?> loadTargetOrFail() {
        try {
            return Class.forName("com.sosea1.powah.content.reactor.PendingReactorDemolitions");
        } catch (ClassNotFoundException missing) {
            fail("PendingReactorDemolitions has not been implemented yet", missing);
            return Object.class;
        }
    }

    private static Object newData(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getConstructor(String.class);
        return constructor.newInstance("powah_test");
    }

    private static Object invoke(Object target, String name, Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }
}
