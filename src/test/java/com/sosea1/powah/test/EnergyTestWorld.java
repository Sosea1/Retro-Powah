package com.sosea1.powah.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.MapStorage;

/** Loaded-tile world fixture; any accidental chunk access still fails. */
public final class EnergyTestWorld extends World {
    static { Bootstrap.register(); }
    private final Map<BlockPos, TileEntity> tiles = new HashMap<BlockPos, TileEntity>();
    private final Set<BlockPos> unloaded = new HashSet<BlockPos>();
    private final MapStorage savedData = new MapStorage(null);
    private final IBlockState air = new Block(Material.AIR).getDefaultState();

    public EnergyTestWorld() {
        super(null, null, new WorldProviderSurface(), new Profiler(), false);
    }

    public void put(BlockPos pos, TileEntity tile) {
        tile.setPos(pos);
        tile.setWorld(this);
        tiles.put(pos, tile);
    }

    public void unload(BlockPos pos) {
        unloaded.add(pos);
    }

    @Override public boolean isBlockLoaded(BlockPos pos) { return !unloaded.contains(pos); }
    @Override public TileEntity getTileEntity(BlockPos pos) { return tiles.get(pos); }
    @Override public MapStorage getMapStorage() { return savedData; }
    @Override public IBlockState getBlockState(BlockPos pos) { return air; }
    @Override public long getTotalWorldTime() { return 0L; }
    @Override public void markChunkDirty(BlockPos pos, TileEntity tile) { }
    @Override public void updateComparatorOutputLevel(BlockPos pos, Block block) { }
    @Override public void notifyBlockUpdate(BlockPos pos, IBlockState before, IBlockState after, int flags) { }
    @Override public void markBlockRangeForRenderUpdate(BlockPos from, BlockPos to) { }
    @Override protected IChunkProvider createChunkProvider() { return null; }
    @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
}
