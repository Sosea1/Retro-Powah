package com.sosea1.powah.content.reactor;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import com.sosea1.powah.Powah;

/** Persistent demolition intents for reactors whose core chunks are currently unloaded. */
public final class PendingReactorDemolitions extends WorldSavedData {
    private static final String DATA_NAME = Powah.MOD_ID + "_pending_reactor_demolitions";
    private static final String NBT_POSITIONS = "CorePositions";

    private final Set<Long> corePositions = new HashSet<Long>();

    public PendingReactorDemolitions(String name) {
        super(name);
    }

    public static PendingReactorDemolitions get(WorldServer world) {
        MapStorage storage = world.getPerWorldStorage();
        WorldSavedData existing = storage.getOrLoadData(PendingReactorDemolitions.class, DATA_NAME);
        if (existing instanceof PendingReactorDemolitions) {
            return (PendingReactorDemolitions) existing;
        }

        PendingReactorDemolitions created = new PendingReactorDemolitions(DATA_NAME);
        storage.setData(DATA_NAME, created);
        return created;
    }

    public boolean add(BlockPos corePos) {
        if (!corePositions.add(corePos.toLong())) return false;
        markDirty();
        return true;
    }

    public boolean remove(BlockPos corePos) {
        if (!corePositions.remove(corePos.toLong())) return false;
        markDirty();
        return true;
    }

    public boolean contains(BlockPos corePos) {
        return corePositions.contains(corePos.toLong());
    }

    public Set<BlockPos> positionsInChunk(int chunkX, int chunkZ) {
        Set<BlockPos> positions = new HashSet<BlockPos>();
        for (Long packedPos : corePositions) {
            BlockPos corePos = BlockPos.fromLong(packedPos.longValue());
            if ((corePos.getX() >> 4) == chunkX && (corePos.getZ() >> 4) == chunkZ) {
                positions.add(corePos);
            }
        }
        return positions;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        corePositions.clear();
        NBTTagList positions = compound.getTagList(NBT_POSITIONS, 4);
        for (int i = 0; i < positions.tagCount(); i++) {
            NBTBase tag = positions.get(i);
            if (tag instanceof NBTTagLong) {
                corePositions.add(((NBTTagLong) tag).getLong());
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList positions = new NBTTagList();
        for (Long corePos : corePositions) {
            positions.appendTag(new NBTTagLong(corePos.longValue()));
        }
        compound.setTag(NBT_POSITIONS, positions);
        return compound;
    }
}
