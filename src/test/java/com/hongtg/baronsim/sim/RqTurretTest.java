package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * RQ 터렛 테스트.
 * 공통 조건: 바론 15레벨(체력 18555.05, 마저 85.06), Q5, R3, 주문력 300, 관통 0.
 * 기댓값은 코드와 별개로 손계산(Python)한 값이다.
 *   RQ 평타 1발 = (120 + 0.35*300) * 100/185.06 ≈ 121.58
 *   RQ 광선 1방 = (180 + 0.70*300) * 100/185.06 ≈ 210.74
 *   공격 간격 1/1.05초, 패턴: 평타 5 → 광선 1 → 평타 2 (총 8회)
 *   R 쿨(R3 70초)마다 다시 설치한다: 0, 70, 140, 210, 280초 ...
 */
class RqTurretTest {

    private static final double RQ_SHOT = 121.582;
    private static final double RQ_BEAM = 210.742;

    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int wLevel, int eLevel, UltimateChoice ultimate, int turretCount) {
        return run(wLevel, eLevel, ultimate, turretCount, 300);
    }

    private SimulatorResult run(int wLevel, int eLevel, UltimateChoice ultimate, int turretCount, double timeLimit) {
        int rLevel = (ultimate == UltimateChoice.NONE) ? 0 : 3;
        return simulator.run(new SimulationInput(
                15, 13, 5, wLevel, eLevel, rLevel, ultimate, 300, 0, 0, turretCount, timeLimit));
    }

    private double total(SimulatorResult result, DamageSource source) {
        return result.damageBySource().getOrDefault(source, 0.0);
    }

    @Test
    void rqTurretAloneCannotKillBaron() {
        SimulatorResult result = run(0, 0, UltimateChoice.RQ, 0);

        assertFalse(result.killed());
    }

    @Test
    void rqTurretFiresSevenShotsAndOneBeam() {
        // 포탑 0개, 60초 제한: 재설치(70초) 전이라 터렛 1개의 8번 공격만 들어간다
        SimulatorResult result = run(0, 0, UltimateChoice.RQ, 0, 60);

        assertEquals(7 * RQ_SHOT, total(result, DamageSource.RQ_TURRET_SHOT), 0.01, "RQ 평타 합계 (7발)");
        assertEquals(1 * RQ_BEAM, total(result, DamageSource.RQ_TURRET_BEAM), 0.01, "RQ 광선 합계 (1방)");
    }

    @Test
    void rqTurretIsPlacedAgainEveryUltimateCooldown() {
        // 포탑 0개, 300초 제한: 0, 70, 140, 210, 280초에 5번 설치
        SimulatorResult result = run(0, 0, UltimateChoice.RQ, 0, 300);

        assertEquals(5 * 7 * RQ_SHOT, total(result, DamageSource.RQ_TURRET_SHOT), 0.05);
        assertEquals(5 * RQ_BEAM, total(result, DamageSource.RQ_TURRET_BEAM), 0.01);
    }

    @Test
    void rqTurretWithThreeTurrets() {
        SimulatorResult result = run(0, 0, UltimateChoice.RQ, 3);

        assertTrue(result.killed());
        assertEquals(48.00, result.killTime(), 0.01);
    }

    @Test
    void rqTurretWithOneTurret() {
        // 70초, 140초에 재설치 (재설치가 없으면 144.00초)
        SimulatorResult result = run(0, 0, UltimateChoice.RQ, 1);

        assertTrue(result.killed());
        assertEquals(135.43, result.killTime(), 0.01);
    }

    @Test
    void rqTurretWithAllSkills() {
        SimulatorResult result = run(5, 5, UltimateChoice.RQ, 3);

        assertTrue(result.killed());
        assertEquals(40.57, result.killTime(), 0.01);
    }

    @Test
    void rqTurretShortensKillTime() {
        SimulatorResult withoutR = run(5, 5, UltimateChoice.NONE, 3);
        SimulatorResult withRq = run(5, 5, UltimateChoice.RQ, 3);

        assertTrue(withRq.killTime() < withoutR.killTime());
    }

    @Test
    void ultimateWithoutRLevelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimulationInput(
                15, 13, 5, 0, 0, 0, UltimateChoice.RQ, 300, 0, 0, 3, 300));
    }
}
