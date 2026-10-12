package com.hongtg.baronsim.calc;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageCalculatorTest {
    @Test 
    void zeroResistTakesFullDamage() {
        double result = DamageCalculator.reducedDamage(100, 0, 0, 0);
        assertEquals(100, result, 0.01);
    }
    
    @Test 
    void hundredResistTakesFullDamage() {
        double result = DamageCalculator.reducedDamage(100, 100, 0, 0);
        assertEquals(50, result, 0.01);
    }

    @Test 
    void percentThenFlatPenetration() {
        double result = DamageCalculator.reducedDamage(100, 100, 0.4, 15);
        assertEquals(68.97, result, 0.01);
    }

    @Test 
    void panetrationCannotGoBelowZero() {
        double result = DamageCalculator.reducedDamage(100, 10, 0, 15);
        assertEquals(100, result, 0.01);
    }

    @Test 
    void negativeResistIncreasesDamage() {
        double result = DamageCalculator.reducedDamage(100, -50, 0, 0);
        assertEquals(133.33, result, 0.01);
    }
}
