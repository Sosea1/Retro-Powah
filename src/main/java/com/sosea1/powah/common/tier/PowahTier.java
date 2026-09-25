package com.sosea1.powah.common.tier;

/**
 * Gameplay tiers used by Powah. Numeric machine values deliberately live outside this enum
 * so config reloads never require rebuilding enum state.
 */
public enum PowahTier {
    STARTER(0, 0xA7A7A7),
    BASIC(1, 0xA3AB9F),
    HARDENED(2, 0xBBA993),
    BLAZING(3, 0xE4B040),
    NIOTIC(4, 0x13EED2),
    SPIRITED(5, 0xAFE241),
    NITRO(6, 0xD7746C),
    CREATIVE(7, 0x8D29AD);

    private final int index;
    private final int color;

    PowahTier(int index, int color) {
        this.index = index;
        this.color = color;
    }

    public int index() {
        return index;
    }

    public int color() {
        return color;
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }

    public boolean isNormal() {
        return this != CREATIVE;
    }

    public static PowahTier[] normalValues() {
        return new PowahTier[]{STARTER, BASIC, HARDENED, BLAZING, NIOTIC, SPIRITED, NITRO};
    }
}
