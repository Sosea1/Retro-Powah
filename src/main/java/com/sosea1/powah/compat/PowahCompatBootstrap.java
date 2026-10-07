package com.sosea1.powah.compat;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public final class PowahCompatBootstrap {
    private PowahCompatBootstrap() {}
    public static void preInit() {
        if (Loader.isModLoaded("baubles")) {
            initBaubles();
        }
        if (Loader.isModLoaded("theoneprobe")) {
            FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", "com.sosea1.powah.compat.top.PowahTopCompat$GetTheOneProbe");
        }
        if (Loader.isModLoaded("waila")) {
            FMLInterModComms.sendMessage("waila", "register", "com.sosea1.powah.compat.waila.PowahWailaCompat.register");
        }
    }

    @Optional.Method(modid = "baubles")
    private static void initBaubles() {
        com.sosea1.powah.compat.baubles.BaublesCompat.init();
    }
}
