package com.sosea1.powah.common.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.tier.PowahTier;

final class LongEnergyPersistenceTest {
    @Test
    void tileNbtRetainsEnergyBeyondForgeIntLimit() {
        Bootstrap.register();
        TileEntity.register("powah:test_long_storage", Storage.class);
        Storage source = new Storage();
        source.getEnergyBuffer().setEnergy(5_000_000_000L);
        Storage restored = new Storage();
        restored.readFromNBT(source.writeToNBT(new NBTTagCompound()));
        assertEquals(5_000_000_000L, restored.getEnergyBuffer().energy());
        assertEquals(10_000_000_000L, restored.getEnergyBuffer().capacity());
    }

    @Test
    void generationAtLongCeilingDoesNotOverflowAndSimulationDoesNotCommit() {
        LongEnergyBuffer buffer = new LongEnergyBuffer(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        buffer.setEnergy(Long.MAX_VALUE - 10L);
        assertEquals(10L, buffer.generate(Long.MAX_VALUE, true));
        assertEquals(Long.MAX_VALUE - 10L, buffer.energy());
        assertEquals(10L, buffer.generate(Long.MAX_VALUE, false));
        assertEquals(Long.MAX_VALUE, buffer.energy());
    }

    public static final class Storage extends AbstractEnergyTile {
        public Storage() { super(PowahTier.STARTER, 10_000_000_000L, 1_000L, 1_000L, EnergyPortMode.BOTH, EnergyPortMode.BOTH); }
        @Override protected long getCapacityForTier(PowahTier tier) { return 10_000_000_000L; }
        @Override protected long getMaxReceiveForTier(PowahTier tier) { return 1_000L; }
        @Override protected long getMaxExtractForTier(PowahTier tier) { return 1_000L; }
    }
}
