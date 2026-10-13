package com.hongtg.baronsim.sim;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.calc.CooldownCalculator;

/**
 * 스킬 가속 테스트 (8-1단계).
 * 재사용 대기시간 = 기본값 × 100 / (100 + 가속). W·E·R 쿨, Q 사이 1초, 키트 충전 20초에 적용.
 * 가속 100이면 전부 절반: W5 7→3.5초, E 11→5.5초, R3 70→35초, Q 사이 1→0.5초, 키트 20→10초.
 *
 * 공통 조건: 바론 15레벨, 하이머 13레벨, Q5, AP 300, 관통 0, 병풍 있음.
 * 가속 100이면 남은 키트 충전 시간은 10초 이하여야 한다 (포탑 3개인 테스트는 키트와 무관).
 * 기대값은 코드와 별개로 Python으로 계산했다 (가속 0일 때 값은 기존 테스트와 같음).
 */
class AbilityHasteTest {

    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int wLevel, int eLevel, int rLevel, UltimateChoice ultimate, double haste,
            int turretCount, int kits, double restKitSeconds) {
        return simulator.run(new SimulationInput(15, 13, 5, wLevel, eLevel, rLevel, ultimate, new Build(List.of(), 300, haste, 0, 0, 0),
                turretCount, kits, restKitSeconds, RqTurretPosition.FRONT, true, 300, RoleQuest.NONE));
    }

    // ── 공식 ──

    @Test
    void formula() {
        assertEquals(7.0, CooldownCalculator.reducedCooldown(7, 0), 1e-9);
        assertEquals(3.5, CooldownCalculator.reducedCooldown(7, 100), 1e-9);
        assertEquals(10.0, CooldownCalculator.reducedCooldown(20, 100), 1e-9);
        assertEquals(20 * 100 / 150.0, CooldownCalculator.reducedCooldown(20, 50), 1e-9);
    }

    // ── 입력 검사 ──

    @Test
    void negativeHasteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> run(0, 0, 0, UltimateChoice.NONE, -1, 3, 0, 20));
    }

    @Test
    void remainingKitTimeMustFitReducedRecharge() {
        // 가속 100이면 키트 충전이 10초 → 남은 시간 10초까지만 허용
        assertThrows(IllegalArgumentException.class, () -> run(0, 0, 0, UltimateChoice.NONE, 100, 1, 0, 15));
        run(0, 0, 0, UltimateChoice.NONE, 100, 1, 0, 10);
    }

    @Test
    void zeroHasteIsUnchanged() {
        assertEquals(44.57, run(5, 0, 0, UltimateChoice.NONE, 0, 3, 0, 20).killTime(), 0.01);
    }

    // ── W, E ──

    @Test
    void hasteShortensRocketCooldown() {
        // W 3.5초마다: 0 ~ 38.5초 12번
        SimulatorResult result = run(5, 0, 0, UltimateChoice.NONE, 100, 3, 0, 10);

        assertEquals(40.57, result.killTime(), 0.01);
        assertEquals(12 * 312.8715, result.damageBySource().get(DamageSource.ROCKETS), 0.01);
    }

    @Test
    void hasteShortensRocketAndGrenadeCooldown() {
        // W 3.5초, E 5.5초 → 37.14초 (가속 없으면 42.29초)
        SimulatorResult result = run(5, 5, 0, UltimateChoice.NONE, 100, 3, 0, 10);

        assertEquals(37.14, result.killTime(), 0.01);
        assertEquals(7 * 216.1461, result.damageBySource().get(DamageSource.GRENADE), 0.01);
    }

    // ── 포탑 (키트, Q) ──

    @Test
    void hasteShortensKitRecharge() {
        // 포탑 1개, 키트 0개, 남은 10초 → 10초, 20초에 설치 (가속 없으면 20, 40초 → 70.86초)
        assertEquals(60.86, run(0, 0, 0, UltimateChoice.NONE, 100, 1, 0, 10).killTime(), 0.01);
    }

    @Test
    void hasteShortensTimeBetweenPlacements() {
        // 포탑 0개, 키트 3개 → 0, 0.5, 1.0초에 설치 (가속 없으면 0, 1, 2초 → 51.86초)
        assertEquals(51.36, run(0, 0, 0, UltimateChoice.NONE, 100, 0, 3, 0).killTime(), 0.01);
    }

    // ── R ──

    @Test
    void hasteShortensUltimateCooldown() {
        // R3 70 → 35초: 35초에 RQ 터렛을 한 번 더 깔아서 45.14초 (가속 없으면 48.00초, RQ 1번)
        SimulatorResult result = run(0, 0, 3, UltimateChoice.RQ, 100, 3, 0, 10);

        assertEquals(45.14, result.killTime(), 0.01);
        assertEquals(2 * 210.742, result.damageBySource().get(DamageSource.RQ_TURRET_BEAM), 0.01);
    }
}
