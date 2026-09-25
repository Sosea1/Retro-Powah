package com.sosea1.powah.compat.waila;

import mcp.mobius.waila.api.IWailaRegistrar;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.content.energizing.BlockEnergizingOrb;
import com.sosea1.powah.content.reactor.BlockReactorPart;

public final class PowahWailaCompat {
    private PowahWailaCompat() {}
    public static void register(IWailaRegistrar registrar) {
        PowahWailaProvider provider=new PowahWailaProvider();
        registrar.registerBodyProvider(provider, BlockPowahMachine.class);
        registrar.registerNBTProvider(provider, BlockPowahMachine.class);
        registrar.registerBodyProvider(provider, BlockEnergizingOrb.class);
        registrar.registerNBTProvider(provider, BlockEnergizingOrb.class);
        registrar.registerStackProvider(provider, BlockReactorPart.class);
    }
}
