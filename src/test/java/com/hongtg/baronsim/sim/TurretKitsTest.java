package com.hongtg.baronsim.sim;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * 포탑 키트 충전 테스트 (6단계 ②).
 * 키트는 최대 3개, 3개 미만이면 20초마다 하나씩 찬다. 시작 시 남은 충전 시간이 첫 충전 시각이 된다.
 * 키트가 차는 것만 보려고 포탑 3개를 미리 깔아 둔다 (자리가 꽉 차 있으니 충전돼도 설치하지 않는다).
 */
class TurretKitsTest {

    /** 키트 상태로 전투를 만들고, 키트 충전 이벤트만 timeLimit초까지 돌린다. Simulator와 같은 시작 처리. */
    private TurretKits runRecharge(int startKits, double restSeconds, double timeLimit) {
        TurretKits kits = new TurretKits(startKits, 3, 20);
        CombatState state = new CombatState(1_000_000, new DummyTankingStrategy(new Dummy(0)),
                new Heimer(2000, 50), 0.5, kits, new ArrayList<>(List.of(new Turret(500, 30), new Turret(500, 30), new Turret(500, 30))), new TurretSpec(500, 30, 0, 1.0, 1.0, 3));
        if (!kits.isFull()) {
            kits.setRecharging(true);
            state.schedule(new KitRechargeEvent(restSeconds));
        }
        while (state.hasEvents()) {
            Event e = state.nextEvent();
            if (e.time() > timeLimit) {
                break;
            }
            e.apply(state);
        }
        return kits;
    }

    @Test
    void firstKitArrivesAfterRemainingTime() {
        assertEquals(0, runRecharge(0, 5, 4.9).kits());
        assertEquals(1, runRecharge(0, 5, 5.0).kits());
    }

    @Test
    void thenOneKitEveryTwentySeconds() {
        // 0개, 남은 5초 → 5초 1개, 25초 2개, 45초 3개
        assertEquals(1, runRecharge(0, 5, 24.9).kits());
        assertEquals(2, runRecharge(0, 5, 25.0).kits());
        assertEquals(3, runRecharge(0, 5, 45.0).kits());
    }

    @Test
    void stopsAtThreeKits() {
        TurretKits kits = runRecharge(0, 5, 300);

        assertEquals(3, kits.kits());
        assertTrue(kits.isFull());
        assertFalse(kits.isRecharging(), "가득 차면 충전을 멈춘다");
    }

    @Test
    void fullKitsDoNotRecharge() {
        TurretKits kits = runRecharge(3, 0, 300);

        assertEquals(3, kits.kits());
        assertFalse(kits.isRecharging());
    }

    @Test
    void zeroRemainingTimeMeansKitArrivesImmediately() {
        // 2개, 남은 0초 → 0초에 바로 3개
        assertEquals(3, runRecharge(2, 0, 0).kits());
    }

    @Test
    void kitsDoNotChangeResultWhenSlotsAreFull() {
        // 포탑이 이미 3개면 키트가 있어도 더 깔 자리가 없어 결과가 그대로다 (병풍 있음, 포탑 3개: 50.86초)
        SimulatorResult result = new Simulator().run(new SimulationInput(
                15, 13, 5, 0, 0, 0, 0, UltimateChoice.NONE, 300, 0, 0, 3, 1, 7, RqTurretPosition.FRONT, true, 300));

        assertEquals(50.86, result.killTime(), 0.01);
    }
}
