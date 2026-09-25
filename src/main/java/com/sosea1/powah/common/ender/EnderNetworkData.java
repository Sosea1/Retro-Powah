package com.sosea1.powah.common.ender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/** Server-global persistent owner/channel storage. No endpoint registry and no world scanning. */
public final class EnderNetworkData extends WorldSavedData {
    public static final String DATA_NAME = "powah_ender_network";
    public static final int MAX_CHANNELS = 12;
    private static final String NBT_OWNERS = "Owners";
    private static final String NBT_OWNER = "Owner";
    private static final String NBT_CHANNELS = "Channels";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_CAPACITY = "Capacity";

    private final Map<UUID, EnderChannelState[]> owners = new HashMap<UUID, EnderChannelState[]>();

    public EnderNetworkData() { super(DATA_NAME); }
    public EnderNetworkData(String name) { super(name); }

    public static EnderNetworkData get(World world) {
        if (world == null) throw new NullPointerException("world");
        MapStorage storage = world.getMapStorage();
        if (storage == null) throw new IllegalStateException("World has no global MapStorage");
        EnderNetworkData data = (EnderNetworkData) storage.getOrLoadData(EnderNetworkData.class, DATA_NAME);
        if (data == null) {
            data = new EnderNetworkData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public EnderChannelState channel(UUID owner, int channel) {
        if (owner == null) throw new NullPointerException("owner");
        checkChannel(channel);
        EnderChannelState[] channels = owners.get(owner);
        if (channels == null) {
            channels = createChannels();
            owners.put(owner, channels);
        }
        return channels[channel];
    }

    public long receive(UUID owner, int channel, long amount, long transfer, boolean simulate) {
        EnderChannelState state = channel(owner, channel);
        long moved = state.receive(amount, transfer, simulate);
        if (!simulate && moved > 0L) markDirty();
        return moved;
    }

    public long extract(UUID owner, int channel, long amount, long transfer, boolean simulate) {
        EnderChannelState state = channel(owner, channel);
        long moved = state.extract(amount, transfer, simulate);
        if (!simulate && moved > 0L) markDirty();
        return moved;
    }

    public boolean extend(UUID owner, int channel, long capacity, long importedEnergy) {
        if (capacity <= 0L) return false;
        boolean extended = channel(owner, channel).extend(capacity, importedEnergy);
        if (extended) markDirty();
        return extended;
    }

    private static EnderChannelState[] createChannels() {
        EnderChannelState[] channels = new EnderChannelState[MAX_CHANNELS];
        for (int i = 0; i < channels.length; i++) channels[i] = new EnderChannelState();
        return channels;
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= MAX_CHANNELS) throw new IllegalArgumentException("channel out of range: " + channel);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        owners.clear();
        NBTTagList ownerList = compound.getTagList(NBT_OWNERS, 10);
        for (int i = 0; i < ownerList.tagCount(); i++) {
            NBTTagCompound ownerTag = ownerList.getCompoundTagAt(i);
            String ownerText = ownerTag.getString(NBT_OWNER);
            if (ownerText == null || ownerText.isEmpty()) continue;
            UUID owner;
            try { owner = UUID.fromString(ownerText); } catch (IllegalArgumentException ignored) { continue; }
            EnderChannelState[] channels = createChannels();
            NBTTagList channelList = ownerTag.getTagList(NBT_CHANNELS, 10);
            int count = Math.min(MAX_CHANNELS, channelList.tagCount());
            for (int c = 0; c < count; c++) {
                NBTTagCompound channelTag = channelList.getCompoundTagAt(c);
                channels[c].restore(Math.max(0L, channelTag.getLong(NBT_ENERGY)), Math.max(0L, channelTag.getLong(NBT_CAPACITY)));
            }
            owners.put(owner, channels);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList ownerList = new NBTTagList();
        for (Map.Entry<UUID, EnderChannelState[]> entry : owners.entrySet()) {
            NBTTagCompound ownerTag = new NBTTagCompound();
            ownerTag.setString(NBT_OWNER, entry.getKey().toString());
            NBTTagList channelList = new NBTTagList();
            for (EnderChannelState state : entry.getValue()) {
                NBTTagCompound channelTag = new NBTTagCompound();
                channelTag.setLong(NBT_ENERGY, state.energy());
                channelTag.setLong(NBT_CAPACITY, state.capacity());
                channelList.appendTag(channelTag);
            }
            ownerTag.setTag(NBT_CHANNELS, channelList);
            ownerList.appendTag(ownerTag);
        }
        compound.setTag(NBT_OWNERS, ownerList);
        return compound;
    }
}
