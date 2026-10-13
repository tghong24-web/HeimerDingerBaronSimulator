package com.hongtg.baronsim.sim;

import java.util.ArrayList;
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
 *     하이머: 19.2초부터 11방 → 35.2초 사망 (키트로 재설치가 없을 때)
 *
 * Simulator 통합 테스트는 키트 0개, 남은 충전 19초로 시작한다 (20초면 40초에 바론 평타와 같은 시각이 되어 피함).
 *   19초: 키트 → 죽은 1번 자리에 새 포탑 A → 바론은 19.2~24.0초에 A를 4방 → 24.0초 A 사망
 *   하이머: 25.6~38.4초에 9방
 *   39초: 키트 → 1번 자리에 새 포탑 B → 바론은 40.0~44.8초에 B를 4방 → 44.8초 B 사망
 *   하이머: 46.4초 10방째, 48.0초 11방째 → 48.0초 사망
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

    private final Turret t1 = new Turret(TURRET_HP, TURRET_ARMOR, 0);
    private final Turret t2 = new Turret(TURRET_HP, TURRET_ARMOR, 0);
    private final Turret t3 = new Turret(TURRET_HP, TURRET_ARMOR, 0);
    private final Heimer heimer = new Heimer(HEIMER_HP, HEIMER_ARMOR, 0);

    /** 바론 평타만 timeLimit초까지 돌린다 (하이머 스킬, 포탑 공격 없음). */
    private void runBaronAttacks(List<Turret> turrets, double timeLimit) {
        CombatState state = new CombatState(1_000_000, new NoDummyTankingStrategy(turrets, heimer, RqTurretPosition.FRONT), heimer, 0.5, new TurretKits(0, 3, 20), turrets, new TurretSpec(500, 30, 0, 0, 1.0, 1.0, 3), new ArrayList<>(), new LowHealthBonus(0, 0), new ActiveEffect(0, 0, 1));
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
     * 처음 포탑 3개가 사망 시각까지 쏜 횟수: 포탑1 9발, 포탑2 20발, 포탑3 31발 = 60발
     * 포탑 평타 한 발 = (23 + 0.35 * 300) * 100/185.06 ≈ 69.1668
     * Baron's Gaze: 바론이 그 포탑을 때린 뒤부터 그 포탑의 평타는 50%.
     *   포탑1: 0초 1발은 정상(바론 첫 평타 전), 0.57~4.57초 8발은 절반
     *   포탑2: 6.4초 전까지 12발 정상(Gaze는 죽은 포탑1에 남아 있음), 그 뒤 8발 절반
     *   포탑3: 12.8초 전까지 23발 정상, 그 뒤 8발 절반
     *   → 정상 36발 + 절반 24발(= 12발 분량) = 48발 분량
     * 재설치 포탑 (사격 간격 4/7초):
     *   A(19초 설치, 24.0초 사망): 19.0초 1발 정상(Gaze는 죽은 포탑3), 19.57~23.57초 8발 절반 → 5발 분량
     *   B(39초 설치, 44.8초 사망): Gaze가 하이머에 있어 처음부터 절반, 40.0초부터는 B에 Gaze → 11발 절반 → 5.5발 분량
     *   → 48 + 5 + 5.5 = 58.5발 분량
     */
    @Test
    void deadTurretsStopShooting() {
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, new Build(List.of(), 300, 0, 0, 0, 0), 3, 0, 19, RqTurretPosition.FRONT, false, 60, RoleQuest.NONE));

        assertFalse(result.killed());
        assertEquals(Outcome.HEIMER_DEAD, result.outcome());
        assertEquals(58.5 * 69.16676, result.damageBySource().get(DamageSource.TURRET_SHOT), 0.05);
    }

    @Test
    void sameInputWithDummyStillKills() {
        // 병풍이 있으면 포탑이 안 죽으니 기존처럼 50.86초에 처치
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, new Build(List.of(), 300, 0, 0, 0, 0), 3, 0, 20, RqTurretPosition.FRONT, true, 60, RoleQuest.NONE));

        assertTrue(result.killed());
        assertEquals(50.86, result.killTime(), 0.01);
    }

    @Test
    void heimerDeathEndsSimulation() {
        // 포탑 3개가 17.6초까지 다 죽고, 19초·39초에 키트로 깐 포탑이 8방을 대신 맞아 하이머는 48.0초에 사망 → 거기서 끝
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, new Build(List.of(), 300, 0, 0, 0, 0), 3, 0, 19, RqTurretPosition.FRONT, false, 300, RoleQuest.NONE));

        assertEquals(Outcome.HEIMER_DEAD, result.outcome());
        assertEquals(48.0, result.killTime(), 0.001);
    }

    @Test
    void heimerSkillsStopWhenHeimerDies() {
        // 포탑 0개: 하이머가 0초부터 맞아서 16.0초(11번째 평타)에 사망
        // 그 전까지 W는 0, 7, 14초에 3번(한 번 ≈ 312.87), E는 0, 11초에 2번(한 번 ≈ 216.15)
        // Baron's Gaze가 하이머에게 걸리므로 0초(바론 첫 평타 전)를 빼고 전부 50%
        //   W = 312.87 + 156.44 + 156.44 = 2 × 312.87,  E = 216.15 + 108.07 = 1.5 × 216.15
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 18, 5, 5, 5, 0, UltimateChoice.NONE, new Build(List.of(), 300, 0, 0, 0, 0), 0, 0, 20, RqTurretPosition.FRONT, false, 300, RoleQuest.NONE));

        assertEquals(Outcome.HEIMER_DEAD, result.outcome());
        assertEquals(16.0, result.killTime(), 0.001);
        assertEquals(2 * 312.8715, result.damageBySource().get(DamageSource.ROCKETS), 0.01);
        assertEquals(1.5 * 216.1461, result.damageBySource().get(DamageSource.GRENADE), 0.01);
    }

    @Test
    void outcomeWithDummy() {
        SimulatorResult killed = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, new Build(List.of(), 300, 0, 0, 0, 0), 3, 0, 20, RqTurretPosition.FRONT, true, 300, RoleQuest.NONE));
        SimulatorResult timeOut = new Simulator().run(new SimulationInput(
                15, 18, 5, 0, 0, 0, UltimateChoice.NONE, new Build(List.of(), 300, 0, 0, 0, 0), 3, 0, 20, RqTurretPosition.FRONT, true, 30, RoleQuest.NONE));

        assertEquals(Outcome.KILLED, killed.outcome());
        assertEquals(Outcome.TIME_LIMIT, timeOut.outcome(), "병풍이 있으면 하이머는 죽지 않는다");
    }
}
