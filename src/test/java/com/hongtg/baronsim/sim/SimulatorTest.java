package com.hongtg.baronsim.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulatorTest {
    private final Simulator simulator = new Simulator();

    @Test
    void threeTurretKillLevel15Baron() {
        SimulatorResult result = simulator.run(new SimulationInput(15, 5, 0, 0, 300, 0, 0, 3, 300));

        assertTrue(result.killed());
        assertEquals(50.86, result.killTime(), 0.01);
    }

    @Test
    void oneTurretKillLevel15Baron() {
        SimulatorResult result = simulator.run(new SimulationInput(15, 5, 0, 0, 300, 0, 0, 1, 300));

        assertTrue(result.killed());
        assertEquals(153.14, result.killTime(), 0.01);
    }

    @Test
    void failsWhenTimeLimit() {
        SimulatorResult result = simulator.run(new SimulationInput(15, 5, 0, 0, 300, 0, 0, 1, 60));

        assertFalse(result.killed());
    }

    @Test
    void zeroTurretCannotKill() {
        SimulatorResult result = simulator.run(new SimulationInput(15, 5, 0, 0, 300, 0, 0, 0, 300));

        assertFalse(result.killed());
    }

    @Test
    void grenadeShortensKillTime() {
        SimulatorResult withoutE = simulator.run(new SimulationInput(15, 5, 0, 0, 300, 0, 0, 3, 300));
        SimulatorResult withE = simulator.run(new SimulationInput(15, 5, 0, 5, 300, 0, 0, 3, 300));

        assertTrue(withE.killTime() < withoutE.killTime());
    }
}
