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
import net.minecraft.world.World;
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
        long chunkKey = ChunkPos.asLong(loadedChunk.x, loadedChunk.z);
        pending.wakeCoreChunk(chunkKey, world.getTotalWorldTime() + 1L);
        for (BlockPos corePos : PendingReactorDemolitions.get(world).positionsInChunk(loadedChunk.x, loadedChunk.z)) {
            pending.scheduleDemolition(corePos, world.getTotalWorldTime() + 1L);
        }
    }

    /** Handles non-player removals without ever forcing the linked core chunk to load. */
    public static void requestDemolition(World world, BlockPos corePos) {
        if (world == null || world.isRemote || !(world instanceof WorldServer) || corePos == null) return;
        WorldServer serverWorld = (WorldServer) world;
        if (serverWorld.isBlockLoaded(corePos)) {
            TileEntity raw = serverWorld.getTileEntity(corePos);
            if (!(raw instanceof TileReactor)) return;
            TileReactor.DemolitionResult result = ((TileReactor) raw).tryDemolitionFromPart();
            if (result != TileReactor.DemolitionResult.RETRY) return;
        }

        // Persist intent before scheduling transient work so a server shutdown
        // between chunk unload and processing cannot lose the demolition.
        PendingReactorDemolitions.get(serverWorld).add(corePos);
        state(serverWorld).scheduleDemolition(corePos, serverWorld.getTotalWorldTime() + 1L);
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
        private final Set<BlockPos> pendingDemolitions = new HashSet<BlockPos>();
        private final Map<Long, Set<BlockPos>> demolitionsByTick = new HashMap<Long, Set<BlockPos>>();

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

        private void scheduleDemolition(BlockPos corePos, long dueTick) {
            BlockPos immutablePos = corePos.toImmutable();
            if (pendingDemolitions.add(immutablePos)) {
                demolitionsByTick.computeIfAbsent(dueTick, ignored -> new HashSet<BlockPos>()).add(immutablePos);
            }
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

                TileReactorPart part = (TileReactorPart) rawPart;
                if (!part.isCoreBound()) {
                    if (world.getBlockState(partPos).getBlock() instanceof BlockReactorPart) {
                        world.setBlockState(partPos, Blocks.AIR.getDefaultState(), 2 | 16);
                    }
                    pendingPositions.remove(partPos);
                    continue;
                }

                BlockPos corePos = part.getCorePos();
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

            List<BlockPos> readyDemolitions = new ArrayList<BlockPos>();
            Iterator<Map.Entry<Long, Set<BlockPos>>> demolitions = demolitionsByTick.entrySet().iterator();
            while (demolitions.hasNext()) {
                Map.Entry<Long, Set<BlockPos>> entry = demolitions.next();
                if (entry.getKey().longValue() > currentTick) continue;
                readyDemolitions.addAll(entry.getValue());
                demolitions.remove();
            }

            PendingReactorDemolitions saved = null;
            for (BlockPos corePos : readyDemolitions) {
                if (!world.isBlockLoaded(corePos)) {
                    pendingDemolitions.remove(corePos);
                    continue; // The saved intent is re-queued by the next ChunkEvent.Load.
                }
                TileEntity raw = world.getTileEntity(corePos);
                if (!(raw instanceof TileReactor)) {
                    if (saved == null) saved = PendingReactorDemolitions.get(world);
                    saved.remove(corePos);
                    pendingDemolitions.remove(corePos);
                    continue;
                }
                TileReactor.DemolitionResult result = ((TileReactor) raw).tryDemolitionFromPart();
                if (result.shouldKeepRequest()) {
                    scheduleDemolition(corePos, currentTick + 20L);
                } else {
                    if (saved == null) saved = PendingReactorDemolitions.get(world);
                    saved.remove(corePos);
                    pendingDemolitions.remove(corePos);
                }
            }
        }

        private boolean isEmpty() {
            return pendingPositions.isEmpty() && pendingDemolitions.isEmpty();
        }
    }
}
