package com.hongtg.baronsim.sim;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;

/**
 * 바론 평타 이벤트 테스트 (모드 3: 병풍 더미).
 * Simulator를 거치지 않고 CombatState에 이벤트를 직접 넣어 돌린다.
 *   바론 15레벨 공격력 = 175 + 20 * 14 * (0.7025 + 0.0175 * 14) = 440.3
 *   공격 간격 = 1 / 0.625 = 1.6초
 */
class BaronAttackEventTest {

    private static final double BARON_AD = 440.3;
    private static final double INTERVAL = 1.6;

    /** 더미 하나를 대상으로 바론 평타를 timeLimit초까지 돌린다. */
    private Dummy runAttacks(double dummyArmor, double timeLimit) {
        Dummy dummy = new Dummy(dummyArmor, 0);
        // 바론 체력은 충분히 크게: 바론이 죽어서 멈추는 일이 없도록
        CombatState state = new CombatState(1_000_000, new DummyTankingStrategy(dummy), new Heimer(2000, 50, 0), 0.5, new TurretKits(3, 3, 20), new ArrayList<>(), new TurretSpec(500, 30, 0, 0, 1.0, 1.0, 3));
        state.schedule(new BaronAttackEvent(0, BARON_AD, INTERVAL));

        while (state.hasEvents()) {
            Event e = state.nextEvent();
            if (e.time() > timeLimit) {
                break;
            }
            e.apply(state);
        }
        return dummy;
    }

    @Test
    void baronAttacksEveryInterval() {
        // 0, 1.6, 3.2, ..., 16.0초 → 11번
        Dummy dummy = runAttacks(0, 16.5);

        assertEquals(11, dummy.hitCount());
    }

    @Test
    void zeroArmorTakesFullDamage() {
        Dummy dummy = runAttacks(0, 0.5);   // 0초 한 방만

        assertEquals(1, dummy.hitCount());
        assertEquals(BARON_AD, dummy.totalDamage(), 0.001);
    }

    @Test
    void armorReducesBaronDamage() {
        // 방어력 100 → 100 / (100 + 100) = 절반
        Dummy dummy = runAttacks(100, 0.5);

        assertEquals(BARON_AD / 2, dummy.totalDamage(), 0.001);
    }

    @Test
    void totalDamageAddsUpOverTime() {
        // 방어력 0, 11번 → 440.3 * 11
        Dummy dummy = runAttacks(0, 16.5);

        assertEquals(BARON_AD * 11, dummy.totalDamage(), 0.01);
    }

    @Test
    void dummyDoesNotChangeKillTime() {
        // 모드 3에서는 바론 반격이 있어도 처치 시간이 그대로여야 한다 (포탑 3개: 50.86초)
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 13, 5, 0, 0, 0, UltimateChoice.NONE, new Build(List.of(), 300, 0, 0, 0, 0), 3, 0, 20, RqTurretPosition.FRONT, true, 300, RoleQuest.NONE));

        assertEquals(50.86, result.killTime(), 0.01);
    }
}
