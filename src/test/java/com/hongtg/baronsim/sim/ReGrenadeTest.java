package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * RE 수류탄 테스트.
 * 공통 조건: 바론 15레벨(체력 18555.05, 마저 85.06), Q5, E5, 주문력 300, 관통 0.
 * 기댓값은 코드와 별개로 손계산(Python)한 값이다.
 *   RE 한 번 R3 = (300 + 0.60*300) * 2회 적중 = 960 → 마저 적용 ≈ 518.75
 *   일반 E5 한 번 = (220 + 0.60*300) = 400 → ≈ 216.15
 *   RE는 E 차례에 일반 E 대신 나가고, R 쿨(R3 70초)이 돈 뒤 다음 E 차례(11초 간격)에 다시 나간다.
 */
class ReGrenadeTest {

    private static final double RE_R3 = 518.751;
    private static final double E5 = 216.146;

    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int wLevel, int rLevel, int turretCount, double timeLimit) {
        return simulator.run(new SimulationInput(
                15, 5, wLevel, 5, rLevel, UltimateChoice.RE, 300, 0, 0, turretCount, timeLimit));
    }

    private double total(SimulatorResult result, DamageSource source) {
        return result.damageBySource().getOrDefault(source, 0.0);
    }

    @Test
    void reWithThreeTurretsAndAllSkills() {
        // R 없이는 42.29초
        SimulatorResult result = run(5, 3, 3, 300);

        assertTrue(result.killed());
        assertEquals(42.00, result.killTime(), 0.01);
        assertEquals(RE_R3, total(result, DamageSource.RE_GRENADE), 0.01);
    }

    @Test
    void reWithThreeTurretsWithoutW() {
        SimulatorResult result = run(0, 3, 3, 300);

        assertTrue(result.killed());
        assertEquals(46.86, result.killTime(), 0.01);
    }

    @Test
    void reReplacesNormalGrenade() {
        // 0~44초 E 차례 5번(0, 11, 22, 33, 44) 중 0초는 RE → 일반 E는 4번
        SimulatorResult result = run(0, 3, 3, 300);

        assertEquals(4 * E5, total(result, DamageSource.GRENADE), 0.01,
                "RE를 쓴 차례에는 일반 E가 들어가면 안 된다");
    }

    @Test
    void reIsReusedOnNextGrenadeTurnAfterCooldown() {
        // 포탑 1개 (R 없이는 98.29초): R 쿨은 70초에 돌지만 E 차례는 77초 → RE 2번
        SimulatorResult result = run(5, 3, 1, 300);

        assertTrue(result.killed());
        assertEquals(96.00, result.killTime(), 0.01);
        assertEquals(2 * RE_R3, total(result, DamageSource.RE_GRENADE), 0.01);
    }

    @Test
    void reFiresEveryUltimateCooldown() {
        // 포탑 0개, 200초 제한: 0, 77, 154초에 RE 3번 / 나머지 E 차례 16번
        SimulatorResult result = run(0, 3, 0, 200);

        assertFalse(result.killed());
        assertEquals(3 * RE_R3, total(result, DamageSource.RE_GRENADE), 0.01);
        assertEquals(16 * E5, total(result, DamageSource.GRENADE), 0.01);
    }

    @Test
    void reRankOne() {
        // R1 RE 한 번 = (100 + 180) * 2 → ≈ 302.60
        SimulatorResult result = run(5, 1, 3, 300);

        assertTrue(result.killed());
        assertEquals(42.29, result.killTime(), 0.01);
        assertEquals(302.605, total(result, DamageSource.RE_GRENADE), 0.01);
    }
}

