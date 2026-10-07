package com.sosea1.powah.content.ender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.tier.PowahTier;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.registries.GameData;

final class EnderPortableStateTest {
    static { Bootstrap.register(); }
    private static final UUID OWNER = UUID.fromString("a8b63107-a3bf-4d13-9d85-c9a9d0726e2b");
    private static final UUID OTHER_PLAYER = UUID.fromString("70bfb328-18d1-41cc-a34a-7b7f4c0e5fa5");
    private final PortableProbe block = new PortableProbe();

    @Test
    void cellRetainsOwnerNameAndChannelAcrossPortableRoundTrip() {
        assertRoundTrip(new TileEnderCell(PowahTier.NITRO), new TileEnderCell(PowahTier.NITRO));
    }

    @Test
    void gateRetainsOwnerNameAndChannelAcrossPortableRoundTrip() {
        assertRoundTrip(new TileEnderGate(PowahTier.NITRO), new TileEnderGate(PowahTier.NITRO));
    }

    @Test
    void legacyStackWithoutDataCanStillBeClaimedByItsPlacer() {
        TileEnderCell placed = new TileEnderCell(PowahTier.STARTER);
        block.restore(headlessStack(), placed);
        assertNull(placed.getOwner());
        assertEquals(0, placed.getChannel());

        placed.claim(OTHER_PLAYER, "New owner");
        assertTrue(placed.isOwner(OTHER_PLAYER));
        assertEquals("New owner", placed.getOwnerName());
    }

    @Test
    void normalDropListAddsOnePortableBlockWithoutChangingEarlierDrops() {
        TileEnderCell original = new TileEnderCell(PowahTier.NITRO);
        original.claim(OWNER, "Alice");
        original.setChannel(3);
        Item item = new ItemBlock(block);
        Item previous = GameData.getBlockItemMap().put(block, item);
        try {
            NonNullList<ItemStack> drops = NonNullList.create();
            ItemStack earlierDrop = new ItemStack(Items.DIAMOND);
            drops.add(earlierDrop);

            block.getDrops(drops, new EndpointWorld(original), BlockPos.ORIGIN, block.getDefaultState(), 0);

            assertEquals(2, drops.size());
            assertNull(earlierDrop.getTagCompound());
            assertEquals(item, drops.get(1).getItem());
            assertEquals(OWNER.toString(), drops.get(1).getTagCompound().getString("EnderOwner"));
            assertEquals(3, drops.get(1).getTagCompound().getInteger("EnderChannel"));
        } finally {
            if (previous == null) GameData.getBlockItemMap().remove(block);
            else GameData.getBlockItemMap().put(block, previous);
        }
    }

    @Test
    void placementRestoresPortableIdentityThroughTheCommonBlockHook() {
        TileEnderGate original = new TileEnderGate(PowahTier.NITRO);
        original.claim(OWNER, "Alice");
        original.setChannel(3);
        ItemStack stack = headlessStack();
        block.store(stack, original);
        TileEnderGate placed = new TileEnderGate(PowahTier.NITRO);

        block.onBlockPlacedBy(new EndpointWorld(placed), BlockPos.ORIGIN, block.getDefaultState(), null, stack);

        assertEquals(OWNER, placed.getOwner());
        assertEquals("Alice", placed.getOwnerName());
        assertEquals(3, placed.getChannel());
    }

    @Test
    void malformedOwnerDoesNotClaimAPlayersNetworkAndChannelIsClamped() {
        ItemStack stack = headlessStack();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("EnderOwner", "invalid-uuid");
        tag.setString("EnderOwnerName", "Old name");
        tag.setInteger("EnderChannel", Integer.MAX_VALUE);
        stack.setTagCompound(tag);
        TileEnderCell placed = new TileEnderCell(PowahTier.NITRO);

        block.restore(stack, placed);

        assertNull(placed.getOwner());
        assertEquals(11, placed.getChannel());
        placed.claim(OTHER_PLAYER, "New owner");
        assertTrue(placed.isOwner(OTHER_PLAYER));
        assertEquals("New owner", placed.getOwnerName());
    }

    private void assertRoundTrip(AbstractEnderTile original, AbstractEnderTile placed) {
        original.claim(OWNER, "Alice");
        assertTrue(original.setChannel(3));
        ItemStack dropped = headlessStack();
        block.store(dropped, original);
        NBTTagCompound tag = dropped.getTagCompound();
        assertTrue(tag != null, "Ender portable state was lost on breaking");
        assertFalse(tag.hasKey("PowahEnergy"), "Shared network energy must stay in WorldSavedData");
        assertFalse(tag.hasKey("EnderInventory"), "Inventory drops must not also be copied into the block item");
        ItemStack carried = headlessStack();
        carried.setTagCompound(tag.copy());

        block.restore(carried, placed);
        placed.claim(OTHER_PLAYER, "Bob");

        assertEquals(OWNER, placed.getOwner());
        assertEquals("Alice", placed.getOwnerName());
        assertEquals(3, placed.getChannel());
        assertTrue(placed.isOwner(OWNER));
        assertFalse(placed.isOwner(OTHER_PLAYER));
    }

    // No registry bootstrap is needed to exercise a stack's real NBT container.
    private static ItemStack headlessStack() { return new ItemStack((Item) null); }

    private static final class PortableProbe extends BlockEnderMachine {
        PortableProbe() { super(PowahTier.STARTER); }
        @Override protected TileEntity createPowahTile(PowahTier tier) { return new TileEnderCell(tier); }
        void store(ItemStack stack, TileEntity tile) { writePortableState(stack, tile); }
        void restore(ItemStack stack, TileEntity tile) { readPortableState(stack, tile); }
    }

    private static final class EndpointWorld extends World {
        private final TileEntity endpoint;
        EndpointWorld(TileEntity endpoint) {
            super(null, new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false);
            this.endpoint = endpoint;
        }
        @Override protected IChunkProvider createChunkProvider() { throw new AssertionError("Must not load chunks"); }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        @Override public TileEntity getTileEntity(BlockPos pos) { return endpoint; }
    }
}
