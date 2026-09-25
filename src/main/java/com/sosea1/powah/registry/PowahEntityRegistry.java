package com.sosea1.powah.registry;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import com.sosea1.powah.Powah;
import com.sosea1.powah.content.special.EntityChargedSnowball;

public final class PowahEntityRegistry {
    private PowahEntityRegistry() {}
    public static void register() {
        EntityRegistry.registerModEntity(new ResourceLocation(Powah.MOD_ID, "charged_snowball"),
                EntityChargedSnowball.class, "charged_snowball", 0, Powah.INSTANCE, 64, 10, true);
    }
}
