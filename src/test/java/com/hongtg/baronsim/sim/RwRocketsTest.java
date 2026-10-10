package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * RW 로켓 테스트.
 * 공통 조건: 바론 15레벨(체력 18555.05, 마저 85.06), Q5, W5, 주문력 300, 관통 0.
 * 기댓값은 코드와 별개로 손계산(Python)한 값이다.
 *   RW 한 번(20발 전탄) R3 = (225 + 0.45*300) + 4*(58 + 0.12*300) + 15*(29 + 0.06*300)
 *                        = 1441 → 마저 적용 ≈ 778.67
 *   RW는 W 차례에 일반 W 대신 나가고, R 쿨(R1~3: 100/85/70초)이 돌면 다음 W 차례에 다시 나간다.
 */
class RwRocketsTest {

    private static final double RW_R3 = 778.666;

    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int eLevel, int rLevel, int turretCount, double timeLimit) {
        return simulator.run(new SimulationInput(
                15, 5, 5, eLevel, rLevel, UltimateChoice.RW, 300, 0, 0, turretCount, timeLimit));
    }

    private double rwTotal(SimulatorResult result) {
        return result.damageBySource().getOrDefault(DamageSource.RW_ROCKETS, 0.0);
    }

    @Test
    void rwWithThreeTurretsAndAllSkills() {
        // R 없이는 42.29초
        SimulatorResult result = run(5, 3, 3, 300);

        assertTrue(result.killed());
        assertEquals(41.71, result.killTime(), 0.01);
        assertEquals(RW_R3, rwTotal(result), 0.01, "42초 안에 끝나므로 RW는 1번");
    }

    @Test
    void rwWithThreeTurretsWithoutE() {
        SimulatorResult result = run(0, 3, 3, 300);

        assertTrue(result.killed());
        assertEquals(43.43, result.killTime(), 0.01);
    }

    @Test
    void rwIsReusedAfterUltimateCooldown() {
        // 포탑 1개는 오래 걸려서 70초에 RW가 한 번 더 나간다 (R 없이는 98.29초)
        SimulatorResult result = run(5, 3, 1, 300);

        assertTrue(result.killed());
        assertEquals(93.14, result.killTime(), 0.01);
        assertEquals(2 * RW_R3, rwTotal(result), 0.01, "0초, 70초에 RW 2번");
    }

    @Test
    void rwFiresEveryUltimateCooldown() {
        // 포탑 0개, 200초 제한: 0, 70, 140초에 RW 3번
        SimulatorResult result = run(0, 3, 0, 200);

        assertFalse(result.killed());
        assertEquals(3 * RW_R3, rwTotal(result), 0.01);
    }

    @Test
    void rwRankOne() {
        // R1 RW 한 번 ≈ 568.46
        SimulatorResult result = run(5, 1, 3, 300);

        assertTrue(result.killed());
        assertEquals(42.00, result.killTime(), 0.01);
        assertEquals(568.464, rwTotal(result), 0.01);
    }

    @Test
    void rwRequiresW() {
        assertThrows(IllegalArgumentException.class, () -> new SimulationInput(
                15, 5, 0, 0, 3, UltimateChoice.RW, 300, 0, 0, 3, 300));
    }
}
