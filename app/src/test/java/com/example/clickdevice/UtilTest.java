package com.example.clickdevice;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void randomInt_resultInRange() {
        for (int i = 0; i < 100; i++) {
            int result = Util.randomInt(10, 20);
            assertTrue("result should be >= 10, got " + result, result >= 10);
            assertTrue("result should be <= 20, got " + result, result <= 20);
        }
    }

    @Test
    public void randomInt_reversedArgs_stillWorks() {
        for (int i = 0; i < 100; i++) {
            int result = Util.randomInt(20, 10);
            assertTrue("result should be >= 10, got " + result, result >= 10);
            assertTrue("result should be <= 20, got " + result, result <= 20);
        }
    }

    @Test
    public void randomInt_sameValue_returnsThatValue() {
        for (int i = 0; i < 50; i++) {
            assertEquals(5, Util.randomInt(5, 5));
        }
    }

    @Test
    public void randomInt_negativeRange_works() {
        for (int i = 0; i < 100; i++) {
            int result = Util.randomInt(-10, -1);
            assertTrue(result >= -10);
            assertTrue(result <= -1);
        }
    }

    @Test
    public void randomInt_zeroToZero_returnsZero() {
        assertEquals(0, Util.randomInt(0, 0));
    }

    @Test
    public void randomInt_producesVariety() {
        boolean seenDifferent = false;
        int first = Util.randomInt(1, 100);
        for (int i = 0; i < 100; i++) {
            if (Util.randomInt(1, 100) != first) {
                seenDifferent = true;
                break;
            }
        }
        assertTrue("randomInt should produce different values over 100 calls", seenDifferent);
    }
}
