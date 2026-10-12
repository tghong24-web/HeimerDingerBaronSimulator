package com.hongtg.baronsim.sim;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * Baron's Gaze 테스트.
 * 바론이 마지막으로 평타를 맞힌 대상(gazeTarget)에 따라 하이머 쪽 피해가 50% 줄고,
 * 줄어든 양은 gazeLossBySource에 출처별로 쌓인다.
 * 기댓값은 코드와 별개로 손계산(Python)한 값이다.
 */
class BaronGazeTest {

    private static final double Q = 69.16676;    // 포탑 평타 한 발 (Q5, AP 300, 바론 15레벨)
    private static final double W = 312.8715;    // W5 한 번 (5발 합)
    private static final double E = 216.1461;    // E5 한 번

    private final Heimer heimer = new Heimer(2000, 50);
    private final Turret t1 = new Turret(500, 50);
    private final Turret t2 = new Turret(500, 50);

    private CombatState state(double baronHealth) {
        return new CombatState(baronHealth, new NoDummyTankingStrategy(List.of(t1, t2), heimer), heimer, 0.5, new TurretKits(3, 3, 20), new ArrayList<>());
    }

    private double loss(CombatState s, DamageSource source) {
        return s.gazeLossBySource().getOrDefault(source, 0.0);
    }

    private double dealt(CombatState s, DamageSource source) {
        return s.damageBySource().getOrDefault(source, 0.0);
    }

    // ── CombatState.damageBaron 단위 테스트 ──

    @Test
    void noGazeBeforeBaronFirstHit() {
        CombatState s = state(10_000);
        s.damageBaron(DamageSource.TURRET_SHOT, 100, t1);

        assertEquals(100, dealt(s, DamageSource.TURRET_SHOT), 0.0001);
        assertEquals(0, loss(s, DamageSource.TURRET_SHOT), 0.0001);
    }

    @Test
    void onlyTheGazedTurretIsReduced() {
        CombatState s = state(10_000);
        s.setGazeTarget(t1);
        s.damageBaron(DamageSource.TURRET_SHOT, 100, t1);   // 바론에게 맞는 포탑 → 50
        s.damageBaron(DamageSource.TURRET_SHOT, 100, t2);   // 다른 포탑 → 100
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);   // 하이머 → 100

        assertEquals(150, dealt(s, DamageSource.TURRET_SHOT), 0.0001);
        assertEquals(50, loss(s, DamageSource.TURRET_SHOT), 0.0001);
        assertEquals(100, dealt(s, DamageSource.ROCKETS), 0.0001);
        assertEquals(0, loss(s, DamageSource.ROCKETS), 0.0001);
    }

    @Test
    void gazeOnHeimerReducesEverything() {
        CombatState s = state(10_000);
        s.setGazeTarget(heimer);
        s.damageBaron(DamageSource.TURRET_SHOT, 100, t1);
        s.damageBaron(DamageSource.TURRET_SHOT, 100, t2);
        s.damageBaron(DamageSource.GRENADE, 100, heimer);

        assertEquals(100, dealt(s, DamageSource.TURRET_SHOT), 0.0001);
        assertEquals(100, loss(s, DamageSource.TURRET_SHOT), 0.0001);
        assertEquals(50, dealt(s, DamageSource.GRENADE), 0.0001);
        assertEquals(50, loss(s, DamageSource.GRENADE), 0.0001);
    }

    @Test
    void gazeOnDummyReducesNothing() {
        CombatState s = new CombatState(10_000, new DummyTankingStrategy(new Dummy(0)), heimer, 0.5, new TurretKits(3, 3, 20), new ArrayList<>());
        s.setGazeTarget(new Dummy(0));
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);

        assertEquals(100, dealt(s, DamageSource.ROCKETS), 0.0001);
        assertEquals(0, loss(s, DamageSource.ROCKETS), 0.0001);
    }

    @Test
    void lossCountsOnlyDamageThatWouldHaveLanded() {
        // 바론 체력 200: Gaze 없으면 300 중 200이 들어감, Gaze로 150 → 손해 50
        CombatState s = state(200);
        s.setGazeTarget(heimer);
        s.damageBaron(DamageSource.ROCKETS, 300, heimer);

        assertEquals(150, dealt(s, DamageSource.ROCKETS), 0.0001);
        assertEquals(50, loss(s, DamageSource.ROCKETS), 0.0001);
    }

    @Test
    void noLossWhenBaronDiesAnyway() {
        // 바론 체력 100: Gaze가 있어도 150이 들어가 바론이 죽음 → 손해 0
        CombatState s = state(100);
        s.setGazeTarget(heimer);
        s.damageBaron(DamageSource.ROCKETS, 300, heimer);

        assertEquals(100, dealt(s, DamageSource.ROCKETS), 0.0001);
        assertEquals(0, loss(s, DamageSource.ROCKETS), 0.0001);
    }

    // ── Simulator 통합 테스트 (NoDummyTankingTest와 같은 조건) ──

    @Test
    void turretShotsLostToGaze() {
        // 포탑 3개, 병풍 없음, 하이머 18레벨: 60발 중 24발이 Gaze로 절반 → 손해 12발 분량
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, false, 300));

        assertEquals(12 * Q, result.gazeLossBySource().get(DamageSource.TURRET_SHOT), 0.05);
        // 들어간 딜 + 잃은 딜 = Gaze가 없었을 때의 딜(60발)
        assertEquals(60 * Q, result.damageBySource().get(DamageSource.TURRET_SHOT)
                + result.gazeLossBySource().get(DamageSource.TURRET_SHOT), 0.05);
    }

    @Test
    void skillsLostToGazeOnHeimer() {
        // 포탑 0개: W 3번 중 7·14초 2번이 절반 → 손해 W 1번 분량, E 2번 중 11초 1번이 절반 → 손해 E 0.5번 분량
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 5, 5, 0, UltimateChoice.NONE, 300, 0, 0, 0, 0, 20, RqTurretPosition.FRONT, false, 300));

        assertEquals(W, result.gazeLossBySource().get(DamageSource.ROCKETS), 0.01);
        assertEquals(0.5 * E, result.gazeLossBySource().get(DamageSource.GRENADE), 0.01);
    }

    @Test
    void noGazeLossWithDummy() {
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 5, 5, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, true, 300));

        double totalLoss = result.gazeLossBySource().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0, totalLoss, 0.0001);
    }
}
