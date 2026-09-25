package com.sosea1.powah.compat.top;

import java.util.function.Function;
import mcjty.theoneprobe.api.ITheOneProbe;

public final class PowahTopCompat {
    private PowahTopCompat() {}
    public static final class GetTheOneProbe implements Function<ITheOneProbe, Void> {
        @Override public Void apply(ITheOneProbe probe) { probe.registerProvider(new PowahTopProvider()); return null; }
    }
}
