package com.sosea1.powah.content.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import com.sosea1.powah.Powah;

/** Performs one deferred orphan check for reactor parts when relevant chunks load. */
@Mod.EventBusSubscriber(modid = Powah.MOD_ID)
public final class ReactorPartLifecycle {
    private static final Map<WorldServer, PendingChecks> WORLDS =
            new WeakHashMap<WorldServer, PendingChecks>();

    private ReactorPartLifecycle() { }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.getWorld();
        Chunk chunk = event.getChunk();
        PendingChecks pending = state(world);

        for (TileEntity tile : chunk.getTileEntityMap().values()) {
            if (tile instanceof TileReactorPart) {
                pending.schedule((TileReactorPart) tile, world.getTotalWorldTime() + 1L);
            }
        }
        ChunkPos loadedChunk = chunk.getPos();
        pending.wakeCoreChunk(ChunkPos.asLong(loadedChunk.x, loadedChunk.z),
                world.getTotalWorldTime() + 1L);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        PendingChecks pending = WORLDS.get(world);
        if (pending != null) {
            pending.process(world, world.getTotalWorldTime());
            if (pending.isEmpty()) WORLDS.remove(world);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof WorldServer) {
            WORLDS.remove((WorldServer) event.getWorld());
        }
    }

    private static PendingChecks state(WorldServer world) {
        PendingChecks pending = WORLDS.get(world);
        if (pending == null) {
            pending = new PendingChecks();
            WORLDS.put(world, pending);
        }
        return pending;
    }

    private static final class PendingChecks {
        private final Set<BlockPos> pendingPositions = new HashSet<BlockPos>();
        private final Map<Long, Set<BlockPos>> dueByTick = new HashMap<Long, Set<BlockPos>>();
        private final Map<Long, Set<BlockPos>> waitingByCoreChunk = new HashMap<Long, Set<BlockPos>>();

        private void schedule(TileReactorPart part, long dueTick) {
            BlockPos partPos = part.getPos().toImmutable();
            if (pendingPositions.add(partPos)) {
                dueByTick.computeIfAbsent(dueTick, ignored -> new HashSet<BlockPos>()).add(partPos);
            }
        }

        private void wakeCoreChunk(long chunkKey, long dueTick) {
            Set<BlockPos> waiting = waitingByCoreChunk.remove(chunkKey);
            if (waiting == null) return;
            dueByTick.computeIfAbsent(dueTick, ignored -> new HashSet<BlockPos>()).addAll(waiting);
        }

        private void process(WorldServer world, long currentTick) {
            List<BlockPos> readyPositions = new ArrayList<BlockPos>();
            Iterator<Map.Entry<Long, Set<BlockPos>>> due = dueByTick.entrySet().iterator();
            while (due.hasNext()) {
                Map.Entry<Long, Set<BlockPos>> entry = due.next();
                if (entry.getKey().longValue() > currentTick) continue;
                readyPositions.addAll(entry.getValue());
                due.remove();
            }

            // World callbacks below can synchronously load another chunk and enqueue
            // more work. Drain the due map first so nested events cannot mutate its iterator.
            for (BlockPos partPos : readyPositions) {
                if (!world.isBlockLoaded(partPos)) {
                    pendingPositions.remove(partPos);
                    continue;
                }
                TileEntity rawPart = world.getTileEntity(partPos);
                if (!(rawPart instanceof TileReactorPart)) {
                    pendingPositions.remove(partPos);
                    continue;
                }

                BlockPos corePos = ((TileReactorPart) rawPart).getCorePos();
                if (!world.isBlockLoaded(corePos)) {
                    ChunkPos coreChunk = new ChunkPos(corePos);
                    waitingByCoreChunk.computeIfAbsent(ChunkPos.asLong(coreChunk.x, coreChunk.z),
                            ignored -> new HashSet<BlockPos>()).add(partPos);
                    continue;
                }

                if (!(world.getTileEntity(corePos) instanceof TileReactor)
                        && world.getBlockState(partPos).getBlock() instanceof BlockReactorPart) {
                    // Flags 2 and 16 update clients while suppressing neighbor and
                    // observer walks, which can touch unloaded chunks in 1.12.
                    world.setBlockState(partPos, Blocks.AIR.getDefaultState(), 2 | 16);
                }
                pendingPositions.remove(partPos);
            }
        }

        private boolean isEmpty() {
            return pendingPositions.isEmpty();
        }
    }
}
