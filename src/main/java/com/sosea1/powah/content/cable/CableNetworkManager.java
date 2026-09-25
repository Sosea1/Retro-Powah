package com.sosea1.powah.content.cable;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/** Loaded-cable registry + lazy topology cache. No chunk loading and no per-tick cable traversal. */
final class CableNetworkManager {
    private static final Map<World, Map<Long, WeakReference<TileCable>>> LOADED_CABLES =
            new WeakHashMap<World, Map<Long, WeakReference<TileCable>>>();

    private CableNetworkManager() {
    }

    static void add(TileCable cable) {
        World world = cable.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        Map<Long, WeakReference<TileCable>> cables = LOADED_CABLES.get(world);
        if (cables == null) {
            cables = new HashMap<Long, WeakReference<TileCable>>();
            LOADED_CABLES.put(world, cables);
        }
        WeakReference<TileCable> previousRef = cables.put(
                cable.getPos().toLong(), new WeakReference<TileCable>(cable));
        TileCable previous = dereference(previousRef);
        if (previous != null && previous != cable) {
            invalidate(previous);
        }
        invalidateAround(cable);
    }

    static void remove(TileCable cable) {
        World world = cable.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        invalidate(cable);

        Map<Long, WeakReference<TileCable>> cables = LOADED_CABLES.get(world);
        if (cables == null) {
            return;
        }
        long key = cable.getPos().toLong();
        TileCable current = dereference(cables.get(key));
        if (current == null || current == cable) {
            cables.remove(key);
        }
        if (cables.isEmpty()) {
            LOADED_CABLES.remove(world);
        } else {
            invalidateAround(cable);
        }
    }

    static void neighborChanged(TileCable cable) {
        invalidate(cable);
        invalidateAround(cable);
    }

    static CableNetwork networkFor(TileCable start) {
        CableNetwork cached = start.getNetworkIfPresent();
        World world = start.getWorld();
        if (world == null || world.isRemote) {
            return new CableNetwork(new ArrayList<TileCable>(), new ArrayList<CableEndpoint>(), 0L);
        }
        if (cached != null) {
            long age = world.getTotalWorldTime() - cached.builtAt;
            if (age >= 0L && age <= 100L) {
                return cached;
            }
            // A bounded lazy refresh recovers from missed lifecycle/neighbor callbacks and from
            // external machines changing their exposed FE capability without a block update.
            cached.invalidate();
        }

        Map<Long, WeakReference<TileCable>> loaded = LOADED_CABLES.get(world);
        if (loaded == null) {
            add(start);
            loaded = LOADED_CABLES.get(world);
        }
        if (loaded == null) {
            return new CableNetwork(new ArrayList<TileCable>(), new ArrayList<CableEndpoint>(),
                    world.getTotalWorldTime());
        }

        Set<TileCable> members = new LinkedHashSet<TileCable>();
        ArrayDeque<TileCable> queue = new ArrayDeque<TileCable>();
        members.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            TileCable current = queue.removeFirst();
            for (EnumFacing direction : EnumFacing.values()) {
                TileCable adjacent = findCable(world, loaded, current.getPos().offset(direction));
                if (adjacent != null && current.canConnectTo(adjacent) && members.add(adjacent)) {
                    queue.addLast(adjacent);
                }
            }
        }

        Set<CableEndpoint> endpoints = new LinkedHashSet<CableEndpoint>();
        for (TileCable cable : members) {
            for (EnumFacing direction : EnumFacing.values()) {
                BlockPos targetPos = cable.getPos().offset(direction);
                TileCable adjacentCable = findCable(world, loaded, targetPos);
                if (adjacentCable != null) {
                    continue;
                }
                if (!world.isBlockLoaded(targetPos)) {
                    continue;
                }
                TileEntity tile = world.getTileEntity(targetPos);
                if (tile == null || tile instanceof TileCable) {
                    continue;
                }
                EnumFacing targetSide = direction.getOpposite();
                if (!tile.hasCapability(CapabilityEnergy.ENERGY, targetSide)) {
                    continue;
                }
                IEnergyStorage storage = tile.getCapability(CapabilityEnergy.ENERGY, targetSide);
                if (storage != null) {
                    // Cache physical capability presence, not the current transfer mode. Side configuration
                    // may change without a block update; both routing directions re-check it dynamically.
                    endpoints.add(new CableEndpoint(cable.getPos(), direction, targetPos, targetSide));
                }
            }
        }

        CableNetwork network = new CableNetwork(new ArrayList<TileCable>(members),
                new ArrayList<CableEndpoint>(endpoints), world.getTotalWorldTime());
        for (TileCable cable : members) {
            cable.setNetwork(network);
        }
        return network;
    }

    private static TileCable getLoaded(Map<Long, WeakReference<TileCable>> loaded, long key) {
        WeakReference<TileCable> reference = loaded.get(key);
        TileCable cable = dereference(reference);
        if (reference != null && cable == null) {
            loaded.remove(key);
        }
        return cable;
    }

    /**
     * Reconciles the disposable registry with the world's authoritative loaded TileEntity map.
     * Forge/Cleanroom can invoke cable onLoad callbacks in a different order while a save is
     * reopened. Depending only on the callback-built registry then produces a partial network
     * which remains cached until a block update. This lookup never loads a chunk; it merely
     * repairs the registry while the topology is already being walked.
     */
    private static TileCable findCable(World world, Map<Long, WeakReference<TileCable>> loaded,
                                       BlockPos pos) {
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        long key = pos.toLong();
        TileEntity raw = world.getTileEntity(pos);
        if (raw instanceof TileCable) {
            TileCable cable = (TileCable) raw;
            TileCable registered = getLoaded(loaded, key);
            if (registered != cable) {
                loaded.put(key, new WeakReference<TileCable>(cable));
            }
            return cable;
        }
        loaded.remove(key);
        return null;
    }

    private static TileCable dereference(WeakReference<TileCable> reference) {
        return reference == null ? null : reference.get();
    }

    private static void invalidate(TileCable cable) {
        CableNetwork network = cable.getNetworkIfPresent();
        if (network != null) {
            network.invalidate();
        }
        cable.setNetwork(null);
    }

    private static void invalidateAround(TileCable cable) {
        World world = cable.getWorld();
        Map<Long, WeakReference<TileCable>> loaded = world == null ? null : LOADED_CABLES.get(world);
        if (loaded == null) {
            return;
        }
        for (EnumFacing direction : EnumFacing.values()) {
            TileCable adjacent = getLoaded(loaded, cable.getPos().offset(direction).toLong());
            if (adjacent != null) {
                invalidate(adjacent);
            }
        }
    }
}
