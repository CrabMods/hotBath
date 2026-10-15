package com.crabmod.hotbath.dirtiness;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirtinessPatternTest {
    @Test void minimumIntegerMagnitudeNeverProducesANegativeIndex() {
        for (long seed : new long[]{2147483648L, -2147483648L, 0x1234567880000000L})
            assertEquals(8, DirtinessPattern.index(seed, 10));
    }

    @Test void preservesExistingOrdinarySeedPatterns() {
        for (long seed : new long[]{0, 1, -1, 12345, -987654321, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE}) {
            assertEquals(Math.abs((int) seed) % 10, DirtinessPattern.index(seed, 10));
            assertTrue(DirtinessPattern.index(seed, 10) >= 0 && DirtinessPattern.index(seed, 10) < 10);
        }
    }

    @Test void rejectsInvalidPatternCounts() {
        assertThrows(IllegalArgumentException.class, () -> DirtinessPattern.index(1, 0));
    }
}
