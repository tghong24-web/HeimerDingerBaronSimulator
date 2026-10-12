package com.hongtg.baronsim.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W(로켓)와 E(수류탄)를 포함한 시뮬레이션 테스트.
 * 공통 조건: 15레벨 바론(체력 18555.05, 마저 85.06), Q 5레벨, 주문력 300, 관통 없음, 제한 300초.
 *
 * 기대값은 Simulator와 별개로 같은 규칙(모두 0초에 시작, 쿨타임마다 반복)을 따로 계산해서 구한 값이다.
 * - 포탑 한 발: 69.17
 * - W 5레벨 한 번(5발 합): 312.87, 쿨타임 7초
 * - W 1레벨 한 번(5발 합): 215.61, 쿨타임 11초
 * - E 5레벨 한 번: 216.15, 쿨타임 11초
 */
class SkillSimulationTest {

    private final Simulator simulator = new Simulator();

    /** 바론 레벨, Q 레벨, 주문력, 관통, 제한 시간은 고정하고 바뀌는 값만 받는다. */
    private SimulatorResult run(int wLevel, int eLevel, int turretCount) {
        return simulator.run(new SimulationInput(15, 13, 5, wLevel, eLevel, 0, UltimateChoice.NONE, 300, 0, 0, turretCount, 0, 20, RqTurretPosition.FRONT, true, 300));
    }

    @Test
    void rocketsShortenKillTime() {
        SimulatorResult turretsOnly = run(0, 0, 3);
        SimulatorResult withW = run(5, 0, 3);

        assertTrue(withW.killTime() < turretsOnly.killTime());
    }

    @Test
    void higherRocketRankKillsFaster() {
        SimulatorResult w1 = run(1, 0, 3);
        SimulatorResult w5 = run(5, 0, 3);

        assertTrue(w5.killTime() < w1.killTime());
    }

    @Test
    void threeTurretsWithW5() {
        SimulatorResult result = run(5, 0, 3);

        assertTrue(result.killed());
        assertEquals(44.57, result.killTime(), 0.01);
    }

    @Test
    void threeTurretsWithW5AndE5() {
        SimulatorResult result = run(5, 5, 3);

        assertTrue(result.killed());
        assertEquals(42.29, result.killTime(), 0.01);
    }

    @Test
    void oneTurretWithW5AndE5() {
        SimulatorResult result = run(5, 5, 1);

        assertTrue(result.killed());
        assertEquals(98.29, result.killTime(), 0.01);
    }

    /** 포탑 없이 W만으로는 300초 안에 못 잡는다 (294초 시점까지 체력이 남음). */
    @Test
    void rocketsAloneCannotKillInTime() {
        SimulatorResult result = run(5, 0, 0);

        assertFalse(result.killed());
    }

    /** 포탑 없이 W와 E만으로는 286초에 잡는다. 이벤트 종류가 섞여도 큐가 시간순으로 처리되는지 확인. */
    @Test
    void rocketsAndGrenadeWithoutTurrets() {
        SimulatorResult result = run(5, 5, 0);

        assertTrue(result.killed());
        assertEquals(286.0, result.killTime(), 0.01);
    }

    @Test
    void rejectsInvalidSkillLevel() {
        assertThrows(IllegalArgumentException.class, () -> run(6, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> run(0, -1, 3));
    }
}
