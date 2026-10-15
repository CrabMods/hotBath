package com.crabmod.hotbath.dirtiness;

public final class DirtinessPattern {
    private DirtinessPattern() {}

    public static int index(long seed, int patternCount) {
        if (patternCount <= 0) throw new IllegalArgumentException("patternCount must be positive");
        // Widen before abs so Integer.MIN_VALUE has a representable positive magnitude.
        return (int) (Math.abs((long) (int) seed) % patternCount);
    }
}
