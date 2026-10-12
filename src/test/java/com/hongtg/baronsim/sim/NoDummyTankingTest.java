package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * 병풍 없음 테스트: 바론은 먼저 깐 포탑부터 때리고, 포탑이 다 죽으면 하이머를 때린다.
 * 기댓값은 코드와 별개로 손계산(Python)한 값이다.
 *
 * 조건: 바론 15레벨(공격력 440.3, 1.6초마다), 하이머 18레벨, 주문력 300
 *   포탑 체력 = 640 + 0.45 * 300 = 775, 방어력 80 → 바론 한 방 = 440.3 * 100/180 ≈ 244.61 → 4방에 죽음
 *   하이머 체력 = 558 + 105 * 17 = 2343, 방어력 19 + 4.2 * 17 = 90.4 → 한 방 = 231.25 → 11방에 죽음
 *   바론 공격 시각: 0, 1.6, 3.2, ...
 *     포탑1: 0~4.8초에 4방 → 4.8초 사망
 *     포탑2: 6.4~11.2초 → 11.2초 사망
 *     포탑3: 12.8~17.6초 → 17.6초 사망
 *     하이머: 19.2초부터 11방 → 35.2초 사망
 */
class NoDummyTankingTest {

    private static final double BARON_AD = 440.3;
    private static final double INTERVAL = 1.6;
    private static final double TURRET_HP = 775;
    private static final double TURRET_ARMOR = 80;
    private static final double HEIMER_HP = 2343;
    private static final double HEIMER_ARMOR = 90.4;
    private static final double HIT_ON_TURRET = 244.6111;
    private static final double HIT_ON_HEIMER = 231.25;

    private final Turret t1 = new Turret(TURRET_HP, TURRET_ARMOR);
    private final Turret t2 = new Turret(TURRET_HP, TURRET_ARMOR);
    private final Turret t3 = new Turret(TURRET_HP, TURRET_ARMOR);
    private final Heimer heimer = new Heimer(HEIMER_HP, HEIMER_ARMOR);

    /** 바론 평타만 timeLimit초까지 돌린다 (하이머 스킬, 포탑 공격 없음). */
    private void runBaronAttacks(List<Turret> turrets, double timeLimit) {
        CombatState state = new CombatState(1_000_000, new NoDummyTankingStrategy(turrets, heimer), heimer, 0.5, new TurretKits(3, 3, 20), turrets);
        state.schedule(new BaronAttackEvent(0, BARON_AD, INTERVAL));
        while (state.hasEvents()) {
            Event e = state.nextEvent();
            if (e.time() > timeLimit) {
                break;
            }
            e.apply(state);
        }
    }

    @Test
    void firstPlacedTurretIsHitFirst() {
        runBaronAttacks(List.of(t1, t2, t3), 4.0);   // 0, 1.6, 3.2초 → 3방

        assertEquals(TURRET_HP - 3 * HIT_ON_TURRET, t1.health(), 0.01);
        assertEquals(TURRET_HP, t2.health(), 0.0001);
        assertEquals(TURRET_HP, t3.health(), 0.0001);
    }

    @Test
    void turretsDieInPlacementOrder() {
        runBaronAttacks(List.of(t1, t2, t3), 11.3);

        assertTrue(t1.isDead());
        assertTrue(t2.isDead());
        assertFalse(t3.isDead());
        assertEquals(TURRET_HP, t3.health(), 0.0001, "포탑3은 12.8초부터 맞는다");
    }

    @Test
    void heimerIsTargetAfterAllTurretsDie() {
        runBaronAttacks(List.of(t1, t2, t3), 20.0);  // 포탑3 17.6초 사망 → 19.2초에 하이머 첫 방

        assertTrue(t3.isDead());
        assertEquals(HEIMER_HP - HIT_ON_HEIMER, heimer.health(), 0.01);
    }

    @Test
    void heimerSurvivesTenHits() {
        runBaronAttacks(List.of(t1, t2, t3), 34.0);   // 19.2 ~ 33.6초 → 10방

        assertFalse(heimer.isDead());
        assertEquals(HEIMER_HP - 10 * HIT_ON_HEIMER, heimer.health(), 0.01);
    }

