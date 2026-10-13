package com.hongtg.baronsim.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulatorTest {
    private final Simulator simulator = new Simulator();

    @Test
    void threeTurretKillLevel15Baron() {
        SimulatorResult result = simulator.run(new SimulationInput(15, 13, 5, 0, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, true, 300));

        assertTrue(result.killed());
        assertEquals(50.86, result.killTime(), 0.01);
    }

    @Test
    void oneTurretKillLevel15Baron() {
        // 포탑 1개, 키트 0개(남은 충전 20초) → 20초, 40초에 키트가 차서 포탑 추가 → 포탑 0, 20, 40초 시작
        // (추가 설치가 없으면 153.14초)
        SimulatorResult result = simulator.run(new SimulationInput(15, 13, 5, 0, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 1, 0, 20, RqTurretPosition.FRONT, true, 300));

        assertTrue(result.killed());
        assertEquals(70.86, result.killTime(), 0.01);
    }

    @Test
    void failsWhenTimeLimit() {
        SimulatorResult result = simulator.run(new SimulationInput(15, 13, 5, 0, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 1, 0, 20, RqTurretPosition.FRONT, true, 60));

        assertFalse(result.killed());
    }

    @Test
    void zeroTurretsGetPlacedByKits() {
        // 포탑 0개, 키트 0개 → 20, 40, 60초에 하나씩 설치해서 처치
        SimulatorResult result = simulator.run(new SimulationInput(15, 13, 5, 0, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 0, 0, 20, RqTurretPosition.FRONT, true, 300));

        assertTrue(result.killed());
        assertEquals(90.86, result.killTime(), 0.01);
    }

    @Test
    void grenadeShortensKillTime() {
        SimulatorResult withoutE = simulator.run(new SimulationInput(15, 13, 5, 0, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, true, 300));
        SimulatorResult withE = simulator.run(new SimulationInput(15, 13, 5, 0, 5, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, true, 300));

        assertTrue(withE.killTime() < withoutE.killTime());
    }
}
