package com.hongtg.baronsim.calc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatGrowthTest {

    @Test
    void baronHealthAtLevel11() {
        StatGrowth health = new StatGrowth(16300, 170);
        assertEquals(17791.75, health.valueAt(11), 0.01);
    }
}