    @Test
    void heimerDiesAt35Seconds() {
        runBaronAttacks(List.of(t1, t2, t3), 35.3);

        assertTrue(heimer.isDead());
        assertEquals(0, heimer.health(), 0.0001, "체력은 0 아래로 내려가지 않는다");
    }

    @Test
    void withoutTurretsBaronHitsHeimerFromStart() {
        runBaronAttacks(List.of(), 0.5);   // 0초 한 방

        assertEquals(HEIMER_HP - HIT_ON_HEIMER, heimer.health(), 0.01);
    }

    /**
     * 포탑 사망 시각까지 쏜 횟수: 포탑1 9발, 포탑2 20발, 포탑3 31발 = 60발
     * 포탑 평타 한 발 = (23 + 0.35 * 300) * 100/185.06 ≈ 69.1668
     * Baron's Gaze: 바론이 그 포탑을 때린 뒤부터 그 포탑의 평타는 50%.
     *   포탑1: 0초 1발은 정상(바론 첫 평타 전), 0.57~4.57초 8발은 절반
     *   포탑2: 6.4초 전까지 12발 정상(Gaze는 죽은 포탑1에 남아 있음), 그 뒤 8발 절반
     *   포탑3: 12.8초 전까지 23발 정상, 그 뒤 8발 절반
     *   → 정상 36발 + 절반 24발(= 12발 분량) = 48발 분량
     */
    @Test
    void deadTurretsStopShooting() {
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, false, 60));

        assertFalse(result.killed());
        assertEquals(Outcome.HEIMER_DEAD, result.outcome());
        assertEquals(48 * 69.16676, result.damageBySource().get(DamageSource.TURRET_SHOT), 0.05);
    }

    @Test
    void sameInputWithDummyStillKills() {
        // 병풍이 있으면 포탑이 안 죽으니 기존처럼 50.86초에 처치
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, true, 60));

        assertTrue(result.killed());
        assertEquals(50.86, result.killTime(), 0.01);
    }

    @Test
    void heimerDeathEndsSimulation() {
        // 포탑 3개가 17.6초까지 다 죽고, 하이머는 35.2초에 11번째 평타로 사망 → 거기서 끝
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, false, 300));

        assertEquals(Outcome.HEIMER_DEAD, result.outcome());
        assertEquals(35.2, result.killTime(), 0.001);
    }

    @Test
    void heimerSkillsStopWhenHeimerDies() {
        // 포탑 0개: 하이머가 0초부터 맞아서 16.0초(11번째 평타)에 사망
        // 그 전까지 W는 0, 7, 14초에 3번(한 번 ≈ 312.87), E는 0, 11초에 2번(한 번 ≈ 216.15)
        // Baron's Gaze가 하이머에게 걸리므로 0초(바론 첫 평타 전)를 빼고 전부 50%
        //   W = 312.87 + 156.44 + 156.44 = 2 × 312.87,  E = 216.15 + 108.07 = 1.5 × 216.15
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 5, 5, 0, UltimateChoice.NONE, 300, 0, 0, 0, 0, 20, RqTurretPosition.FRONT, false, 300));

        assertEquals(Outcome.HEIMER_DEAD, result.outcome());
        assertEquals(16.0, result.killTime(), 0.001);
        assertEquals(2 * 312.8715, result.damageBySource().get(DamageSource.ROCKETS), 0.01);
        assertEquals(1.5 * 216.1461, result.damageBySource().get(DamageSource.GRENADE), 0.01);
    }

    @Test
    void outcomeWithDummy() {
        SimulatorResult killed = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, true, 300));
        SimulatorResult timeOut = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 0, 20, RqTurretPosition.FRONT, true, 30));

        assertEquals(Outcome.KILLED, killed.outcome());
        assertEquals(Outcome.TIME_LIMIT, timeOut.outcome(), "병풍이 있으면 하이머는 죽지 않는다");
    }
}
