package com.hongtg.baronsim.calc;

import com.hongtg.baronsim.model.BaronStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaronScalerTest {

    private BaronScaler scaler;

    @BeforeEach
    void setUp() {
        scaler = new BaronScaler(
                new StatGrowth(16300, 170),
                new StatGrowth(175, 20),
                0.625,
                new StatGrowth(34, 8),
                new StatGrowth(32, 4));
    }

    @Test
    void healthAtLevel15() {
        BaronStats stats = scaler.statsAt(15);
        assertEquals(18555.05, stats.health(), 0.01);  
        assertEquals(440.3, stats.attackDamage(), 0.01);
        assertEquals(0.625, stats.attackSpeed(), 0.01);
        assertEquals(140.12, stats.armor(), 0.01);
        assertEquals(85.06, stats.magicResist(), 0.01);
    }
}