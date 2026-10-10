package com.hongtg.baronsim.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PartOfSkillsTest {
    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int wLevel, int eLevel, int turretCount) {
        return simulator.run(new SimulationInput(15, 5, wLevel, eLevel, 0, UltimateChoice.NONE, 300, 0, 0, turretCount, 300));
    }

    private double totalDamage(SimulatorResult result) {
        double total = 0;
        for (double damage : result.damageBySource().values()) {
            total += damage;
        }
        return total;
    }

    @Test 
    void AddUpToBaronHealthWhenKilled() {
        SimulatorResult result = run(5, 5, 3);

        double total = totalDamage(result);

        assertTrue(result.killed());
        assertEquals(18555.05, total, 0.01);
    }

    @Test 
    void WhenNotWESkills() {
        SimulatorResult result = run(0, 0, 3);

        double total = totalDamage(result);

        assertTrue(result.killed());
        assertFalse(result.damageBySource().containsKey(DamageSource.ROCKETS));
        assertFalse(result.damageBySource().containsKey(DamageSource.GRENADE));
    }

    @Test 
    void WhenWSkillNotESkill() {
        SimulatorResult result = run(5, 0, 3);

        double total = totalDamage(result);

        assertTrue(result.killed());
        assertTrue(result.damageBySource().get(DamageSource.ROCKETS) > 0);
        assertFalse(result.damageBySource().containsKey(DamageSource.GRENADE));
    }
}
