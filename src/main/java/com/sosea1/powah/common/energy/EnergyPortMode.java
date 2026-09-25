package com.sosea1.powah.common.energy;

public enum EnergyPortMode {
    NONE(false, false),
    INPUT(true, false),
    OUTPUT(false, true),
    BOTH(true, true);

    private final boolean receive;
    private final boolean extract;

    EnergyPortMode(boolean receive, boolean extract) {
        this.receive = receive;
        this.extract = extract;
    }

    public boolean canReceive() {
        return receive;
    }

    public boolean canExtract() {
        return extract;
    }

    /** Modern Powah Transfer.ALL cycle: BOTH -> OUTPUT -> INPUT -> NONE -> BOTH. */
    public EnergyPortMode next() {
        switch (this) {
            case BOTH: return OUTPUT;
            case OUTPUT: return INPUT;
            case INPUT: return NONE;
            case NONE: default: return BOTH;
        }
    }
}
