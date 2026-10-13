package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Simulator에서 시작 시 포탑 설치 (6단계 ③ 연결 1).
 * 바론 15레벨, 하이머 13레벨, Q5, AP 300, 병풍 있음, Q만 사용.
 * 포탑 한 발 69.1668, 사격 간격 4/7초, 바론 체력 18555.05 → 269발에 처치.
 * 포탑 설치 시각만 다르게 해서 처치 시각을 비교한다 (기대값은 Python으로 따로 계산).
 */
class TurretPlacementSimulatorTest {

    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int turretCount, int kits) {
        return simulator.run(new SimulationInput(15, 13, 5, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0,
                turretCount, kits, 20, RqTurretPosition.FRONT, true, 300));
    }

    @Test
    void fullTurretsIgnoreKits() {
        // 포탑 3개면 키트가 있어도 더 안 깔림 → 0, 0, 0초 시작 = 키트 없을 때와 같음
        SimulatorResult result = run(3, 2);
        assertTrue(result.killed());
        assertEquals(50.857142857, result.killTime(), 1e-6);
    }

    @Test
    void oneTurretOneKitPlacesAtZero() {
        // 포탑 1개 + 키트 1개 → 0초에 1개 설치, 20초에 키트가 차서 1개 더 → 0, 0, 20초 시작
        assertEquals(57.714285714, run(1, 1).killTime(), 1e-6);
    }

    @Test
    void oneTurretTwoKitsPlacesAtZeroAndOne() {
        // 포탑 1개 + 키트 2개 → 0초, 1초(Q 쿨)에 설치 → 0, 0, 1초 시작
        assertEquals(51.285714286, run(1, 2).killTime(), 1e-6);
    }

    @Test
    void noTurretThreeKitsPlacesEverySecond() {
        // 포탑 0개 + 키트 3개 → 0, 1, 2초에 설치
        assertEquals(51.857142857, run(0, 3).killTime(), 1e-6);
    }
}
