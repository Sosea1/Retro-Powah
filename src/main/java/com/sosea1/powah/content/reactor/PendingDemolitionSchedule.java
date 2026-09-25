package com.sosea1.powah.content.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;

/** Transient due-tick queue for persisted reactor demolition intents. */
final class PendingDemolitionSchedule {
    private final Set<BlockPos> scheduledPositions = new HashSet<BlockPos>();
    private final Map<Long, Set<BlockPos>> dueByTick = new HashMap<Long, Set<BlockPos>>();

    boolean schedule(BlockPos corePos, long dueTick) {
        BlockPos immutablePos = corePos.toImmutable();
        if (!scheduledPositions.add(immutablePos)) return false;
        addDue(immutablePos, dueTick);
        return true;
    }

    void reschedule(BlockPos corePos, long dueTick) {
        BlockPos immutablePos = corePos.toImmutable();
        scheduledPositions.add(immutablePos);
        addDue(immutablePos, dueTick);
    }

    List<BlockPos> drainDue(long currentTick) {
        List<BlockPos> readyPositions = new ArrayList<BlockPos>();
        Iterator<Map.Entry<Long, Set<BlockPos>>> due = dueByTick.entrySet().iterator();
        while (due.hasNext()) {
            Map.Entry<Long, Set<BlockPos>> entry = due.next();
            if (entry.getKey().longValue() > currentTick) continue;
            readyPositions.addAll(entry.getValue());
            due.remove();
        }
        return readyPositions;
    }

    /** Clears only transient scheduling state; the persistent request remains saved. */
    void defer(BlockPos corePos) {
        scheduledPositions.remove(corePos);
    }

    void remove(BlockPos corePos) {
        scheduledPositions.remove(corePos);
        Iterator<Map.Entry<Long, Set<BlockPos>>> due = dueByTick.entrySet().iterator();
        while (due.hasNext()) {
            Map.Entry<Long, Set<BlockPos>> entry = due.next();
            entry.getValue().remove(corePos);
            if (entry.getValue().isEmpty()) due.remove();
        }
    }

    boolean isEmpty() {
        return scheduledPositions.isEmpty();
    }

    private void addDue(BlockPos corePos, long dueTick) {
        dueByTick.computeIfAbsent(dueTick, ignored -> new HashSet<BlockPos>()).add(corePos);
    }
}
